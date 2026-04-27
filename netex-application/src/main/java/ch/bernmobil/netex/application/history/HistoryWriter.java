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
import ch.bernmobil.netex.persistence.admin.TaskRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.model.CallWithJourney;
import ch.bernmobil.netex.persistence.model.ImportVersion;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;
import ch.bernmobil.netex.persistence.model.task.HistoryTask;

public class HistoryWriter {

	private static final Logger logger = LoggerFactory.getLogger(HistoryWriter.class);

	private final ImportSchedulerProperties properties;
	private final NetexRepository historyNetexRepository;
	private final TaskRepository taskRepository;
	private final ImportVersionRepository importVersionRepository;
	private final MongoClientWrapper mongoClientWrapper;
	private final Clock clock;

	public HistoryWriter(ImportSchedulerProperties properties, NetexRepository historyNetexRepository, TaskRepository taskRepository,
			ImportVersionRepository importVersionRepository, MongoClientWrapper mongoClientWrapper, Clock clock) {
		this.properties = properties;
		this.historyNetexRepository = historyNetexRepository;
		this.taskRepository = taskRepository;
		this.importVersionRepository = importVersionRepository;
		this.mongoClientWrapper = mongoClientWrapper;
		this.clock = clock;
	}

	public void updateHistoryIfNecessary() {
		final LocalDate today = LocalDate.now(clock);

		HistoryTask task = taskRepository.getHistoryTask();
		if (task == null) {
			task = new HistoryTask();
			task.setHistoryExportedUntil(today.minusDays(1));
		}

		final LocalDate start = task.getHistoryExportedUntil().plusDays(1);
		for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
			updateHistory(date);

			task.setHistoryExportedUntil(date);
			taskRepository.updateHistoryTask(task);
		}
	}

	private void updateHistory(LocalDate date) {
		// check if database is in a clean state, remove incomplete day if necessary
		if (historyNetexRepository.containsDataForCalendarDay(date)) {
			logger.warn("history database is in an unclean state, remove journeys for calendar day {}", date);
			historyNetexRepository.deleteDataForCalendarDay(date);
		}

		// copy data from the currently active versions to the history database
		final Collection<ImportVersion> activeVersions = importVersionRepository.getActiveImportVersions();
		for (final ImportVersion activeVersion : activeVersions) {
			try {
				updateHistory(activeVersion, date);
			} catch (RuntimeException e) {
				logger.error("failed to update history database for version " + activeVersion.version + " of " + activeVersion.timetable,
						e);
			}
		}

		// delete old data from history database
		final LocalDate cleanupThreshold = date.minusDays(properties.getHistoryNumberOfDays());
		logger.info("delete old data in history database up to {}", cleanupThreshold);
		historyNetexRepository.deleteDataUpToCalendarDay(cleanupThreshold);
	}

	private void updateHistory(ImportVersion version, LocalDate date) {
		logger.info("adding data for {} from version {} of {} to history database", date, version.version, version.timetable);
		final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(version.databaseName);

		final List<JourneyWithCalls> journeys = netexRepository.getJourneysForCalendarDay(date);
		final List<CallWithJourney> calls = netexRepository.getCallsForCalendarDay(date);

		// override ids to avoid duplicates between different versions (e.g. if the same journey changes calendar day between two versions
		// then it might be inserted twice into the history database; also different versions could just use a different numbering system
		// which could lead to colliding ids).
		journeys.forEach(journey -> journey.id = journey.id + "_" + version.getId());
		calls.forEach(call -> call.id = call.id + "_" + version.getId());

		historyNetexRepository.writeJourneys(journeys);
		historyNetexRepository.writeCalls(calls);
		historyNetexRepository.writeJourneyAggregations(netexRepository.getJourneyAggregationsForCalendarDay(date));
		historyNetexRepository.writeCallAggregations(netexRepository.getCallAggregationsForCalendarDay(date));
		historyNetexRepository.writeRouteAggregations(netexRepository.getRouteAggregationsForCalendarDay(date));
	}
}
