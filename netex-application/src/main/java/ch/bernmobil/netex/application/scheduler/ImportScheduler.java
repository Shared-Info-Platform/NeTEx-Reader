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
import org.springframework.util.FileSystemUtils;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.Downloader.NetexFile;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.dom.CallWithJourney;
import ch.bernmobil.netex.persistence.dom.ImportVersion;
import ch.bernmobil.netex.persistence.dom.JourneyWithCalls;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.search.Helper;
import jakarta.annotation.PostConstruct;

public class ImportScheduler {

	private static final Logger logger = LoggerFactory.getLogger(ImportScheduler.class);

	private static final Pattern FILENAME_VERSION_PATTERN = Pattern.compile(".*_([^_]*)\\.zip");

	private final ImportSchedulerProperties properties;
	private final Downloader downloader;
	private final ImportVersionRepository importVersionRepository;
	private final NetexRepository historyNetexRepository;
	private final MongoClient mongoClient;
	private final Clock clock;

	public ImportScheduler(ImportSchedulerProperties properties, Downloader downloader, ImportVersionRepository importVersionRepository,
			NetexRepository historyNetexRepository, MongoClient mongoClient, Clock clock) {
		this.properties = properties;
		this.downloader = downloader;
		this.importVersionRepository = importVersionRepository;
		this.historyNetexRepository = historyNetexRepository;
		this.mongoClient = mongoClient;
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
		downloadNewVersionsIfNecessary();
		importDataIfNecessary();
		updateHistoryIfNecessary();
		cleanupIfNecessary();
		logger.info("done");
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
					mongoClient.getDatabase(existingVersion.databaseName).drop();
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
			final List<ImportVersion> versions = importVersionRepository.getImportVersions(timetable);
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
		final NetexRepository netexRepository = new NetexRepository(mongoClient, version.databaseName);

		// prepare directory if necessary
		if (version.directory == null || !new File(version.directory).exists()) {
			logger.warn("directory for version {} does not exist, trying to extract data", version.version);
			// set directory to null and update document. we do this in case that extracting the zip file fails mid-operation. in that case
			// the directory would exist again but it would be incomplete. we can't detect this. so we set the directory to null and only
			// set it back to its actual value when the zip file was successfully extracted. this way we would execute this if-clause again
			// and re-try extracting the zip file which makes the logic more robust in case of a failure.
			version.directory = null;
			importVersionRepository.insertOrUpdate(version);

			if (version.zipFile == null || !new File(version.zipFile).exists()) {
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
		final Importer importer = new Importer(importerProperties, netexRepository);
		importer.importDirectory(new File(version.directory));

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

	/**
	 * Cleanup does the following things:
	 *  - removes old data from all versions (including those of other schema versions)
	 *  - removes versions of older schema versions that have no data anymore
	 *  - removes versions of the current schema that aren't needed anymore
	 *  - removes leftover databases, directories, and files that don't belong to any known version
	 */
	private void cleanupIfNecessary() {
		// delete old data from all versions (including those with other schema versions - assuming that they have at least a 'calendarDay'
		// field).
		final LocalDate cleanupThreshold = LocalDate.now(clock).minusDays(properties.getImportDaysInPast()).minusDays(1);
		logger.info("delete data up to {}", cleanupThreshold);
		for (final ImportVersion version : importVersionRepository.getAllImportVersions()) {
			final NetexRepository netexRepository = new NetexRepository(mongoClient, version.databaseName);
			netexRepository.deleteDataUpToCalendarDay(cleanupThreshold);

			if (version.firstDate != null && !cleanupThreshold.isBefore(version.firstDate)) {
				version.firstDate = cleanupThreshold.plusDays(1); // first available date is one day after cleanup threshold
				importVersionRepository.insertOrUpdate(version);
			}

			// if the version belongs to an older schema version then delete it completely if the database is empty after old data was
			// deleted
			if (version.schemaVersion < ImportVersion.CURRENT_SCHEMA_VERSION && netexRepository.isDatabaseEmpty()) {
				deleteVersion(version);
			}
		}

		// delete all versions per timetable that are not needed anymore (includes only those with current schema version)
		for (final String timetable : properties.getUriPerTimetable().keySet()) {
			final List<ImportVersion> versions = importVersionRepository.getImportVersions(timetable);
			for (int i = 0; i < versions.size(); ++i) {
				final ImportVersion version = versions.get(i);
				if (i >= properties.getMaxVersionsToKeep() && !version.force && !version.keep) {
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
			for (final String database : Helper.iterableToList(mongoClient.listDatabaseNames())) {
				if (database.startsWith(properties.getImportDatabasePrefix()) && !knownDatabases.contains(database)) {
					logger.warn("deleting database {} that is not referenced by any import version", database);
					mongoClient.getDatabase(database).drop();
				}
			}
			for (final File file : properties.getTemporaryFilesDirectory().listFiles()) {
				if (!knownZipFiles.contains(file.getAbsolutePath()) && !knownDirectories.contains(file.getAbsolutePath())) {
					if (file.isFile()) {
						logger.warn("deleting file {} that is not referenced by any import version", file);
						deleteFile(file);
					} else if (file.isDirectory()) {
						logger.warn("deleting directory {} that is not referenced by any import version", file);
						deleteDirectory(file);
					}
				}
			}
		}
	}

	private void deleteVersion(ImportVersion importVersion) {
		logger.info("remove version {} of {}", importVersion.version, importVersion.timetable);
		mongoClient.getDatabase(importVersion.databaseName).drop();
		if (importVersion.zipFile != null) {
			deleteFile(new File(importVersion.zipFile));
		}
		if (importVersion.directory != null) {
			deleteDirectory(new File(importVersion.directory));
		}
		importVersionRepository.deleteImportVersion(importVersion);
	}

	private void deleteFile(File file) {
		if (!file.delete()) {
			logger.error("could not delete file {}", file);
		}
	}

	private void deleteDirectory(File directory) {
		if (!FileSystemUtils.deleteRecursively(directory)) {
			logger.error("could not delete directory {}", directory);
		}
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
		final NetexRepository netexRepository = new NetexRepository(mongoClient, version.databaseName);

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
