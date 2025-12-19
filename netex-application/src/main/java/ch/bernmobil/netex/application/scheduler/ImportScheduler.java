package ch.bernmobil.netex.application.scheduler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.Downloader.NetexFile;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.dom.ImportVersion;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import jakarta.annotation.PostConstruct;

public class ImportScheduler {

	private static final Logger logger = LoggerFactory.getLogger(ImportScheduler.class);

	private static final Pattern FILENAME_VERSION_PATTERN = Pattern.compile(".*_([^_]*)\\.zip");

	private final ImportSchedulerProperties properties;
	private final Downloader downloader;
	private final ImportVersionRepository importVersionRepository;
	private final MongoClient mongoClient;
	private final Clock clock;

	public ImportScheduler(ImportSchedulerProperties properties, Downloader downloader, ImportVersionRepository importVersionRepository,
			MongoClient mongoClient, Clock clock) {
		this.properties = properties;
		this.downloader = downloader;
		this.importVersionRepository = importVersionRepository;
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
			final List<ImportVersion> versions = importVersionRepository.getAllImportVersions(timetable);
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
