package ch.bernmobil.netex.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.application.UnittestTime;
import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.Downloader.NetexFile;
import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.importer.ImporterProperties;
import ch.bernmobil.netex.persistence.PersistenceConfig;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.model.CallAggregation;
import ch.bernmobil.netex.persistence.model.CallWithJourney;
import ch.bernmobil.netex.persistence.model.ImportVersion;
import ch.bernmobil.netex.persistence.model.JourneyAggregation;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;
import ch.bernmobil.netex.persistence.model.RouteAggregation;

@SpringBootTest(classes = {ImportSchedulerConfig.class, PersistenceConfig.class, ImportSchedulerIntegrationTest.TestConfig.class})
@ActiveProfiles("test")
public class ImportSchedulerIntegrationTest {

	@Configuration
	public static class TestConfig {
		@Bean
		@Primary
		public Downloader mockedDownloader() throws IOException, InterruptedException {
			return Mockito.mock(Downloader.class);
		}

		@Bean
		@Primary
		public ImporterFactory mockedImporterFactory() throws XMLStreamException, InterruptedException {
			final ImporterFactory importerFactory = Mockito.mock(ImporterFactory.class);
			when(importerFactory.createImporter(any(), any())).thenAnswer(args -> {
				final ImporterProperties properties = args.getArgument(0);
				final NetexRepository netexRepository = args.getArgument(1);
				final Importer importer = Mockito.mock(Importer.class);
				doAnswer(args2 -> {
					final File directory = args2.getArgument(0);
					if (directory.exists()) {
						final String version = directory.getName().replaceAll("download_", "");
						for (LocalDate date = properties.getFirstCalendarDay(); !date.isAfter(properties.getLastCalendarDay()); date =
								date.plusDays(1)) {
							{
								final JourneyWithCalls journey = new JourneyWithCalls();
								journey.id = UUID.randomUUID().toString();
								journey.calendarDay = date.toString();
								journey.operatorName = version; // store version as operator name to identify data in tests
								netexRepository.writeJourneys(List.of(journey));
							}
							if (properties.isWriteCalls()) {
								final CallWithJourney call = new CallWithJourney();
								call.id = UUID.randomUUID().toString();
								call.calendarDay = date.toString();
								netexRepository.writeCalls(List.of(call));
							}
							{
								final JourneyAggregation aggregation = new JourneyAggregation();
								aggregation.calendarDay = date.toString();
								aggregation.operatorCode = "operator";
								aggregation.lineCode = "line";
								aggregation.regionCode = "region";
								netexRepository.writeJourneyAggregations(List.of(aggregation));
							}
							{
								final CallAggregation aggregation = new CallAggregation();
								aggregation.calendarDay = date.toString();
								aggregation.stopPlaceCode = "stop";
								aggregation.operatorCode = "operator";
								aggregation.lineCode = "line";
								aggregation.regionCode = "region";
								netexRepository.writeCallAggregations(List.of(aggregation));
							}
							{
								final RouteAggregation aggregation = new RouteAggregation();
								aggregation.calendarDay = date.toString();
								aggregation.operatorCode = "operator";
								aggregation.lineCode = "line";
								aggregation.regionCode = "region";
								aggregation.directionType = "direction";
								aggregation.directionId = "dir1";
								netexRepository.writeRouteAggregations(List.of(aggregation));
							}
						}
					}
					return null;
				}).when(importer).importDirectory(any());
				return importer;
			});
			return importerFactory;
		}

		@Bean
		@Primary
		public Clock mockedClock(UnittestTime unittestTime) {
			return unittestTime.createClock();
		}

		@Bean
		public UnittestTime unittestTime() {
			return new UnittestTime(Instant.ofEpochMilli(0));
		}
	}

	private static final String TIMETABLE_2025 = "2025";
	private static final String TIMETABLE_2026 = "2026";
	private static final URI URI_2025 = URI.create("uri2025");
	private static final URI URI_2026 = URI.create("uri2026");

	@Autowired
	private ImportScheduler importScheduler;

	@Autowired
	private ImportSchedulerProperties properties;

	@Autowired
	private ImportVersionRepository importVersionRepository;

	@Autowired
	private NetexRepository historyNetexRepository;

	@Autowired
	private Downloader downloader;

	@Autowired
	private MongoClient mongoClient;

	@Autowired
	private MongoClientWrapper mongoClientWrapper;

	@Autowired
	private UnittestTime unittestTime;

