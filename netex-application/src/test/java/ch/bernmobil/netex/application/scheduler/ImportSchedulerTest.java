package ch.bernmobil.netex.application.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.Downloader.NetexFile;
import ch.bernmobil.netex.application.helper.FilesystemWrapper;
import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository.Order;
import ch.bernmobil.netex.persistence.dom.ImportVersion;
import ch.bernmobil.netex.persistence.export.NetexRepository;

public class ImportSchedulerTest {

	private static final String TIMETABLE_2025 = "2025";
	private static final String TIMETABLE_2026 = "2026";
	private static final URI URI_2025 = URI.create("uri2025");
	private static final URI URI_2026 = URI.create("uri2026");

	private ImportScheduler importScheduler;
	private ImportSchedulerProperties properties;
	private Downloader downloader;
	private ImporterFactory importerFactory;
	private Importer importer;
	private ImportVersionRepository importVersionRepository;
	private NetexRepository historyNetexRepository;
	private MongoClientWrapper mongoClientWrapper;
	private NetexRepository netexRepository;
	private FilesystemWrapper filesystemWrapper;
	private Clock clock;

	@BeforeEach
	public void setup() {
		properties = new ImportSchedulerProperties();
		properties.setImportDaysInPast(2);
		properties.setImportDaysInFuture(2);
		properties.setUriPerTimetable(Map.of(TIMETABLE_2025, URI_2025, TIMETABLE_2026, URI_2026));

		downloader = Mockito.mock(Downloader.class);
		importer = Mockito.mock(Importer.class);
		importerFactory = Mockito.mock(ImporterFactory.class);
		when(importerFactory.createImporter(any(), any())).thenReturn(importer);
		importVersionRepository = Mockito.mock(ImportVersionRepository.class);
		historyNetexRepository = Mockito.mock(NetexRepository.class);
		mongoClientWrapper = Mockito.mock(MongoClientWrapper.class);
		netexRepository = Mockito.mock(NetexRepository.class);
		when(mongoClientWrapper.createNetexRepository(any())).thenReturn(netexRepository);
		filesystemWrapper = Mockito.mock(FilesystemWrapper.class);
		when(filesystemWrapper.exists(any())).thenReturn(true);
		clock = Clock.fixed(ZonedDateTime.of(2025, 12, 23, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

		importScheduler = new ImportScheduler(properties, downloader, importerFactory, importVersionRepository, historyNetexRepository,
				mongoClientWrapper, filesystemWrapper, clock);
	}

	@Test
	public void whenSchedulerIsRun_thenLooksForNewVersionForEachTimetable() throws IOException, InterruptedException {
		importScheduler.runPeriodicImportTasks();

		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null);
		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2026, null);
		verifyNoMoreInteractions(downloader);
	}

