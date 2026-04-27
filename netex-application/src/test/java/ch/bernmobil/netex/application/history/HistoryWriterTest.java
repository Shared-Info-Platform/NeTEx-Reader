package ch.bernmobil.netex.application.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.application.scheduler.ImportSchedulerProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.model.ImportVersion;

public class HistoryWriterTest {

	private static final String TIMETABLE_2025 = "2025";
	private static final String TIMETABLE_2026 = "2026";

	private HistoryWriter historyWriter;
	private ImportSchedulerProperties properties;
	private NetexRepository historyNetexRepository;
	private ImportVersionRepository importVersionRepository;
	private NetexRepository netexRepository;
	private MongoClientWrapper mongoClientWrapper;
	private Clock clock;

	@BeforeEach
	public void setup() {
		properties = new ImportSchedulerProperties();

		historyNetexRepository = Mockito.mock(NetexRepository.class);
		importVersionRepository = Mockito.mock(ImportVersionRepository.class);
		mongoClientWrapper = Mockito.mock(MongoClientWrapper.class);
		netexRepository = Mockito.mock(NetexRepository.class);
		when(mongoClientWrapper.createNetexRepository(any())).thenReturn(netexRepository);
		clock = Clock.fixed(ZonedDateTime.of(2025, 12, 23, 12, 0, 0, 0, ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

		historyWriter = new HistoryWriter(properties, historyNetexRepository, importVersionRepository, mongoClientWrapper, clock);
	}

	@Test
	public void whenHasActiveVersions_thenUpdatesHistoryDatabase() {
		final ImportVersion version1 = new ImportVersion();
		version1.version = "version";
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = new ImportVersion();
		version2.version = "version";
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		final NetexRepository repository1 = Mockito.mock(NetexRepository.class);
		final NetexRepository repository2 = Mockito.mock(NetexRepository.class);
		when(mongoClientWrapper.createNetexRepository(version1.databaseName)).thenReturn(repository1);
		when(mongoClientWrapper.createNetexRepository(version2.databaseName)).thenReturn(repository2);

		properties.setHistoryNumberOfDays(10);

		historyWriter.updateHistoryIfNecessary();

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
		final ImportVersion version1 = new ImportVersion();
		version1.version = "version";
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = new ImportVersion();
		version2.version = "version";
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		when(historyNetexRepository.containsDataForCalendarDay(LocalDate.of(2025, 12, 23))).thenReturn(true);

		historyWriter.updateHistoryIfNecessary();

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
		final ImportVersion version1 = new ImportVersion();
		version1.version = "version";
		version1.timetable = TIMETABLE_2025;
		version1.databaseName = "database1";
		final ImportVersion version2 = new ImportVersion();
		version2.version = "version";
		version2.timetable = TIMETABLE_2026;
		version2.databaseName = "database2";
		when(importVersionRepository.getActiveImportVersions()).thenReturn(List.of(version1, version2));

		when(netexRepository.getJourneysForCalendarDay(any())).thenThrow(new RuntimeException("test"));

		properties.setHistoryNumberOfDays(10);

		historyWriter.updateHistoryIfNecessary();

		verify(netexRepository, times(2)).getJourneysForCalendarDay(LocalDate.of(2025, 12, 23));
		verify(historyNetexRepository).deleteDataUpToCalendarDay(LocalDate.of(2025, 12, 13));
	}
}
