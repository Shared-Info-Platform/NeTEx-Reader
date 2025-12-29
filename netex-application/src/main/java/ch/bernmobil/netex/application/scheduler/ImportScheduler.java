package ch.bernmobil.netex.application.scheduler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.Downloader.NetexFile;
import ch.bernmobil.netex.application.helper.FilesystemWrapper;
import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository.Order;
import ch.bernmobil.netex.persistence.dom.CallWithJourney;
import ch.bernmobil.netex.persistence.dom.ImportVersion;
import ch.bernmobil.netex.persistence.dom.JourneyWithCalls;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import jakarta.annotation.PostConstruct;

public class ImportScheduler {

	private static final Logger logger = LoggerFactory.getLogger(ImportScheduler.class);

	private static final Pattern FILENAME_VERSION_PATTERN = Pattern.compile(".*_([^_]*)\\.zip");

	private final ImportSchedulerProperties properties;
	private final Downloader downloader;
	private final ImporterFactory importerFactory;
	private final ImportVersionRepository importVersionRepository;
	private final NetexRepository historyNetexRepository;
	private final MongoClientWrapper mongoClientWrapper;
	private final FilesystemWrapper filesystem;
	private final Clock clock;

	public ImportScheduler(ImportSchedulerProperties properties, Downloader downloader, ImporterFactory importerFactory,
			ImportVersionRepository importVersionRepository, NetexRepository historyNetexRepository, MongoClientWrapper mongoClientWrapper,
			FilesystemWrapper filesystem, Clock clock) {
		this.properties = properties;
		this.downloader = downloader;
		this.importerFactory = importerFactory;
		this.importVersionRepository = importVersionRepository;
		this.historyNetexRepository = historyNetexRepository;
		this.mongoClientWrapper = mongoClientWrapper;
		this.filesystem = filesystem;
		this.clock = clock;
	}

	@PostConstruct
	public void startup() {
		if (properties.isRunImportAtStartup()) {
			runPeriodicImportTasks();
		}
	}

	@Scheduled(cron = "${importCronExpression:0 0 * * * *}")
	public void runPeriodicImportTasks() {
		try {
			downloadNewVersionsIfNecessary();
			importDataIfNecessary();
			updateHistoryIfNecessary();
			cleanupIfNecessary();
			logger.info("done");
		} catch (Throwable t) {
			logger.error("periodic import task failed", t);
		}
	}

	private void downloadNewVersionsIfNecessary() {
		for (final Entry<String, URI> entry : properties.getUriPerTimetable().entrySet()) {
			try {
				downloadNewVersionIfNecessary(entry.getKey(), entry.getValue());
			} catch (IOException | RuntimeException e) {
				logger.error("failed to download new version", e);
			} catch (InterruptedException e) {
				logger.error("interrupted", e);
				Thread.currentThread().interrupt();
			}
		}
	}

	private void downloadNewVersionIfNecessary(String timetable, URI uri) throws IOException, InterruptedException {
		logger.info("check for new version for {}", timetable);

		final Optional<ImportVersion> lastImportVersion = importVersionRepository.getLastImportVersion(timetable);

		final NetexFile netexFile =
				downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(uri, lastImportVersion.map(i -> i.etag).orElse(null));
		if (netexFile != null) {
			final String version = getVersion(netexFile.file());
			logger.info("found new version {} for {}", version, timetable);

			// check if this version already exists
			importVersionRepository.getImportVersion(timetable, version).ifPresent(existingVersion -> {
				logger.warn("version {} already exists for {}", version, timetable);
				if (existingVersion.complete) {
					throw new IllegalStateException("existing version is complete; abort importing new file");
				} else {
					logger.warn("existing version is incomplete; drop it and continue importing new file");
					mongoClientWrapper.dropDatabase(existingVersion.databaseName);
				}
			});

			final File directory = downloader.extractZipFileToTemporarySubfolder(netexFile.file());

			final ImportVersion currentImport = new ImportVersion();
			currentImport.timetable = timetable;
			currentImport.version = version;
			currentImport.createdAt = Instant.now(clock);
			currentImport.uri = uri.toString();
			currentImport.etag = netexFile.etag();
			currentImport.zipFile = netexFile.file().getAbsolutePath();
			currentImport.directory = directory.getAbsolutePath();
			currentImport.databaseName = properties.getImportDatabasePrefix() + "-" + currentImport.getId();

			importVersionRepository.insertOrUpdate(currentImport);
		} else {
			logger.info("no new version found");
		}
	}