	@BeforeEach
	public void setup() throws IOException, InterruptedException {
		for (final String databaseName : mongoClientWrapper.listDatabaseNames()) {
			if (databaseName.startsWith("netex-test-autoimport-") || databaseName.equals("netex-test-history")) {
				new NetexRepository(mongoClient, databaseName).deleteAll();
			} else if (databaseName.equals("netex-test-admin")) {
				mongoClient.getDatabase(databaseName).getCollection("ImportVersions").deleteMany(Filters.empty());
				mongoClient.getDatabase(databaseName).getCollection("Tasks").deleteMany(Filters.empty());
			}
		}

		unittestTime.set(ZonedDateTime.of(2025, 12, 23, 12, 0, 1, 0, ZoneId.systemDefault()).toInstant());

		reset(downloader);
		mockNewDownload(URI_2025, "version2025a");
		mockNewDownload(URI_2026, "version2026a");
	}

	private void mockNewDownload(URI uri, String version) throws IOException, InterruptedException {
		Files.createDirectories(properties.getTemporaryFilesDirectory().toPath());

		final File file = new File(properties.getTemporaryFilesDirectory(), "download_" + version + ".zip").getAbsoluteFile();
		final File directory = new File(properties.getTemporaryFilesDirectory(), "download_" + version).getAbsoluteFile();

		// return file except if etag is unchanged
		final NetexFile netexFile = new NetexFile(file, UUID.randomUUID().toString());
		when(downloader.downloadFileFromUrlToTemporaryDirectory(uri)).thenAnswer(args -> {
			file.createNewFile();
			return netexFile;
		});
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(eq(uri), any())).thenAnswer(args -> {
			file.createNewFile();
			return netexFile;
		});
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(eq(uri), eq(netexFile.etag()))).thenReturn(null);
		when(downloader.extractZipFileToTemporarySubfolder(file)).thenAnswer(args -> {
			Files.createDirectories(directory.toPath());
			return directory;
		});
	}

	@Test
	public void whenSchedulerRunsFirstTime_thenImportsAllDataForAllTimetables() {
		importScheduler.runPeriodicImportTasks();

		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsMultipleTimes_thenImportsDataOnlyOneTimes() {
		importScheduler.runPeriodicImportTasks();
		importScheduler.runPeriodicImportTasks();
		importScheduler.runPeriodicImportTasks();

		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRuns_andTheresAlreadyDataInDatabase_thenDeletesDataAndImportsActualData() {
		// for preparation, let the scheduler run, then delete the versions (but keep the data)
		importScheduler.runPeriodicImportTasks();

		importVersionRepository.getAllImportVersions().forEach(importVersionRepository::deleteImportVersion);
		assertThat(importVersionRepository.getAllImportVersions()).isEmpty();
		assertThat(importVersionRepository.getActiveImportVersions()).isEmpty();

		// run scheduler again, expect that there are no duplicates in the data
		importScheduler.runPeriodicImportTasks();

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgain_andImportTimeframeStartsEarlier_thenImportsMoreData() {
		importScheduler.runPeriodicImportTasks();

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");

		try {
			// change import timeframe
			properties.setImportDaysInPast(3); // 3 instead of 2

			// run scheduler again
			importScheduler.runPeriodicImportTasks();

			assertHasData("version2025a", LocalDate.of(2025, 12, 20), LocalDate.of(2025, 12, 25)); // 2025-12-20 instead of 2025-12-21
			assertHasData("version2026a", LocalDate.of(2025, 12, 20), LocalDate.of(2025, 12, 25)); // ""
			assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
		} finally {
			// reset properties
			properties.setImportDaysInPast(2);
		}
	}

	@Test
	public void whenSchedulerRunsAgan_andNewFileForSameVersionIsAdded_thenDoesNotImportNewVersion()
			throws IOException, InterruptedException {
		importScheduler.runPeriodicImportTasks();

		// advance one minute and mock same version again but with different etag, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026a");
		importScheduler.runPeriodicImportTasks();

		// expect that there are still only two versions
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgain_andNewVersionIsAdded_thenImportsNewVersion() throws IOException, InterruptedException {
		importScheduler.runPeriodicImportTasks();

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026b");
		importScheduler.runPeriodicImportTasks();

		// expect that there are now 3 versions overall, but only two are active
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(3);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		assertHasData("version2025a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026a", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasData("version2026b", LocalDate.of(2025, 12, 21), LocalDate.of(2025, 12, 25));
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 23), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgain_andNewVersionIsAdded_butNewVersionHasMoreDataThanPreviousVersion_thenNewVersionIsInvalid()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026b");
		importScheduler.runPeriodicImportTasks();

		// delete all data of second version
		new NetexRepository(mongoClient, importVersionRepository.getLastImportVersion(TIMETABLE_2026).get().databaseName).deleteAll();

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026c");
		importScheduler.runPeriodicImportTasks();

		// expect that new version is invalid because it has infinity more data than the previous version (after we deleted it)
		final ImportVersion version2026c = importVersionRepository.getLastImportVersion(TIMETABLE_2026).get();
		assertThat(version2026c.version).isEqualTo("version2026c");
		assertThat(version2026c.complete).isTrue();
		assertThat(version2026c.valid).isFalse(); // invalid
	}

	@Test
	public void whenSchedulerRunsAgain_andNewVersionIsAdded_butNewVersionHasMoreDataThanPreviousVersion_butPreviousVersionIsInvalid_thenNewVersionValid()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026b");
		importScheduler.runPeriodicImportTasks();

		// delete all data of second version and mark it as invalid
		{
			final ImportVersion version2026b = importVersionRepository.getLastImportVersion(TIMETABLE_2026).get();
			new NetexRepository(mongoClient, version2026b.databaseName).deleteAll();
			version2026b.valid = false;
			importVersionRepository.insertOrUpdate(version2026b);
		}

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026c");
		importScheduler.runPeriodicImportTasks();

		// expect that new version is valid even though it has infinity more data than the previous version, but the previous version is
		// invalid and is therefore ignored
		final ImportVersion version2026b = importVersionRepository.getLastImportVersion(TIMETABLE_2026).get();
		assertThat(version2026b.version).isEqualTo("version2026c");
		assertThat(version2026b.complete).isTrue();
		assertThat(version2026b.valid).isTrue(); // valid
	}

	@Test
	public void whenSchedulerRunsAgain_andNewVersionIsAdded_butNewVersionHasMoreDataThanPreviousVersion_butPreviousVersionIsIncomplete_thenNewVersionIsInvalid()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026b");
		importScheduler.runPeriodicImportTasks();

		// delete all data of second version and mark it as incomplete
		{
			final ImportVersion version2026b = importVersionRepository.getLastImportVersion(TIMETABLE_2026).get();
			new NetexRepository(mongoClient, version2026b.databaseName).deleteAll();
			version2026b.complete = false;
			version2026b.valid = false;
			version2026b.firstDate = null;
			version2026b.lastDate = null;
			importVersionRepository.insertOrUpdate(version2026b);
		}

		// advance one minute (so that new version gets newer createdAt timestamp) and mock new version, then import again
		unittestTime.advance(Duration.ofMinutes(1));
		mockNewDownload(URI_2026, "version2026c");
		importScheduler.runPeriodicImportTasks();

		// expect that new version is valid even though it has infinity more data than the previous version, but the previous version is
		// invalid and is therefore ignored
		final ImportVersion version2026b = importVersionRepository.getLastImportVersion(TIMETABLE_2026).get();
		assertThat(version2026b.version).isEqualTo("version2026c");
		assertThat(version2026b.complete).isTrue();
		assertThat(version2026b.valid).isTrue(); // valid
	}

	@Test
	public void whenSchedulerRunsAgainNextDay_thenImportsNewDataForNewDay_andDeletesDataForOldDay() {
		importScheduler.runPeriodicImportTasks();

		// advance one day and import again
		unittestTime.advance(Duration.ofDays(1));
		importScheduler.runPeriodicImportTasks();

		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data for 2025-12-21 is deleted and data for 2025-12-26 is added
		assertHasData("version2025a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
		assertHasData("version2026a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));

		// expect that history for 2025-12-24 is added
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 24), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainNextDay_andNewVersionIsAdded_thenImportsNewDataForNewDay_andDeletesDataForOldDay()
			throws IOException, InterruptedException {
		importScheduler.runPeriodicImportTasks();

		// advance one day and import again
		unittestTime.advance(Duration.ofDays(1));
		mockNewDownload(URI_2026, "version2026b");
		importScheduler.runPeriodicImportTasks();

		// expect that there are now 3 versions overall, but only two are active
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(3);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data for 2025-12-21 is deleted and data for 2025-12-26 is added
		assertHasData("version2025a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
		assertHasData("version2026a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
		assertHasData("version2026b", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));

		// expect that history for 2025-12-24 is added
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 24), "version2025a", "version2026a", "version2026b");
	}

	@Test
	public void whenSchedulerRunsAgainNextDay_butDirectoryWasDeleted_thenExtractsZipfileAgain_andImportsNewDataForNewDay() {
		importScheduler.runPeriodicImportTasks();

		// delete directory
		assertThat(new File(importVersionRepository.getLastImportVersion(TIMETABLE_2025).get().directory).delete()).isTrue();

		// advance one day and import again
		unittestTime.advance(Duration.ofDays(1));
		importScheduler.runPeriodicImportTasks();

		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data for 2025-12-21 is deleted and data for 2025-12-26 is added
		assertHasData("version2025a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
		assertHasData("version2026a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));

		// expect that history for 2025-12-24 is added
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 24), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainNextDay_butDirectoryAndZipfileWereDeleted_thenDownloadsAndExtractsZipfileAgain_andImportsNewDataForNewDay() {
		importScheduler.runPeriodicImportTasks();

		// delete zipfile and directory
		assertThat(new File(importVersionRepository.getLastImportVersion(TIMETABLE_2025).get().zipFile).delete()).isTrue();
		assertThat(new File(importVersionRepository.getLastImportVersion(TIMETABLE_2025).get().directory).delete()).isTrue();

		// advance one day and import again
		unittestTime.advance(Duration.ofDays(1));
		importScheduler.runPeriodicImportTasks();

		assertThat(importVersionRepository.getAllImportVersions()).hasSize(2);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data for 2025-12-21 is deleted and data for 2025-12-26 is added
		assertHasData("version2025a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));
		assertHasData("version2026a", LocalDate.of(2025, 12, 22), LocalDate.of(2025, 12, 26));

		// expect that history for 2025-12-24 is added
		assertHasHistory(LocalDate.of(2025, 12, 23), LocalDate.of(2025, 12, 24), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainNextDays_withManyNewVersions_thenDeletesOldDataAndOldVersions()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// import five more versions
		final List<String> newVersions = List.of("version2025b", "version2025c", "version2025d", "version2025e", "version2025f");
		for (final String newVersion : newVersions) {
			// advance one day and mock new version, then import again
			unittestTime.advance(Duration.ofDays(1));
			mockNewDownload(URI_2025, newVersion);
			importScheduler.runPeriodicImportTasks();
		}

		// expect that only the last three versions exist (plus the version of the other timetable)
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(4);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data up to 2025-12-25 is deleted and new data was added
		assertHasData("version2025d", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025e", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025f", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2026a", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));

		// expect that history for 2025-12-24 is deleted and new history was added
		assertHasHistory(LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 28), "version2025c", "version2025d", "version2025e",
				"version2025f", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainNextDays_withManyNewVersions_butFirstVersionIsForced_thenDeletesOldDataAndOldVersions()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// mark first version as forced
		{
			final ImportVersion version2025a = importVersionRepository.getLastImportVersion(TIMETABLE_2025).get();
			version2025a.force = true;
			importVersionRepository.insertOrUpdate(version2025a);
		}

		// import five more versions
		final List<String> newVersions = List.of("version2025b", "version2025c", "version2025d", "version2025e", "version2025f");
		for (final String newVersion : newVersions) {
			// advance one day and mock new version, then import again
			unittestTime.advance(Duration.ofDays(1));
			mockNewDownload(URI_2025, newVersion);
			importScheduler.runPeriodicImportTasks();
		}

		// expect that only the last three versions plus the forced one exist (plus the version of the other timetable)
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(5);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data up to 2025-12-25 is deleted and new data was added
		assertHasData("version2025a", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025d", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025e", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025f", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2026a", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));

		// expect that history for 2025-12-24 is deleted and new history was added, all history of timetable 2025 stems from the first
		// (forced) version
		assertHasHistory(LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 28), "version2025a", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainNextDays_withManyNewVersions_butFirstVersionMustBeKept_thenDeletesOldDataAndOldVersions()
			throws IOException, InterruptedException {
		// run scheduler to import first version
		importScheduler.runPeriodicImportTasks();

		// mark first version with 'keep' (so it does not get deleted)
		{
			final ImportVersion version2025a = importVersionRepository.getLastImportVersion(TIMETABLE_2025).get();
			version2025a.keep = true;
			importVersionRepository.insertOrUpdate(version2025a);
		}

		// import five more versions
		final List<String> newVersions = List.of("version2025b", "version2025c", "version2025d", "version2025e", "version2025f");
		for (final String newVersion : newVersions) {
			// advance one day and mock new version, then import again
			unittestTime.advance(Duration.ofDays(1));
			mockNewDownload(URI_2025, newVersion);
			importScheduler.runPeriodicImportTasks();
		}

		// expect that only the last three versions exist plus the one that must be kept (plus the version of the other timetable)
		assertThat(importVersionRepository.getAllImportVersions()).hasSize(5);
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);

		// expect that data up to 2025-12-25 is deleted and new data was added
		assertHasData("version2025a", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025d", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025e", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2025f", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));
		assertHasData("version2026a", LocalDate.of(2025, 12, 26), LocalDate.of(2025, 12, 30));

		// expect that history for 2025-12-24 is deleted and new history was added. archiving the first version has no impact on history, so
		// all new versions should appear in history (unless already deleted)
		assertHasHistory(LocalDate.of(2025, 12, 25), LocalDate.of(2025, 12, 28), "version2025c", "version2025d", "version2025e",
				"version2025f", "version2026a");
	}

	@Test
	public void whenSchedulerRunsAgainAfterSeveralDays_andTimetableWasRemoved_thenDeletesVersionWhenAllDataWasRemoved()
			throws IOException, InterruptedException {
		try {
			// add additional timetable
			final URI uri = URI.create("some-uri");
			properties.getUriPerTimetable().put("some-timetable", uri);
			mockNewDownload(uri, "some-version");

			// run scheduler to import the current version of all three timetables
			importScheduler.runPeriodicImportTasks();
			assertThat(importVersionRepository.getAllImportVersions()).extracting(iv -> iv.timetable)
					.containsExactlyInAnyOrder("some-timetable", "2025", "2026");
			assertThat(importVersionRepository.getActiveImportVersions()).hasSize(3);
		} finally {
			// revert properties
			properties.getUriPerTimetable().remove("some-timetable");
		}

		// advance time four days and run scheduler again, expect that there are still three versions
		unittestTime.advance(Duration.ofDays(4));
		importScheduler.runPeriodicImportTasks();
		assertThat(importVersionRepository.getAllImportVersions()).extracting(iv -> iv.timetable)
				.containsExactlyInAnyOrder("some-timetable", "2025", "2026");
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(3);

		// advance time another day and run scheduler again, expect that the version of the removed timetable is now gone
		unittestTime.advance(Duration.ofDays(1));
		importScheduler.runPeriodicImportTasks();
		assertThat(importVersionRepository.getAllImportVersions()).extracting(iv -> iv.timetable).containsExactlyInAnyOrder("2025", "2026");
		assertThat(importVersionRepository.getActiveImportVersions()).hasSize(2);
	}

	private void assertHasData(String version, LocalDate earliestDate, LocalDate latestDate) {
		final String database =
				importVersionRepository.getAllImportVersions().stream().filter(importVersion -> importVersion.version.equals(version))
						.findFirst().map(importVersion -> importVersion.databaseName).orElseThrow();
		final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(database);
		assertHasData(netexRepository, earliestDate, latestDate, version);
	}

	private void assertHasData(NetexRepository netexRepository, LocalDate earliestDate, LocalDate latestDate, String expectedVersion) {
		// assert that there is data inside of expected boundaries with expected version
		for (LocalDate date = earliestDate; !date.isAfter(latestDate); date = date.plusDays(1)) {
			assertThat(netexRepository.getNumberOfJourneysForCalendarDay(date)).describedAs("number of journeys for " + date).isEqualTo(1);
			assertThat(netexRepository.getJourneysForCalendarDay(date).getFirst().operatorName).isEqualTo(expectedVersion);
		}

		// assert that there is no data outside of expected boundaries
		assertThat(netexRepository.getNumberOfJourneysForCalendarDay(earliestDate.minusDays(1))).isEqualTo(0);
		assertThat(netexRepository.getNumberOfJourneysForCalendarDay(latestDate.plusDays(1))).isEqualTo(0);
	}

	private void assertHasHistory(LocalDate earliestDate, LocalDate latestDate, String ... expectedVersions) {
		// assert that there is data inside of expected boundaries with expected version
		final Set<String> versions = new HashSet<>();
		for (LocalDate date = earliestDate; !date.isAfter(latestDate); date = date.plusDays(1)) {
			assertThat(historyNetexRepository.getNumberOfJourneysForCalendarDay(date)).isEqualTo(2);
			historyNetexRepository.getJourneysForCalendarDay(date).forEach(journey -> versions.add(journey.operatorName));
		}
		assertThat(versions).containsExactlyInAnyOrder(expectedVersions);

		// assert that there is no data outside of expected boundaries
		assertThat(historyNetexRepository.getNumberOfJourneysForCalendarDay(earliestDate.minusDays(1))).isEqualTo(0);
		assertThat(historyNetexRepository.getNumberOfJourneysForCalendarDay(latestDate.plusDays(1))).isEqualTo(0);
	}
}
