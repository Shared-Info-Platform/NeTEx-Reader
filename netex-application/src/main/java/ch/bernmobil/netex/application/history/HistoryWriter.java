package ch.bernmobil.netex.application.history;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.application.scheduler.ImportSchedulerProperties;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.model.CallWithJourney;
import ch.bernmobil.netex.persistence.model.ImportVersion;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;

public class HistoryWriter {

	private static final Logger logger = LoggerFactory.getLogger(HistoryWriter.class);

	private final ImportSchedulerProperties properties;
	private final NetexRepository historyNetexRepository;
	private final ImportVersionRepository importVersionRepository;
	private final MongoClientWrapper mongoClientWrapper;
	private final Clock clock;

	public HistoryWriter(ImportSchedulerProperties properties, NetexRepository historyNetexRepository,
			ImportVersionRepository importVersionRepository, MongoClientWrapper mongoClientWrapper, Clock clock) {
		this.properties = properties;
		this.historyNetexRepository = historyNetexRepository;
		this.importVersionRepository = importVersionRepository;
		this.mongoClientWrapper = mongoClientWrapper;
		this.clock = clock;
	}

	public void updateHistoryIfNecessary() {
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
}