	private void importDataIfNecessary() {
		for (final String timetable : properties.getUriPerTimetable().keySet()) {
			// import data from older versions first so we can use them as baseline to validate imports of newer versions
			final List<ImportVersion> versions = importVersionRepository.getImportVersions(timetable, Order.OLDEST_FIRST);
			for (final ImportVersion version : versions) {
				final ImporterProperties importerProperties = createPropertiesIfImportIsNecessary(version);
				if (importerProperties != null) {
					try {
						logger.info("import data for version {} of {} from {} to {}", version.version, timetable,
								importerProperties.getFirstCalendarDay(), importerProperties.getLastCalendarDay());
						importData(version, importerProperties);
					} catch (IOException | XMLStreamException | RuntimeException e) {
						logger.error("failed to import data", e);
					} catch (InterruptedException e) {
						logger.error("interrupted", e);
						Thread.currentThread().interrupt();
					}
				}
			}
		}
	}

	private ImporterProperties createPropertiesIfImportIsNecessary(ImportVersion version) {
		final LocalDate desiredFirstDate = LocalDate.now(clock).minusDays(properties.getImportDaysInPast());
		final LocalDate desiredLastDate = LocalDate.now(clock).plusDays(properties.getImportDaysInFuture());

		if (!version.complete) {
			final ImporterProperties result = new ImporterProperties();
			result.setWriteCalls(properties.isWriteCallsCollection());
			result.setFirstCalendarDay(desiredFirstDate);
			result.setLastCalendarDay(desiredLastDate);
			return result;
		} else if (!version.valid) {
			return null; // don't import data for invalid version
		} else if (desiredLastDate.isAfter(version.lastDate)) {
			final ImporterProperties result = new ImporterProperties();
			result.setWriteCalls(properties.isWriteCallsCollection());
			result.setFirstCalendarDay(version.lastDate.plusDays(1));
			result.setLastCalendarDay(desiredLastDate);
			return result;
		} else if (desiredFirstDate.isBefore(version.firstDate)) {
			// only possible if properties change
			final ImporterProperties result = new ImporterProperties();
			result.setWriteCalls(properties.isWriteCallsCollection());
			result.setFirstCalendarDay(desiredFirstDate);
			result.setLastCalendarDay(version.firstDate.minusDays(1));
			return result;
		} else {
			return null;
		}
	}

	private void importData(ImportVersion version, ImporterProperties importerProperties)
			throws XMLStreamException, InterruptedException, IOException {
		final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(version.databaseName);

		// prepare directory if necessary
		if (version.directory == null || !filesystem.exists(new File(version.directory))) {
			logger.warn("directory for version {} does not exist, trying to extract data", version.version);
			// set directory to null and update document. we do this in case that extracting the zip file fails mid-operation. in that case
			// the directory would exist again but it would be incomplete. we can't detect this. so we set the directory to null and only
			// set it back to its actual value when the zip file was successfully extracted. this way we would execute this if-clause again
			// and re-try extracting the zip file which makes the logic more robust in case of a failure.
			version.directory = null;
			importVersionRepository.insertOrUpdate(version);

			if (version.zipFile == null || !filesystem.exists(new File(version.zipFile))) {
				logger.warn("zip file for version {} does not exist, trying to download it", version.version);
				// set zipFile to null and update document. we do this for the same reason like for the directory a few lines above
				version.zipFile = null;
				importVersionRepository.insertOrUpdate(version);

				version.zipFile = downloader.downloadFileFromUrlToTemporaryDirectory(URI.create(version.uri)).file().getAbsolutePath();
			}
			version.directory = downloader.extractZipFileToTemporarySubfolder(new File(version.zipFile)).getAbsolutePath();
		}

		// remove incomplete data if necessary
		for (LocalDate date = importerProperties.getFirstCalendarDay(); !date.isAfter(importerProperties.getLastCalendarDay()); date =
				date.plusDays(1)) {
			if (netexRepository.containsDataForCalendarDay(date)) {
				logger.warn("version {} for {} is in an unclean state, remove journeys for calendar day {}", version.version,
						version.timetable, date);
				netexRepository.deleteDataForCalendarDay(date);
			}
		}

		// import data
		final Importer importer = importerFactory.createImporter(importerProperties, netexRepository);
		importer.importDirectory(new File(version.directory));

		// if it was the initial import then validate the imported data by comparing it to the previous versions.
		if (!version.complete) {
			version.valid = validate(version, importerProperties);
		}

		// update version in admin database
		if (version.firstDate == null || version.firstDate.isAfter(importerProperties.getFirstCalendarDay())) {
			version.firstDate = importerProperties.getFirstCalendarDay();
		}
		if (version.lastDate == null || version.lastDate.isBefore(importerProperties.getLastCalendarDay())) {
			version.lastDate = importerProperties.getLastCalendarDay();
		}
		version.complete = true;
		importVersionRepository.insertOrUpdate(version);
	}