	@Test
	public void whenLastImportVersionForTimetableExists_thenUsesEtagToLookForNewFile() throws IOException, InterruptedException {
		final ImportVersion version1 = new ImportVersion();
		version1.etag = "some-etag";
		when(importVersionRepository.getLastImportVersion(TIMETABLE_2025)).thenReturn(Optional.of(version1));

		final ImportVersion version2 = new ImportVersion();
		version2.etag = "other-etag";
		when(importVersionRepository.getLastImportVersion(TIMETABLE_2026)).thenReturn(Optional.of(version2));

		importScheduler.runPeriodicImportTasks();

		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, "some-etag");
		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2026, "other-etag");
		verifyNoMoreInteractions(downloader);
	}

	@Test
	public void whenDownloaderReturnsNewVersionForTimetable_thenUnzipsFileAndCreatesNewImportVersion()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_version1.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		importScheduler.runPeriodicImportTasks();

		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null);
		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2026, null);
		verify(downloader).extractZipFileToTemporarySubfolder(new File("somezipfile_with_version1.zip"));
		verifyNoMoreInteractions(downloader);

		verify(importVersionRepository).insertOrUpdate(argThat(importVersion -> {
			assertThat(importVersion.getId()).isEqualTo("1_2025_version1");
			assertThat(importVersion.timetable).isEqualTo(TIMETABLE_2025);
			assertThat(importVersion.version).isEqualTo("version1");
			assertThat(importVersion.createdAt).isEqualTo(clock.instant());
			assertThat(importVersion.uri).isEqualTo("uri2025");
			assertThat(importVersion.etag).isEqualTo("etag");
			assertThat(importVersion.zipFile).endsWith("somezipfile_with_version1.zip");
			assertThat(importVersion.directory).endsWith("somedirectory");
			assertThat(importVersion.databaseName).isEqualTo("netex-autoimport-1_2025_version1");
			assertThat(importVersion.firstDate).isNull();
			assertThat(importVersion.lastDate).isNull();
			assertThat(importVersion.complete).isFalse();
			assertThat(importVersion.valid).isFalse();
			assertThat(importVersion.force).isFalse();
			assertThat(importVersion.keep).isFalse();
			assertThat(importVersion.schemaVersion).isEqualTo(1);
			return true;
		}));
	}

	@Test
	public void whenDownloaderReturnsNewVersionForTimetable_andAnotherVersionForThisTimetableAlreadyExists_thenCreatesNewImportVersion()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_newversion.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		final ImportVersion version = createCompleteImportVersion();
		when(importVersionRepository.getImportVersion(version.timetable, version.version)).thenReturn(Optional.of(version));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository).insertOrUpdate(any());
		verify(mongoClientWrapper, never()).dropDatabase(any());
	}

	@Test
	public void whenDownloaderReturnsNewVersionForTimetable_andTheSameVersionForThisTimetableAlreadyExists_butItIsNotComplete_thenCreatesNewImportVersion_andDropsDatabaseOfExistingVersion()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_newversion.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		final ImportVersion version = createIncompleteImportVersion();
		version.version = "newversion";
		when(importVersionRepository.getImportVersion(version.timetable, version.version)).thenReturn(Optional.of(version));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository).insertOrUpdate(any());
		verify(mongoClientWrapper).dropDatabase("database");
	}

	@Test
	public void whenDownloaderReturnsNewVersionForTimetable_andTheSameVersionForThisTimetableAlreadyExists_andItIsComplete_thenDoesNotCreateNewImportVersion()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_newversion.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		final ImportVersion version = createCompleteImportVersion();
		version.version = "newversion";
		when(importVersionRepository.getImportVersion(version.timetable, version.version)).thenReturn(Optional.of(version));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, never()).insertOrUpdate(any());
		verify(mongoClientWrapper, never()).dropDatabase(any());
	}

	@Test
	public void whenDownloadThrows_thenStillDownloadsOtherVersion() throws IOException, InterruptedException {
		when(downloader.downloadFileFromUrlToTemporaryDirectoryIfNecessary(any(), any())).thenThrow(new RuntimeException("test"));

		importScheduler.runPeriodicImportTasks();

		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2025, null);
		verify(downloader).downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI_2026, null);
		verifyNoMoreInteractions(downloader);
	}

	@Test
	public void whenHasImportVersion_thenImportsData() throws XMLStreamException, InterruptedException {
		final ImportVersion importVersion = createIncompleteImportVersion();
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(importer).importDirectory(new File("directory"));
		verifyNoMoreInteractions(importer);
	}

	@Test
	public void whenHasMultipleImportVersions_thenImportsDataForAll() throws XMLStreamException, InterruptedException {
		final ImportVersion importVersion1 = createIncompleteImportVersion();
		importVersion1.directory = "directory1";
		final ImportVersion importVersion2 = createIncompleteImportVersion();
		importVersion2.directory = "directory2";
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion1, importVersion2));

		importScheduler.runPeriodicImportTasks();

		verify(importer).importDirectory(new File("directory1"));
		verify(importer).importDirectory(new File("directory2"));
		verifyNoMoreInteractions(importer);
	}

	@Test
	public void whenHasImportVersionForMultipleTimetables_thenImportsDataForAll() throws XMLStreamException, InterruptedException {
		final ImportVersion importVersion1 = createIncompleteImportVersion();
		importVersion1.directory = "directory1";
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion1));

		final ImportVersion importVersion2 = createIncompleteImportVersion();
		importVersion2.directory = "directory2";
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2026), any())).thenReturn(List.of(importVersion2));

		importScheduler.runPeriodicImportTasks();

		verify(importer).importDirectory(new File("directory1"));
		verify(importer).importDirectory(new File("directory2"));
		verifyNoMoreInteractions(importer);
	}

	@Test
	public void whenImportThrows_thenStillImportsOtherVersions() throws XMLStreamException, InterruptedException {
		{
			final ImportVersion importVersion1 = createIncompleteImportVersion();
			importVersion1.directory = "directory1";
			final ImportVersion importVersion2 = createIncompleteImportVersion();
			importVersion2.directory = "directory2";
			when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion1, importVersion2));
		}
		{
			final ImportVersion importVersion1 = createIncompleteImportVersion();
			importVersion1.directory = "directory3";
			final ImportVersion importVersion2 = createIncompleteImportVersion();
			importVersion2.directory = "directory4";
			when(importVersionRepository.getImportVersions(eq(TIMETABLE_2026), any())).thenReturn(List.of(importVersion1, importVersion2));
		}

		// let import throw
		doThrow(new RuntimeException("test")).when(importer).importDirectory(any());

		importScheduler.runPeriodicImportTasks();

		// expect that importer is still called 4 times
		verify(importer).importDirectory(new File("directory1"));
		verify(importer).importDirectory(new File("directory2"));
		verify(importer).importDirectory(new File("directory3"));
		verify(importer).importDirectory(new File("directory4"));
		verifyNoMoreInteractions(importer);
	}

	@Test
	public void whenHasImportVersionThatIsNotComplete_thenImportsDataForFullTimeRange_andUpdatesImportVersionCorrectly() {
		final ImportVersion importVersion = createIncompleteImportVersion();
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(importerFactory).createImporter(argThat(properties -> {
			assertThat(properties.getFirstCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(properties.getLastCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 25));
			return true;
		}), any());
		verify(importVersionRepository).insertOrUpdate(argThat(update -> {
			assertThat(update.firstDate).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(update.lastDate).isEqualTo(LocalDate.of(2025, 12, 25));
			assertThat(update.complete).isTrue();
			assertThat(update.valid).isTrue();
			return true;
		}));
	}

	@Test
	public void whenHasImportVersionThatIsCompleteAndValid_andNewTimeRangeIsUnchanged_thenDoesNotImportData() {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 19);
		importVersion.lastDate = LocalDate.of(2025, 12, 25);
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verifyNoInteractions(importerFactory);
		verify(importVersionRepository, never()).insertOrUpdate(any());
	}

	@Test
	public void whenHasImportVersionThatIsCompleteAndValid_andNewTimeRangeHasLaterEnd_thenImportsDataForMissingDatesAtEnd_andUpdatesImportVersionCorrectly() {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 19);
		importVersion.lastDate = LocalDate.of(2025, 12, 23);
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(importerFactory).createImporter(argThat(properties -> {
			assertThat(properties.getFirstCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 24));
			assertThat(properties.getLastCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 25));
			return true;
		}), any());
		verify(importVersionRepository).insertOrUpdate(argThat(update -> {
			assertThat(update.firstDate).isEqualTo(LocalDate.of(2025, 12, 19));
			assertThat(update.lastDate).isEqualTo(LocalDate.of(2025, 12, 25));
			assertThat(update.complete).isTrue();
			assertThat(update.valid).isTrue();
			return true;
		}));
	}

	@Test
	public void whenHasImportVersionThatIsCompleteAndValid_andNewTimeRangeHasEarlierStart_thenImportsDataForMissingDatesAtBeginning_andUpdatesImportVersionCorrectly() {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 23);
		importVersion.lastDate = LocalDate.of(2025, 12, 27);
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(importerFactory).createImporter(argThat(properties -> {
			assertThat(properties.getFirstCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(properties.getLastCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 22));
			return true;
		}), any());
		verify(importVersionRepository).insertOrUpdate(argThat(update -> {
			assertThat(update.firstDate).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(update.lastDate).isEqualTo(LocalDate.of(2025, 12, 27));
			assertThat(update.complete).isTrue();
			assertThat(update.valid).isTrue();
			return true;
		}));
	}

	@Test
	public void whenHasImportVersionThatIsCompleteAndInvalid_thenDoesNotImportData() {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.valid = false;
		importVersion.firstDate = LocalDate.of(2025, 12, 19);
		importVersion.lastDate = LocalDate.of(2025, 12, 23);
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verifyNoInteractions(importerFactory);
		verify(importVersionRepository, never()).insertOrUpdate(any());
	}

	@Test
	public void whenHasImportVersionWithMissingDirectory_thenExtractsFileAgain_andUpdatesImportVersion()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_version1.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectory(URI_2025)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		final ImportVersion importVersion = createIncompleteImportVersion();
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		when(filesystemWrapper.exists(new File(importVersion.directory))).thenReturn(false);

		importScheduler.runPeriodicImportTasks();

		verify(downloader, never()).downloadFileFromUrlToTemporaryDirectory(any());
		verify(downloader).extractZipFileToTemporarySubfolder(new File("zipfile"));

		verify(importerFactory).createImporter(argThat(properties -> {
			assertThat(properties.getFirstCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(properties.getLastCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 25));
			return true;
		}), any());
		verify(importVersionRepository, times(2)).insertOrUpdate(any());
	}

	@Test
	public void whenHasImportVersionWithMissingDirectoryAndZipFile_thenDownloadsAndExtractsFileAgain_andUpdatesImportVersionTwoTimes()
			throws IOException, InterruptedException {
		final NetexFile netexFile = new NetexFile(new File("somezipfile_with_version1.zip"), "etag");
		when(downloader.downloadFileFromUrlToTemporaryDirectory(URI_2025)).thenReturn(netexFile);
		when(downloader.extractZipFileToTemporarySubfolder(any())).thenReturn(new File("somedirectory"));

		final ImportVersion importVersion = createIncompleteImportVersion();
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		when(filesystemWrapper.exists(new File(importVersion.directory))).thenReturn(false);
		when(filesystemWrapper.exists(new File(importVersion.zipFile))).thenReturn(false);

		importScheduler.runPeriodicImportTasks();

		verify(downloader).downloadFileFromUrlToTemporaryDirectory(URI_2025);
		verify(downloader).extractZipFileToTemporarySubfolder(new File("somezipfile_with_version1.zip").getAbsoluteFile());

		verify(importerFactory).createImporter(argThat(properties -> {
			assertThat(properties.getFirstCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 21));
			assertThat(properties.getLastCalendarDay()).isEqualTo(LocalDate.of(2025, 12, 25));
			return true;
		}), any());
		verify(importVersionRepository, times(3)).insertOrUpdate(any());
	}

	@Test
	public void whenDatabaseContainsDataForImportDate_thenDeletesExistingData() {
		final ImportVersion importVersion = createIncompleteImportVersion();
		when(importVersionRepository.getImportVersions(eq(TIMETABLE_2025), any())).thenReturn(List.of(importVersion));

		// mock existing data for all dates
		when(netexRepository.containsDataForCalendarDay(any())).thenReturn(true);

		importScheduler.runPeriodicImportTasks();

		// expect that data for these dates is deleted
		verify(netexRepository).deleteDataForCalendarDay(LocalDate.of(2025, 12, 21));
		verify(netexRepository).deleteDataForCalendarDay(LocalDate.of(2025, 12, 22));
		verify(netexRepository).deleteDataForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(netexRepository).deleteDataForCalendarDay(LocalDate.of(2025, 12, 24));
		verify(netexRepository).deleteDataForCalendarDay(LocalDate.of(2025, 12, 25));
	}

	@Test
	public void whenMakesInitialImportForNewVersion_andThereIsPreviousVersion_andNumberOfJourneysIsEqual_thenNewVersionIsValid() {
		mockSetupForImportValidation(100, 100);

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.version.equals("version2") && update.valid));
	}

	@Test
	public void whenMakesInitialImportForNewVersion_andThereIsPreviousVersion_andNumberOfJourneysIsSlightlyDifferent_thenNewVersionIsValid() {
		mockSetupForImportValidation(100, 105);

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.version.equals("version2") && update.valid));
	}

	@Test
	public void whenMakesInitialImportForNewVersion_andThereIsPreviousVersion_butNumberOfJourneysIsVeryDifferent_thenNewVersionIsNotValid() {
		mockSetupForImportValidation(100, 150);

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.version.equals("version2") && !update.valid));
	}

	@Test
	public void whenMakesInitialImportForNewVersion_andThereIsPreviousVersion_butNumberOfJourneysIsVeryDifferent_butPreviousVersionIsNotValid_thenNewVersionIsValid() {
		mockSetupForImportValidation(100, 150);
		importVersionRepository.getImportVersions(TIMETABLE_2025, Order.OLDEST_FIRST).getFirst().valid = false;

		importScheduler.runPeriodicImportTasks();

		// expect that invalid previous version is not used to validate the new version, so the new version is still valid even though the
		// number of journeys is very different
		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.version.equals("version2") && update.valid));
	}

	private void mockSetupForImportValidation(long journeysInOldVersion, long journeysInNewVersion) {
		final ImportVersion prevVersion = createCompleteImportVersion();
		prevVersion.version = "version1";
		prevVersion.databaseName = "database1";
		final ImportVersion newVersion = createIncompleteImportVersion();
		newVersion.version = "version2";
		newVersion.databaseName = "database2";
		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.OLDEST_FIRST)).thenReturn(List.of(prevVersion, newVersion));
		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(newVersion, prevVersion));

		final NetexRepository repository1 = Mockito.mock(NetexRepository.class);
		final NetexRepository repository2 = Mockito.mock(NetexRepository.class);
		when(repository1.getNumberOfJourneysForCalendarDay(any())).thenReturn(journeysInOldVersion);
		when(repository2.getNumberOfJourneysForCalendarDay(any())).thenReturn(journeysInNewVersion);
		when(mongoClientWrapper.createNetexRepository(prevVersion.databaseName)).thenReturn(repository1);
		when(mongoClientWrapper.createNetexRepository(newVersion.databaseName)).thenReturn(repository2);
	}

	@Test
	public void whenHasImportVersion_thenCleansOldData_andUpdatesImportVersion() {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 1);
		importVersion.lastDate = LocalDate.of(2025, 12, 25);

		when(importVersionRepository.getAllImportVersions()).thenReturn(List.of(importVersion));
		when(importVersionRepository.getImportVersions(any(), any())).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(netexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 20));
		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.firstDate.isEqual(LocalDate.of(2025, 12, 21))));
		verify(importVersionRepository, never()).deleteImportVersion(any());
		verify(mongoClientWrapper, never()).dropDatabase(any());
	}

	@Test
	public void whenHasImportVersionOfOlderTimetable_thenCleansOldData_andDeletesVersionWhenAllDataIsRemoved() throws IOException {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 1);
		importVersion.lastDate = LocalDate.of(2025, 12, 25);
		importVersion.timetable = "old";

		when(importVersionRepository.getAllImportVersions()).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(netexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 20));
		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.firstDate.isEqual(LocalDate.of(2025, 12, 21))));

		// expect that version is not deleted
		verify(importVersionRepository, never()).deleteImportVersion(any());
		verify(mongoClientWrapper, never()).dropDatabase(any());
		verify(filesystemWrapper, never()).deleteFile(any());
		verify(filesystemWrapper, never()).deleteDirectory(any());

		// mock data database is empty
		when(netexRepository.isDatabaseEmpty()).thenReturn(true);

		importScheduler.runPeriodicImportTasks();

		// expect that version is deleted, including the database and all files
		verify(importVersionRepository).deleteImportVersion(any());
		verify(mongoClientWrapper).dropDatabase(any());
		verify(filesystemWrapper).deleteFile(new File("zipfile"));
		verify(filesystemWrapper).deleteDirectory(new File("directory"));
	}

	@Test
	public void whenHasImportVersionOfOlderSchema_thenCleansOldData_andDeletesVersionWhenAllDataIsRemoved() throws IOException {
		final ImportVersion importVersion = createCompleteImportVersion();
		importVersion.firstDate = LocalDate.of(2025, 12, 1);
		importVersion.lastDate = LocalDate.of(2025, 12, 25);
		importVersion.schemaVersion = 0;

		when(importVersionRepository.getAllImportVersions()).thenReturn(List.of(importVersion));

		importScheduler.runPeriodicImportTasks();

		verify(netexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 20));
		verify(importVersionRepository).insertOrUpdate(argThat(update -> update.firstDate.isEqual(LocalDate.of(2025, 12, 21))));

		// expect that version is not deleted
		verify(importVersionRepository, never()).deleteImportVersion(any());
		verify(mongoClientWrapper, never()).dropDatabase(any());
		verify(filesystemWrapper, never()).deleteFile(any());
		verify(filesystemWrapper, never()).deleteDirectory(any());

		// mock data database is empty
		when(netexRepository.isDatabaseEmpty()).thenReturn(true);

		importScheduler.runPeriodicImportTasks();

		// expect that version is deleted, including the database and all files
		verify(importVersionRepository).deleteImportVersion(any());
		verify(mongoClientWrapper).dropDatabase(any());
		verify(filesystemWrapper).deleteFile(new File("zipfile"));
		verify(filesystemWrapper).deleteDirectory(new File("directory"));
	}

	@Test
	public void whenHasFewerImportVersionsThanRequired_thenDeletesNoVersion() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(version1, version2));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, never()).deleteImportVersion(any());
	}

	@Test
	public void whenHasExactlyAsManyImportVersionsThanRequired_thenDeletesNoVersion() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";
		final ImportVersion version3 = createCompleteImportVersion();
		version3.version = "version3";

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(version1, version2, version3));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, never()).deleteImportVersion(any());
	}

	@Test
	public void whenHasMoreImportVersionsThanRequired_thenDeletesOldestVersion() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";
		final ImportVersion version3 = createCompleteImportVersion();
		version3.version = "version3";
		final ImportVersion version4 = createCompleteImportVersion();
		version4.version = "version4";

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(version1, version2, version3, version4));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, times(1)).deleteImportVersion(any());
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version4")));
	}

	@Test
	public void whenHasExactlyAsManyImportVersionsThanRequired_andOneNewVersionThatIsInvalid_thenDeletesNoVersion() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		version1.valid = false;
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";
		final ImportVersion version3 = createCompleteImportVersion();
		version3.version = "version3";
		final ImportVersion version4 = createCompleteImportVersion();
		version4.version = "version4";

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(version1, version2, version3, version4));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, never()).deleteImportVersion(any());
	}

	@Test
	public void whenHasExactlyAsManyImportVersionsThanRequired_andOneOldVersionThatIsInvalid_thenDeletesOldVersion() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";
		final ImportVersion version3 = createCompleteImportVersion();
		version3.version = "version3";
		final ImportVersion version4 = createCompleteImportVersion();
		version4.version = "version4";
		version4.valid = false;

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST)).thenReturn(List.of(version1, version2, version3, version4));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, times(1)).deleteImportVersion(any());
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version4")));
	}

	@Test
	public void whenHasMultipleImportVersions_thenDeletesUnusedOnes() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.version = "version1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.version = "version2";
		version2.valid = false; // invalid
		final ImportVersion version3 = createIncompleteImportVersion(); // incomplete
		version3.version = "version3";
		final ImportVersion version4 = createCompleteImportVersion();
		version4.version = "version4";
		final ImportVersion version5 = createCompleteImportVersion();
		version5.version = "version5";
		final ImportVersion version6 = createCompleteImportVersion();
		version6.version = "version6";
		final ImportVersion version7 = createCompleteImportVersion();
		version7.version = "version7";
		version7.force = true; // force
		final ImportVersion version8 = createCompleteImportVersion();
		version8.version = "version8";
		final ImportVersion version9 = createCompleteImportVersion();
		version9.version = "version9";
		version9.keep = true; // keep
		final ImportVersion version10 = createCompleteImportVersion();
		version10.version = "version10";
		version10.valid = false; // invalid
		final ImportVersion version11 = createIncompleteImportVersion(); // incomplete
		version11.version = "version11";

		when(importVersionRepository.getImportVersions(TIMETABLE_2025, Order.NEWEST_FIRST))
				.thenReturn(List.of(version1, version2, version3, version4, version5, version6, version7, version8, version9, version10, version11));

		importScheduler.runPeriodicImportTasks();

		verify(importVersionRepository, times(4)).deleteImportVersion(any());
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version6")));
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version8")));
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version10")));
		verify(importVersionRepository).deleteImportVersion(argThat(version -> version.version.equals("version11")));
	}

	@Test
	public void whenHasUnkownDatabases_thenDeletesThem() {
		properties.setImportDatabasePrefix("prefix");
		when(mongoClientWrapper.listDatabaseNames()).thenReturn(List.of("asdf", "prefix-something", "prefix-name", "prefix-other"));

		final ImportVersion version = createCompleteImportVersion();
		version.databaseName = "prefix-name";
		when(importVersionRepository.getAllImportVersions()).thenReturn(List.of(version));

		importScheduler.runPeriodicImportTasks();

		verify(mongoClientWrapper, times(2)).dropDatabase(any());
		verify(mongoClientWrapper).dropDatabase("prefix-something");
		verify(mongoClientWrapper).dropDatabase("prefix-other");
	}

	@Test
	public void whenHasUnknownFilesInTemporaryDirectory_thenDeletesThem() throws IOException {
		final ImportVersion version = createCompleteImportVersion();
		version.zipFile = new File("zipfile").getAbsolutePath();
		version.directory = new File("directory").getAbsolutePath();
		when(importVersionRepository.getAllImportVersions()).thenReturn(List.of(version));

		when(filesystemWrapper.listFiles(any())).thenReturn(List.of(
				new File("zipfile"),
				new File("some-file"),
				new File("other-file"),
				new File("directory"),
				new File("some-dir"),
				new File("other-dir")));
		when(filesystemWrapper.isFile(new File("zipfile"))).thenReturn(true);
		when(filesystemWrapper.isFile(new File("some-file"))).thenReturn(true);
		when(filesystemWrapper.isFile(new File("other-file"))).thenReturn(true);
		when(filesystemWrapper.isDirectory(new File("directory"))).thenReturn(true);
		when(filesystemWrapper.isDirectory(new File("some-dir"))).thenReturn(true);
		when(filesystemWrapper.isDirectory(new File("other-dir"))).thenReturn(true);

		importScheduler.runPeriodicImportTasks();

		verify(filesystemWrapper, times(2)).deleteDirectory(any());
		verify(filesystemWrapper).deleteDirectory(new File("some-dir"));
		verify(filesystemWrapper).deleteDirectory(new File("other-dir"));
		verify(filesystemWrapper, times(2)).deleteFile(any());
		verify(filesystemWrapper).deleteFile(new File("some-file"));
		verify(filesystemWrapper).deleteFile(new File("other-file"));
	}

	@Test
	public void whenHasActiveVersions_thenUpdatesHistoryDatabase() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		final NetexRepository repository1 = Mockito.mock(NetexRepository.class);
		final NetexRepository repository2 = Mockito.mock(NetexRepository.class);
		when(mongoClientWrapper.createNetexRepository(version1.databaseName)).thenReturn(repository1);
		when(mongoClientWrapper.createNetexRepository(version2.databaseName)).thenReturn(repository2);

		properties.setHistoryNumberOfDays(10);

		importScheduler.runPeriodicImportTasks();

		verify(repository1).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository1).getCallsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository1).getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository1).getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository1).getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));

		verify(repository2).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository2).getCallsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository2).getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository2).getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(repository2).getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));

		verify(historyNetexRepository, times(2)).writeJourneys(any());
		verify(historyNetexRepository, times(2)).writeCalls(any());
		verify(historyNetexRepository, times(2)).writeJourneyAggregations(any());
		verify(historyNetexRepository, times(2)).writeCallAggregations(any());
		verify(historyNetexRepository, times(2)).writeRouteAggregations(any());

		verify(historyNetexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 13));
	}

	@Test
	public void whenHasActiveVersions_andHistoryDatabaseAlreadyHasDataForToday_thenDoesNotUpdateHistoryDatabase() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		when(historyNetexRepository.containsDataForCalendarDay(LocalDate.of(2025, 12, 23))).thenReturn(true);

		importScheduler.runPeriodicImportTasks();

		verify(netexRepository, never()).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(netexRepository, never()).getCallsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(netexRepository, never()).getJourneyAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(netexRepository, never()).getCallAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(netexRepository, never()).getRouteAggregationsForCalendarDay(LocalDate.of(2025, 12, 23));

		verify(historyNetexRepository, never()).writeJourneys(any());
		verify(historyNetexRepository, never()).writeCalls(any());
		verify(historyNetexRepository, never()).writeJourneyAggregations(any());
		verify(historyNetexRepository, never()).writeCallAggregations(any());
		verify(historyNetexRepository, never()).writeRouteAggregations(any());
	}

	@Test
	public void whenUpdatingHistoryDatabaseThrows_thenStillUpdatesOtherVersions() {
		final ImportVersion version1 = createCompleteImportVersion();
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = createCompleteImportVersion();
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		when(netexRepository.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));

		properties.setHistoryNumberOfDays(10);

		importScheduler.runPeriodicImportTasks();

		verify(netexRepository, times(2)).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(historyNetexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 13));
	}

	private ImportVersion createIncompleteImportVersion() {
		final ImportVersion importVersion = new ImportVersion();
		importVersion.timetable = TIMETABLE_2025;
		importVersion.version = "version";
		importVersion.uri = URI_2025.toString();
		importVersion.zipFile = "zipfile";
		importVersion.directory = "directory";
		importVersion.databaseName = "database";
		importVersion.complete = false;
		return importVersion;
	}

	private ImportVersion createCompleteImportVersion() {
		final ImportVersion importVersion = createIncompleteImportVersion();
		importVersion.complete = true;
		importVersion.valid = true;
		importVersion.firstDate = LocalDate.of(2025, 12, 21);
		importVersion.lastDate = LocalDate.of(2025, 12, 25);
		return importVersion;
	}
}
