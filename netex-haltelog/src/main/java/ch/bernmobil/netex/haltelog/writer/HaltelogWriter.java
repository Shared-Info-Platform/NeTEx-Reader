package ch.bernmobil.netex.haltelog.writer;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.haltelog.mapper.HaltelogEntryMapper;
import ch.bernmobil.netex.haltelog.model.HaltelogEntry;
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

public class HaltelogWriter {

	private static final Logger logger = LoggerFactory.getLogger(HaltelogWriter.class);

	private final HaltelogProperties properties;
	private final HaltelogRepository haltelogRepository;
	private final TaskRepository taskRepository;
	private final ImportVersionRepository importVersionRepository;
	private final MongoClientWrapper mongoClientWrapper;
	private final Clock clock;

	public HaltelogWriter(HaltelogProperties properties, HaltelogRepository haltelogRepository, TaskRepository taskRepository,
			ImportVersionRepository importVersionRepository, MongoClientWrapper mongoClientWrapper, Clock clock) {
		this.properties = properties;
		this.haltelogRepository = haltelogRepository;
		this.taskRepository = taskRepository;
		this.importVersionRepository = importVersionRepository;
		this.mongoClientWrapper = mongoClientWrapper;
		this.clock = clock;
	}

	public void updateHaltelogIfNecessary() {
		if (!properties.isWriteHaltelog()) {
			return;
		}

		final ZonedDateTime now = ZonedDateTime.now(clock);
		final LocalDate today = now.toLocalDate();

		final Collection<ImportVersion> activeVersions = importVersionRepository.getActiveImportVersions();
		final Set<String> activeVersionIds = activeVersions.stream().map(ImportVersion::getId).distinct().collect(Collectors.toSet());

		HaltelogTask task = taskRepository.getHaltelogTask();
		if (task == null) {
			task = new HaltelogTask();
			task.setHaltelogExportedUntil(today.minusDays(1));
		}

		if (!activeVersionIds.equals(task.getLastExportedVersions())) {
			if (task.getHaltelogExportedUntil().isAfter(today)) {
				logger.info("active versions have changed from {} to {}, must delete future haltelog entries", task.getLastExportedVersions(), activeVersionIds);

				// delete data that was exported for the old versions (but only for dates after today)
				for (LocalDate date = today.plusDays(1); !date.isAfter(task.getHaltelogExportedUntil()); date = date.plusDays(1)) {
					logger.info("delete haltelog entries for day {}", date);
					haltelogRepository.deleteDataForCalendarDay(date);
				}

				task.setHaltelogExportedUntil(today);
			}

			// update task
			task.setLastExportedVersions(activeVersionIds);
			taskRepository.updateHaltelogTask(task);
		}

		final LocalDate start = task.getHaltelogExportedUntil().plusDays(1);
		for (LocalDate date = start; shouldWriteHistoryForDate(date); date = date.plusDays(1)) {
			updateHaltelog(task, activeVersions, date, now.toInstant());
		}
	}

	private boolean shouldWriteHistoryForDate(LocalDate date) {
		final Instant now = Instant.now(clock);
		final Instant cutoffTime = ZonedDateTime.of(date, properties.getHaltelogExportTimeOfDay(), ZoneId.systemDefault()).toInstant();
		return now.plus(properties.getHaltelogExportDaysInFuture(), ChronoUnit.DAYS).isAfter(cutoffTime);
	}

	private void updateHaltelog(HaltelogTask task, Collection<ImportVersion> activeVersions, LocalDate date, Instant now) {
		// check if database is in a clean state, remove incomplete day if necessary
		if (haltelogRepository.containsDataForCalendarDay(date)) {
			logger.warn("haltelog is in an unclean state, remove journeys for calendar day {}", date);
			haltelogRepository.deleteDataForCalendarDay(date);
		}

		// copy data from the currently active versions to the haltelog
		for (final ImportVersion activeVersion : activeVersions) {
			try {
				updateHaltelog(activeVersion, date, now);
			} catch (RuntimeException e) {
				logger.error("failed to update haltelog for version " + activeVersion.version + " of " + activeVersion.timetable, e);
			}
		}

		// update task
		task.setHaltelogExportedUntil(date);
		taskRepository.updateHaltelogTask(task);
	}

	private void updateHaltelog(ImportVersion version, LocalDate date, Instant now) {
		logger.info("adding data for {} from version {} of {} to haltelog", date, version.version, version.timetable);
		final NetexRepository netexRepository = mongoClientWrapper.createNetexRepository(version.databaseName);

		// Note: we use the journeys (and iterate over their calls) instead of fetching the calls directly from mongodb because exporting
		// calls can be disabled, but journeys are always present.
		final List<JourneyWithCalls> journeys = netexRepository.getJourneysForCalendarDay(date);
		for (final JourneyWithCalls journey : journeys) {
			for (int i = 0; i < journey.calls.size(); ++i) {
				final Call call = journey.calls.get(i);
				final HaltelogEntry entry = HaltelogEntryMapper.createHalteLogEntry(journey, call, i, now);
				haltelogRepository.save(entry);
			}
		}
	}
}