	private boolean validate(ImportVersion versionToValidate, ImporterProperties importerProperties) {
		// find the version to validate in the list, then get the next older version for the same timetable that is complete & valid
		final List<ImportVersion> versions = importVersionRepository.getImportVersions(versionToValidate.timetable, Order.NEWEST_FIRST);
		for (int i = 0; i < versions.size(); ++i) {
			if (versions.get(i).version.equals(versionToValidate.version)) {
				// found the version to validate
				for (int j = i + 1; j < versions.size(); ++j) {
					if (versions.get(j).complete && versions.get(j).valid) {
						return validate(versionToValidate, versions.get(j), importerProperties);
					}
				}
			}
		}

		// no valid & complete previous version found, cannot validate, assume that this version is valid
		logger.info("cannot validate version {} of {}, no previous version available", versionToValidate.version,
				versionToValidate.timetable);
		return true;
	}

	private boolean validate(ImportVersion versionToValidate, ImportVersion previousVersion, ImporterProperties importerProperties) {
		// we assume that the import of the previous version has already happened (for the same dates), i.e. that the order of versions for
		// the import was OLDEST_FIRST. therefore the current and the previous version should contain data for the same set of dates.
		// the validation checks that they also contain more or less the same number of journeys for this set of dates.

		final NetexRepository repositoryOfCurrentVersion = mongoClientWrapper.createNetexRepository(versionToValidate.databaseName);
		final NetexRepository repositoryOfPreviousVersion = mongoClientWrapper.createNetexRepository(previousVersion.databaseName);

		for (LocalDate date = importerProperties.getFirstCalendarDay(); !date.isAfter(importerProperties.getLastCalendarDay()); date =
				date.plusDays(1)) {
			final long journeysInCurrentVersion = repositoryOfCurrentVersion.getNumberOfJourneysForCalendarDay(date);
			final long journeysInPreviousVersion = repositoryOfPreviousVersion.getNumberOfJourneysForCalendarDay(date);

			final long average = (journeysInCurrentVersion + journeysInPreviousVersion) / 2;
			final long absoluteDifference = Math.abs(journeysInCurrentVersion - journeysInPreviousVersion);
			final double relativeDifference = 1.0 * absoluteDifference / average;
			if (relativeDifference > properties.getMaxRelativeDifference()) {
				// difference too big, import is not valid
				logger.error(
						"new import of version {} of {} is not valid: for date {} there are {} journeys in the new version while "
								+ "there were {} journeys in the previous version {}",
						versionToValidate.version, versionToValidate.timetable, date, journeysInCurrentVersion, journeysInPreviousVersion,
						previousVersion.version);
				return false;
			}
		}

		logger.info("successfully validated version {} of {} against version {}", versionToValidate.version, versionToValidate.timetable,
				previousVersion.version);
		return true;
	}

	/**
	 * Cleanup does the following things:
	 *  - removes old data from all versions (including those of other schema versions)
	 *  - removes versions of older schema versions that have no data anymore
	 *  - removes versions of the current schema that aren't needed anymore
	 *  - removes leftover databases, directories, and files that don't belong to any known version
	 */
	private void cleanupIfNecessary() throws IOException {
		// delete old data from all versions (including those with other schema versions - assuming that they have at least a 'calendarDay'
		// field).
		final LocalDate cleanupThreshold = LocalDate.now(clock).minusDays(properties.getImportDaysInPast()).minusDays(1);
		logger.info("delete data up to {}", cleanupThreshold);
		for (final ImportVersion version : importVersionRepository.getAllImportVersions()) {
			final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(version.databaseName);
			netexRepository.deleteDataUpToCalendarDay(cleanupThreshold);

			if (version.firstDate != null && !cleanupThreshold.isBefore(version.firstDate)) {
				version.firstDate = cleanupThreshold.plusDays(1); // first available date is one day after cleanup threshold
				importVersionRepository.insertOrUpdate(version);
			}

			// if the version belongs to a timetable that doesn't exist anymore or to an older schema version then delete it completely if
			// the database is empty after old data was deleted
			if (netexRepository.isDatabaseEmpty()) {
				if (!properties.getUriPerTimetable().keySet().contains(version.timetable)
						|| version.schemaVersion < ImportVersion.CURRENT_SCHEMA_VERSION) {
					deleteVersion(version);
				}
			}
		}

		// delete all versions per timetable that are not needed anymore (includes only those with current schema version).
		// we need <MaxVersionsToKeep> versions that are complete and valid plus all forced ones and all the should be kept. all other
		// versions can be deleted.
		for (final String timetable : properties.getUriPerTimetable().keySet()) {
			final List<ImportVersion> versions = importVersionRepository.getImportVersions(timetable, Order.NEWEST_FIRST);
			int completeAndValidVersions = 0;
			for (final ImportVersion version : versions) {
				if (version.complete && version.valid) {
					++completeAndValidVersions;
				}
				if (completeAndValidVersions > properties.getMaxVersionsToKeep() && !version.force && !version.keep) {
					deleteVersion(version);
				}
			}
		}

		// find other databases/files/directories that are not referenced anywhere
		if (properties.isDeleteUnknownResources()) {
			final Set<String> knownDatabases = new HashSet<>();
			final Set<String> knownZipFiles = new HashSet<>();
			final Set<String> knownDirectories = new HashSet<>();
			for (final ImportVersion version : importVersionRepository.getAllImportVersions()) {
				knownDatabases.add(version.databaseName);
				knownZipFiles.add(version.zipFile);
				knownDirectories.add(version.directory);
			}
			for (final String database : mongoClientWrapper.listDatabaseNames()) {
				if (database.startsWith(properties.getImportDatabasePrefix()) && !knownDatabases.contains(database)) {
					logger.warn("deleting database {} that is not referenced by any import version", database);
					mongoClientWrapper.dropDatabase(database);
				}
			}
			for (final File file : filesystem.listFiles(properties.getTemporaryFilesDirectory())) {
				if (!knownZipFiles.contains(file.getAbsolutePath()) && !knownDirectories.contains(file.getAbsolutePath())) {
					if (filesystem.isFile(file)) {
						logger.warn("deleting file {} that is not referenced by any import version", file);
						filesystem.deleteFile(file);
					} else if (filesystem.isDirectory(file)) {
						logger.warn("deleting directory {} that is not referenced by any import version", file);
						filesystem.deleteDirectory(file);
					}
				}
			}
		}
	}

