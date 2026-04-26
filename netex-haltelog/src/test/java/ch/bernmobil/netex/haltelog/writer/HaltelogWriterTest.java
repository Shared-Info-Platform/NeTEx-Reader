package ch.bernmobil.netex.haltelog.writer;

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
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.bernmobil.netex.haltelog.properties.HaltelogProperties;
import ch.bernmobil.netex.haltelog.repository.HaltelogRepository;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.TaskRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.helper.MongoClientWrapper;
import ch.bernmobil.netex.persistence.model.Call;
import ch.bernmobil.netex.persistence.model.ImportVersion;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;
import ch.bernmobil.netex.persistence.model.task.HaltelogTask;

public class HaltelogWriterTest {

	private static final String TIMETABLE_2025 = "2025";
	private static final String TIMETABLE_2026 = "2026";

	private static final String VERSION = "1_2026_version";

	private static final LocalDate TODAY = LocalDate.of(2025, 12, 23);

	private HaltelogWriter haltelogWriter;
	private HaltelogProperties properties;
	private HaltelogRepository haltelogRepository;
	private TaskRepository taskRepository;
	private ImportVersionRepository importVersionRepository;
	private MongoClientWrapper mongoClientWrapper;
	private Clock clock;

	private HaltelogTask task;

	@BeforeEach
	public void setup() {
		properties = new HaltelogProperties();
		properties.setWriteHaltelog(true);
		properties.setHaltelogExportDaysInFuture(0);
		properties.setHaltelogExportTimeOfDay(LocalTime.of(12, 0));

		haltelogRepository = Mockito.mock(HaltelogRepository.class);
		taskRepository = Mockito.mock(TaskRepository.class);
		when(taskRepository.getHaltelogTask()).thenAnswer(args -> task);
		doAnswer(args -> task = args.getArgument(0)).when(taskRepository).updateHaltelogTask(any());

		importVersionRepository = Mockito.mock(ImportVersionRepository.class);
		mongoClientWrapper = Mockito.mock(MongoClientWrapper.class);
		clock = Clock.fixed(ZonedDateTime.of(TODAY, LocalTime.of(13, 0, 0), ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

		haltelogWriter = new HaltelogWriter(properties, haltelogRepository, taskRepository, importVersionRepository, mongoClientWrapper, clock);
	}

	@Nested
	public class WhenHasOneActiveVersion {

		private NetexRepository repository;

		@BeforeEach
		public void setup() {
			repository = mockActiveVersion();
		}

		@Nested
		public class AndHaltelogTaskIsNull {

			@BeforeEach
			public void setup() {
				assertThat(taskRepository.getHaltelogTask()).isNull();
			}

			@Test
			public void thenExportsHaltelogForToday_andUpdatesTask() {
				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertWritesHaltelogFor(TODAY);

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY);
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void thenExportsHaltelogForToday_ifTimeOfDayIsLateEnough() {
				// simulated time is 13:00, cutoff time is 13:00 -> no export expected
				properties.setHaltelogExportTimeOfDay(LocalTime.of(13, 0));
				haltelogWriter.updateHaltelogIfNecessary();
				assertReadsNoData(repository);
				assertWritesNoHaltelog();

				// simulated time is 13:00, cutoff time is 12:59 -> export expected
				properties.setHaltelogExportTimeOfDay(LocalTime.of(12, 59));
				haltelogWriter.updateHaltelogIfNecessary();
				assertReadsDataFor(repository, TODAY);
				assertWritesHaltelogFor(TODAY);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForThreeDays_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(2);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.plusDays(1));
				assertReadsDataFor(repository, TODAY.plusDays(2));
				assertWritesHaltelogFor(TODAY, TODAY.plusDays(1), TODAY.plusDays(2));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}
		}

		@Nested
		public class AndHaltelogTaskWasUpdatedForToday {

			@BeforeEach
			public void setup() {
				task = new HaltelogTask();
				task.setHaltelogExportedUntil(TODAY);
				task.setLastExportedVersions(Set.of(VERSION));
			}

			@Test
			public void thenExportsNothing() {
				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsNoData(repository);
				assertWritesNoHaltelog();

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY);
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForTwoMoreDays_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(2);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY.plusDays(1));
				assertReadsDataFor(repository, TODAY.plusDays(2));
				assertWritesHaltelogFor(TODAY.plusDays(1), TODAY.plusDays(2));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Nested
			public class ButForAnotherVersion {

				@BeforeEach
				public void setup() {
					task.setLastExportedVersions(Set.of("other"));
				}

				@Test
				public void thenExportsNothing() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForToday.this.thenExportsNothing();
				}

				@Test
				public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForTwoMoreDays_andUpdatesTask() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForToday.this.andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForTwoMoreDays_andUpdatesTask();
				}
			}
		}

		@Nested
		public class AndHaltelogTaskWasUpdatedForYesterday {

			@BeforeEach
			public void setup() {
				task = new HaltelogTask();
				task.setHaltelogExportedUntil(TODAY.minusDays(1));
				task.setLastExportedVersions(Set.of(VERSION));
			}

			@Test
			public void thenExportsToday_andUpdatesTask() {
				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertWritesHaltelogFor(TODAY);

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY);
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void thenExportsToday_ifTimeOfDayIsLateEnough() {
				// simulated time is 13:00, cutoff time is 13:00 -> no export expected
				properties.setHaltelogExportTimeOfDay(LocalTime.of(13, 0));
				haltelogWriter.updateHaltelogIfNecessary();
				assertReadsNoData(repository);
				assertWritesNoHaltelog();

				// simulated time is 13:00, cutoff time is 12:59 -> export expected
				properties.setHaltelogExportTimeOfDay(LocalTime.of(12, 59));
				haltelogWriter.updateHaltelogIfNecessary();
				assertReadsDataFor(repository, TODAY);
				assertWritesHaltelogFor(TODAY);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForThreeDays_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(2);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.plusDays(1));
				assertReadsDataFor(repository, TODAY.plusDays(2));
				assertWritesHaltelogFor(TODAY, TODAY.plusDays(1), TODAY.plusDays(2));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Nested
			public class ButForAnotherVersion {

				@BeforeEach
				public void setup() {
					task.setLastExportedVersions(Set.of("other"));
				}

				@Test
				public void thenExportsToday_andUpdatesTask() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForYesterday.this.thenExportsToday_andUpdatesTask();
				}

				@Test
				public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForThreeDays_andUpdatesTask() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForYesterday.this.andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForThreeDays_andUpdatesTask();
				}
			}
		}

		@Nested
		public class AndHaltelogTaskWasUpdatedForThreeDaysBefore {

			@BeforeEach
			public void setup() {
				task = new HaltelogTask();
				task.setHaltelogExportedUntil(TODAY.minusDays(3));
				task.setLastExportedVersions(Set.of(VERSION));
			}

			@Test
			public void thenExportsThreeDays_andUpdatesTask() {
				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.minusDays(1));
				assertReadsDataFor(repository, TODAY.minusDays(2));
				assertWritesHaltelogFor(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY);
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForFiveDays_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(2);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY.minusDays(2));
				assertReadsDataFor(repository, TODAY.minusDays(1));
				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.plusDays(1));
				assertReadsDataFor(repository, TODAY.plusDays(2));
				assertWritesHaltelogFor(TODAY.minusDays(2), TODAY.minusDays(1), TODAY, TODAY.plusDays(1), TODAY.plusDays(2));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void andHaltelogDatabaseAlreadyContainsDataForDate_thenDeletesIt() {
				when(haltelogRepository.containsDataForCalendarDay(TODAY.minusDays(1))).thenReturn(true);

				haltelogWriter.updateHaltelogIfNecessary();

				// verify that data was deleted exactly for today-1 (and no other dates)
				verify(haltelogRepository).deleteDataForCalendarDay(any());
				verify(haltelogRepository).deleteDataForCalendarDay(TODAY.minusDays(1));

				assertReadsDataFor(repository, TODAY);
				assertReadsDataFor(repository, TODAY.minusDays(1));
				assertReadsDataFor(repository, TODAY.minusDays(2));
				assertWritesHaltelogFor(TODAY, TODAY.minusDays(1), TODAY.minusDays(2));
			}

			@Nested
			public class ButForAnotherVersion {

				@BeforeEach
				public void setup() {
					task.setLastExportedVersions(Set.of("other"));
				}

				@Test
				public void thenExportsThreeDays_andUpdatesTask() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForThreeDaysBefore.this.thenExportsThreeDays_andUpdatesTask();
				}

				@Test
				public void andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForFiveDays_andUpdatesTask() {
					// same behavior because only future days get deleted but haltelog was only exported until today
					AndHaltelogTaskWasUpdatedForThreeDaysBefore.this.andHaltelogExportDaysInFutureIsTwo_thenExportsHaltelogForFiveDays_andUpdatesTask();
				}
			}
		}

		@Nested
		public class AndHaltelogTaskWasUpdatedForTwoDaysAfter{

			@BeforeEach
			public void setup() {
				task = new HaltelogTask();
				task.setHaltelogExportedUntil(TODAY.plusDays(2));
				task.setLastExportedVersions(Set.of(VERSION));
			}

			@Test
			public void thenExportsNothing_andUpdatesTask() {
				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsNoData(repository);
				assertWritesNoHaltelog();

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsTwo_thenExportsNothing_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(2);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsNoData(repository);
				assertWritesNoHaltelog();

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Test
			public void andHaltelogExportDaysInFutureIsThree_thenExportsOneDay_andUpdatesTask() {
				properties.setHaltelogExportDaysInFuture(3);

				haltelogWriter.updateHaltelogIfNecessary();

				assertReadsDataFor(repository, TODAY.plusDays(3));
				assertWritesHaltelogFor(TODAY.plusDays(3));

				assertThat(taskRepository.getHaltelogTask()).isNotNull();
				assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(3));
				assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
			}

			@Nested
			public class ButForAnotherVersion {

				@BeforeEach
				public void setup() {
					task.setLastExportedVersions(Set.of("other"));
				}

				@Test
				public void thenDeletesFutureData_andUpdatesTask() {
					haltelogWriter.updateHaltelogIfNecessary();

					verify(haltelogRepository).deleteDataForCalendarDay(TODAY.plusDays(1));
					verify(haltelogRepository).deleteDataForCalendarDay(TODAY.plusDays(2));
					verifyNoMoreInteractions(haltelogRepository);

					assertReadsNoData(repository);

					assertThat(taskRepository.getHaltelogTask()).isNotNull();
					assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY);
					assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
				}

				@Test
				public void andHaltelogExportDaysInFutureIsTwo_thenDeletesFutureDataAndExportsItAgain_andUpdatesTask() {
					properties.setHaltelogExportDaysInFuture(2);

					haltelogWriter.updateHaltelogIfNecessary();

					verify(haltelogRepository).deleteDataForCalendarDay(TODAY.plusDays(1));
					verify(haltelogRepository).deleteDataForCalendarDay(TODAY.plusDays(2));

					assertReadsDataFor(repository, TODAY.plusDays(1));
					assertReadsDataFor(repository, TODAY.plusDays(2));
					assertWritesHaltelogFor(TODAY.plusDays(1), TODAY.plusDays(2));

					assertThat(taskRepository.getHaltelogTask()).isNotNull();
					assertThat(taskRepository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(TODAY.plusDays(2));
					assertThat(taskRepository.getHaltelogTask().getLastExportedVersions()).containsExactly(VERSION);
				}
			}
		}

		private NetexRepository mockActiveVersion() {
			final ImportVersion version = new ImportVersion();
			version.version = "version";
			version.timetable = TIMETABLE_2026;
			version.databaseName = "database";
			when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version));

			final NetexRepository repository = mockNetexRepository();
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

			repository1 = mockNetexRepository();
			repository2 = mockNetexRepository();
			when(mongoClientWrapper.createNetexRepository(version1.databaseName)).thenReturn(repository1);
			when(mongoClientWrapper.createNetexRepository(version2.databaseName)).thenReturn(repository2);
		}

		@Test
		public void thenExportsBoth() {
			haltelogWriter.updateHaltelogIfNecessary();

			assertReadsDataFor(repository1, TODAY);
			assertReadsDataFor(repository2, TODAY);
			assertWritesHaltelogFor(TODAY, TODAY);
		}

		@Test
		public void andUpdatingHaltelogDatabaseThrows_thenStillUpdatesOtherVersions() {
			when(repository1.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));
			when(repository2.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));

			haltelogWriter.updateHaltelogIfNecessary();

			verify(repository1).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
			verify(repository2).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		}
	}

	private NetexRepository mockNetexRepository() {
		final JourneyWithCalls journey = new JourneyWithCalls();
		journey.calendarDay = TODAY.toString();
		journey.operatingDay = TODAY.toString();
		journey.calls.add(new Call());

		final NetexRepository repository = Mockito.mock(NetexRepository.class);
		when(repository.getJourneysForCalendarDay(any())).thenReturn(List.of(journey));
		return repository;
	}

	private void assertReadsDataFor(NetexRepository repository, LocalDate date) {
		verify(repository).getJourneysForCalendarDay(date);
	}

	private void assertWritesHaltelogFor(LocalDate ... dates) {
		for (final LocalDate date : dates) {
			verify(haltelogRepository).containsDataForCalendarDay(date);
		}

		verify(haltelogRepository, times(dates.length)).save(any());
		verifyNoMoreInteractions(haltelogRepository);
	}

	private void assertReadsNoData(NetexRepository repository) {
		verifyNoInteractions(repository);
	}

	private void assertWritesNoHaltelog() {
		verifyNoInteractions(haltelogRepository);
	}
}
