package ch.bernmobil.netex.application.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.application.scheduler.ImportSchedulerProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.TaskRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.model.ImportVersion;
import ch.bernmobil.netex.persistence.model.task.HistoryTask;

public class HistoryWriterTest {

	private static final String TIMETABLE_2025 = "2025";
	private static final String TIMETABLE_2026 = "2026";

	private static final LocalDate TODAY = LocalDate.of(2025, 12, 23);

	private HistoryWriter historyWriter;
	private ImportSchedulerProperties properties;
	private NetexRepository historyNetexRepository;
	private TaskRepository taskRepository;
	private ImportVersionRepository importVersionRepository;
	private MongoClientWrapper mongoClientWrapper;
	private Clock clock;

	private HistoryTask task;

	@BeforeEach
	public void setup() {
		properties = new ImportSchedulerProperties();
		properties.setHistoryNumberOfDays(10);
		properties.setHistoryExportTimeOfDay(LocalTime.of(12, 0));

		historyNetexRepository = Mockito.mock(NetexRepository.class);
		taskRepository = Mockito.mock(TaskRepository.class);
		when(taskRepository.getHistoryTask()).thenAnswer(args -> task);
		doAnswer(args -> task = args.getArgument(0)).when(taskRepository).updateHistoryTask(any());

		importVersionRepository = Mockito.mock(ImportVersionRepository.class);
		mongoClientWrapper = Mockito.mock(MongoClientWrapper.class);
		clock = Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.of(13, 0, 0), ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

		historyWriter = new HistoryWriter(properties, historyNetexRepository, taskRepository, importVersionRepository, mongoClientWrapper, clock);
	}

	@Nested
	public class WhenHasOneActiveVersion {

		private NetexRepository repository;

		@BeforeEach
		public void setup() {
			repository = mockActiveVersion();
		}

		@Nested
		public class AndHistoryTaskIsNull {

			@BeforeEach
			public void setup() {
				assertThat(taskRepository.getHistoryTask()).isNull();
			}

			@Test
			public void thenExportsHistoryForToday_andUpdatesTask() {
				historyWriter.updateHistoryIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertWritesHistoryFor(TODAY);

				assertThat(taskRepository.getHistoryTask()).isNotNull();
				assertThat(taskRepository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(TODAY);
			}

			@Test
			public void thenExportsHistoryForToday_ifTimeOfDayIsLateEnough() {
				// simulated time is 13:00, cutoff time is 13:00 -> no export expected
				properties.setHistoryExportTimeOfDay(LocalTime.of(13, 0));
				historyWriter.updateHistoryIfNecessary();
				assertReadsNoData(repository);
				assertWritesNoHistory();

				// simulated time is 13:00, cutoff time is 12:59 -> export expected
				properties.setHistoryExportTimeOfDay(LocalTime.of(12, 59));
				historyWriter.updateHistoryIfNecessary();
				assertReadsDataFor(repository, TODAY);
				assertWritesHistoryFor(TODAY);
			}
		}

		@Nested
		public class AndHistoryTaskWasUpdatedForToday {

			@BeforeEach
			public void setup() {
				task = new HistoryTask();
				task.setHistoryExportedUntil(TODAY);
			}

			@Test
			public void thenExportsNothing() {
				historyWriter.updateHistoryIfNecessary();

				assertReadsNoData(repository);
				assertWritesNoHistory();

				assertThat(taskRepository.getHistoryTask()).isNotNull();
				assertThat(taskRepository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(TODAY);
			}
		}

		@Nested
		public class AndHistoryTaskWasUpdatedForYesterday {

			@BeforeEach
			public void setup() {
				task = new HistoryTask();
				task.setHistoryExportedUntil(TODAY.minusDays(1));
			}

			@Test
			public void thenExportsToday_andUpdatesTask() {
				historyWriter.updateHistoryIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertWritesHistoryFor(TODAY);

				assertThat(taskRepository.getHistoryTask()).isNotNull();
				assertThat(taskRepository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(TODAY);
			}

			@Test
			public void thenExportsToday_ifTimeOfDayIsLateEnough() {
				// simulated time is 13:00, cutoff time is 13:00 -> no export expected
				properties.setHistoryExportTimeOfDay(LocalTime.of(13, 0));
				historyWriter.updateHistoryIfNecessary();
				assertReadsNoData(repository);
				assertWritesNoHistory();

				// simulated time is 13:00, cutoff time is 12:59 -> export expected
				properties.setHistoryExportTimeOfDay(LocalTime.of(12, 59));
				historyWriter.updateHistoryIfNecessary();
				assertReadsDataFor(repository, TODAY);
				assertWritesHistoryFor(TODAY);
			}
		}