	private void deleteVersion(ImportVersion importVersion) throws IOException {
		logger.info("remove version {} of {}", importVersion.version, importVersion.timetable);
		mongoClientWrapper.dropDatabase(importVersion.databaseName);
		if (importVersion.zipFile != null) {
			filesystem.deleteFile(new File(importVersion.zipFile));
		}
		if (importVersion.directory != null) {
			filesystem.deleteDirectory(new File(importVersion.directory));
		}
		importVersionRepository.deleteImportVersion(importVersion);
	}

	private void updateHistoryIfNecessary() {
		final LocalDate today = LocalDate.now(clock);
		if (!historyNetexRepository.containsDataForCalendarDay(today)) {
			updateHistory(today);
		}
	}

	private void updateHistory(LocalDate today) {
		// copy data from the currently active versions to the history database
		final Collection<ImportVersion> activeVersions = importVersionRepository.getActiveImportVersions();
		for (final ImportVersion activeVersion : activeVersions) {
			try {
				updateHistory(activeVersion, today);
			} catch (RuntimeException e) {
				logger.error("failed to update history database for version " + activeVersion.version + " of " + activeVersion.timetable,
						e);
			}
		}

		// delete old data from history database
		final LocalDate cleanupThreshold = today.minusDays(properties.getHistoryNumberOfDays());
		logger.info("delete old data in history database up to {}", cleanupThreshold);
		historyNetexRepository.deleteDataUpToCalendarDay(cleanupThreshold);
	}

	private void updateHistory(ImportVersion version, LocalDate today) {
		logger.info("adding data for {} from version {} of {} to history database", today, version.version, version.timetable);
		final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(version.databaseName);

		final List<JourneyWithCalls> journeys = netexRepository.getJourneysForCalendarDay(today);
		final List<CallWithJourney> calls = netexRepository.getCallsForCalendarDay(today);

		// override ids to avoid duplicates between different versions (e.g. if the same journey changes calendar day between two versions
		// then it might be inserted twice into the history database; also different versions could just use a different numbering system
		// which could lead to colliding ids).
		journeys.forEach(journey -> journey.id = journey.id + "_" + version.getId());
		calls.forEach(call -> call.id = call.id + "_" + version.getId());

		historyNetexRepository.writeJourneys(journeys);
		historyNetexRepository.writeCalls(calls);
		historyNetexRepository.writeJourneyAggregations(netexRepository.getJourneyAggregationsForCalendarDay(today));
		historyNetexRepository.writeCallAggregations(netexRepository.getCallAggregationsForCalendarDay(today));
		historyNetexRepository.writeRouteAggregations(netexRepository.getRouteAggregationsForCalendarDay(today));
	}

	/**
	 * Tries to extract a version from the filename. If this doesn't work, returns the whole filename instead (without illegal chars).
	 */
	private String getVersion(File file) {
		final String filename = file.getName();
		final Matcher matcher = FILENAME_VERSION_PATTERN.matcher(filename);
		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return filename.replaceAll("\\.", "");
		}
	}
}