		@Nested
		public class AndHistoryTaskWasUpdatedForThreeDaysBefore {

			@BeforeEach
			public void setup() {
				task = new HistoryTask();
				task.setHistoryExportedUntil(TODAY.minusDays(3));
			}

			@Test
			public void thenExportsThreeDays_andUpdatesTask() {
				historyWriter.updateHistoryIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.minusDays(1));
				assertReadsDataFor(repository, TODAY.minusDays(2));
				assertWritesHistoryFor(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

				assertThat(taskRepository.getHistoryTask()).isNotNull();
				assertThat(taskRepository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(TODAY);
			}

			@Test
			public void andHistoryDatabaseAlreadyContainsDataForDate_thenDeletesIt() {
				when(historyNetexRepository.containsDataForCalendarDay(TODAY.minusDays(1))).thenReturn(true);

				historyWriter.updateHistoryIfNecessary();

				// verify that data was deleted exactly for today-1 (and no other dates)
				verify(historyNetexRepository).deleteDataForCalendarDay(any());
				verify(historyNetexRepository).deleteDataForCalendarDay(TODAY.minusDays(1));

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.minusDays(1));
				assertReadsDataFor(repository, TODAY.minusDays(2));
				assertWritesHistoryFor(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
			}
		}

		private NetexRepository mockActiveVersion() {
			final ImportVersion version = new ImportVersion();
			version.version = "version";
			version.timetable = TIMETABLE_2026;
			version.databaseName = "database";
			when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version));

			final NetexRepository repository = Mockito.mock(NetexRepository.class);
			when(mongoClientWrapper.createNetexRepository(version.databaseName)).thenReturn(repository);

			return repository;
		}
	}

	@Nested
	public class WhenHasTwoActiveVersions {

		private NetexRepository repository1;
		private NetexRepository repository2;

		@BeforeEach
		public void setup() {
			final ImportVersion version1 = new ImportVersion();
			version1.version = "version";
			version1.timetable = TIMETABLE_2025;
			version1.databaseName = "database1";
			final ImportVersion version2 = new ImportVersion();
			version2.version = "version";
			version2.timetable = TIMETABLE_2026;
			version2.databaseName = "database2";
			when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

			repository1 = Mockito.mock(NetexRepository.class);
			repository2 = Mockito.mock(NetexRepository.class);
			when(mongoClientWrapper.createNetexRepository(version1.databaseName)).thenReturn(repository1);
			when(mongoClientWrapper.createNetexRepository(version2.databaseName)).thenReturn(repository2);
		}

		@Test
		public void thenExportsBoth() {
			historyWriter.updateHistoryIfNecessary();

			assertReadsDataFor(repository1, TODAY);
			assertReadsDataFor(repository2, TODAY);
			assertWritesHistoryFor(TODAY, TODAY);
		}

		@Test
		public void andUpdatingHistoryDatabaseThrows_thenStillUpdatesOtherVersions() {
			when(repository1.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));
			when(repository2.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));

			historyWriter.updateHistoryIfNecessary();

			verify(repository1).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
			verify(repository2).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
			verify(historyNetexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 13));
		}
	}

	private void assertReadsDataFor(NetexRepository repository, LocalDate date) {
		verify(repository).getJourneysForCalendarDay(date);
		verify(repository).getCallsForCalendarDay(date);
		verify(repository).getJourneyAggregationsForCalendarDay(date);
		verify(repository).getCallAggregationsForCalendarDay(date);
		verify(repository).getRouteAggregationsForCalendarDay(date);
	}

	private void assertWritesHistoryFor(LocalDate ... dates) {
		for (final LocalDate date : dates) {
			verify(historyNetexRepository).containsDataForCalendarDay(date);
		}

		verify(historyNetexRepository, times(dates.length)).writeJourneys(any());
		verify(historyNetexRepository, times(dates.length)).writeCalls(any());
		verify(historyNetexRepository, times(dates.length)).writeJourneyAggregations(any());
		verify(historyNetexRepository, times(dates.length)).writeCallAggregations(any());
		verify(historyNetexRepository, times(dates.length)).writeRouteAggregations(any());

		for (final LocalDate date : dates) {
			verify(historyNetexRepository).deleteDataUpToCalendarDay(date.minusDays(10));
		}

		verifyNoMoreInteractions(historyNetexRepository);
	}

	private void assertReadsNoData(NetexRepository repository) {
		verifyNoInteractions(repository);
	}

	private void assertWritesNoHistory() {
		verifyNoInteractions(historyNetexRepository);
	}
}
