package ch.bernmobil.netex.importer;

import java.io.File;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoException;

import ch.bernmobil.netex.importer.journey.dom.CallAggregation;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.journey.dom.RouteAggregation;
import ch.bernmobil.netex.importer.journey.transformer.JourneyAggregator;
import ch.bernmobil.netex.importer.journey.transformer.JourneyTransformer;
import ch.bernmobil.netex.importer.mongodb.export.MongoDbWriter;
import ch.bernmobil.netex.importer.netex.builder.BuilderHelper;
import ch.bernmobil.netex.importer.netex.builder.Frame;
import ch.bernmobil.netex.importer.netex.builder.ObjectTree;
import ch.bernmobil.netex.importer.netex.builder.ResourceDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.ServiceCalendarDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.ServiceDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.SiteDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.TimetableCommonDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.TimetableJourneyDomBuilder;
import ch.bernmobil.netex.importer.netex.builder.TimetableTrainNumberDomBuilder;
import ch.bernmobil.netex.importer.netex.dom.NetexServiceJourney;
import ch.bernmobil.netex.importer.xml.Parser;

/**
 * This class imports journeys from NeTEx files that are located in a given directory.
 */
public class Importer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

	private final MongoDbWriter mongoDbWriter;
	private final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Constants.NUMBER_OF_THREADS);
	private final JourneyAggregator aggregator = new JourneyAggregator();
	private final Statistics statistics = new Statistics();

	public Importer(MongoDbWriter mongoDbWriter) {
		this.mongoDbWriter = mongoDbWriter;
	}

	public void importFile(File file) throws XMLStreamException, InterruptedException {
		final List<File> list = Collections.singletonList(file);
		importFiles(list, list);
	}

	public void importDirectory(File directory) throws XMLStreamException, InterruptedException {
		final List<File> filesForCommonEntities = new ArrayList<>();
		final List<File> filesForServiceJourneys = new ArrayList<>();

		// look at all files in the given directory and try to classify them into files for common entities and
		// files for service journeys (based on the filename).
		final File[] filesArray = directory.listFiles();
		if (filesArray != null) {
			final List<File> xmlFiles = filterXmlFiles(Arrays.asList(filesArray));
			filesForCommonEntities.addAll(filterFilesForCommonEntities(xmlFiles));
			filesForServiceJourneys.addAll(filterFilesForServiceJourneys(xmlFiles));

			// if the files don't contain the necessary tokens to distinguish them, just use all files for both categories
			if (filesForCommonEntities.isEmpty() || filesForServiceJourneys.isEmpty()) {
				LOGGER.warn("could not determine which files contain common objects and which contain journeys - "
						+ "this import will read all files and use a lot of memory!");
				filesForCommonEntities.addAll(xmlFiles);
				filesForServiceJourneys.addAll(xmlFiles);
			}
		}

		importFiles(filesForCommonEntities, filesForServiceJourneys);
	}

	private void importFiles(List<File> filesForCommonEntities, List<File> filesForServiceJourneys) throws XMLStreamException, InterruptedException {
		// abort if no files were found
		if (filesForCommonEntities.isEmpty() || filesForServiceJourneys.isEmpty()) {
			throw new IllegalArgumentException("found no *.xml files to import");
		}

		// check if database is empty
		if (!mongoDbWriter.isDatabaseEmpty()) {
			throw new IllegalStateException("database is not empty");
		}

		// start import. first import common entities (and cache them in ImportState), then import all
		// journeys (which reference the common entities)
		final ImportState state = new ImportState();
		importCommonEntities(filesForCommonEntities, state);
		importServiceJourneys(filesForServiceJourneys, state);

		LOGGER.info("reading XML done, wait for output to be written");
		// tell the executor to shut down. it will process all queued journeys first, then quit
		executor.shutdown();
		// block the current function until the executor has quit
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		LOGGER.info("write aggregations");
		mongoDbWriter.writeJourneyAggregations(aggregator.getJourneyAggregations());
		mongoDbWriter.writeCallAggregations(aggregator.getCallAggregations());
		mongoDbWriter.writeRouteAggregations(aggregator.getRouteAggregations());

		LOGGER.info("done");
		mongoDbWriter.close();
	}

	/**
	 * Returns all files that end with ".xml"
	 */
	private List<File> filterXmlFiles(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().endsWith(".XML")).toList();
	}

	/**
	 * Returns all files that contain a token that marks them as containing common entities (like stop places)
	 */
	private List<File> filterFilesForCommonEntities(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().contains("_RESOURCE_") ||
											 file.getName().toUpperCase().contains("_SITE_") ||
											 file.getName().toUpperCase().contains("_SERVICE_") ||
											 file.getName().toUpperCase().contains("_SERVICECALENDAR_") ||
											 file.getName().toUpperCase().contains("_COMMON_")).toList();
	}

	/**
	 * Returns all files that contain a token that marks them as containing journeys.
	 */
	private List<File> filterFilesForServiceJourneys(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().contains("_TIMETABLE_")).toList();
	}

	/**
	 * Imports common entities (like stop places) from the given files and caches them in ImportState.
	 */
	private void importCommonEntities(List<File> files, ImportState state) throws XMLStreamException {
		final Parser parser = ParserDefinitions.createPublicationDeliveryParser(ParserDefinitions.createCommonFramesParser());
		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();

		// parse all files and keep the parsed object trees in memory. however, the parser is configured up to only
		// keep objects in memory that are actually needed to import the common entities. objects that are not needed
		// at this point (like journeys) are ignored and don't take any space in memory.
		final List<ObjectTree> trees = new ArrayList<>();
		for (final File file : files) {
			final XMLStreamReader2 reader = factory.createXMLStreamReader(file);
			final Object result = parser.parse(reader);
			trees.add(ObjectTree.of(result));
		}

		// log the PublicationTimestamp of the parsed files
		final List<LocalDate> publicationTimestamps = trees.stream().map(tree -> tree.child("PublicationDelivery"))
			.filter(Objects::nonNull)
			.map(publicationDelivery -> publicationDelivery.text("PublicationTimestamp"))
			.filter(Objects::nonNull)
			.map(ZonedDateTime::parse)
			.map(ZonedDateTime::toLocalDate)
			.distinct()
			.toList();
		LOGGER.info("Import data with PublicationTimestamp {}", publicationTimestamps);

		// search all relevant NeTEx frames in all parsed object trees
		final List<Frame> resourceFrames = trees.stream()
				.map(tree -> BuilderHelper.getFrame(tree, BuilderHelper.RESOURCE_FRAME_NAME))
				.filter(Objects::nonNull).toList();
		final List<Frame> siteFrames = trees.stream()
				.map(tree -> BuilderHelper.getFrame(tree, BuilderHelper.SITE_FRAME_NAME))
				.filter(Objects::nonNull).toList();
		final List<Frame> serviceFrames = trees.stream()
				.map(tree -> BuilderHelper.getFrame(tree, BuilderHelper.SERVICE_FRAME_NAME))
				.filter(Objects::nonNull).toList();
		final List<Frame> serviceCalendarFrames = trees.stream()
				.map(tree -> BuilderHelper.getFrame(tree, BuilderHelper.SERVICE_CALENDAR_FRAME_NAME))
				.filter(Objects::nonNull).toList();
		final List<Frame> timetableFrames = trees.stream()
				.map(tree -> BuilderHelper.getFrame(tree, BuilderHelper.TIMETABLE_FRAME_NAME))
				.filter(Objects::nonNull).toList();

		// read the contents of all frames (separated by type), build the common entities and cache them in
		// ImportState. be sure to read the frames in the correct order such that references between the entities
		// can be resolved (e.g. import operators from the resource-frame first before importing lines that reference
		// the operators).

		for (final Frame resourceFrame : resourceFrames) {
			ResourceDomBuilder.buildDom(resourceFrame, state);
			LOGGER.info("operators: {}", state.getOperators().size());
			LOGGER.info("responsibility sets: {}", state.getResponsibilitySets().size());
			LOGGER.info("type of notices: {}", state.getTypeOfNotices().size());
			LOGGER.info("type of product categories: {}", state.getTypeOfProductCategories().size());
			LOGGER.info("vehicle types: {}", state.getVehicleTypes().size());
		}

		for (final Frame siteFrame : siteFrames) {
			SiteDomBuilder.buildDom(siteFrame, state);
			LOGGER.info("stop places: {}", state.getStopPlaces().size());
			LOGGER.info("quays: {}", state.getQuays().size());
		}

		for (final Frame serviceFrame : serviceFrames) {
			ServiceDomBuilder.buildDom(serviceFrame, state);
			LOGGER.info("lines: {}", state.getLines().size());
			LOGGER.info("destination displays: {}", state.getDestinationDisplays().size());
			LOGGER.info("scheduled stop points: {}", state.getScheduledStopPoints().size());
			LOGGER.info("passenger stop assignments: {}", state.getPassengerStopAssignments().size());
			LOGGER.info("notices: {}", state.getNotices().size());

			// TODO: there seems to be a bug in the data - PassengerStopAssignments for quays does not reference ScheduledStopPoint for quays but instead the generic (quay-less) entity
//				for (ScheduledStopPoint s : state.getScheduledStopPoints().values()) {
//					if (s.assignments.size() != 1) {
//						LOGGER.warn("ScheduledStopPoint {} is referenced by {} PassengerStopAssignments", s.id, s.assignments.size());
//					}
//				}
		}

		for (final Frame serviceCalendarFrame : serviceCalendarFrames) {
			ServiceCalendarDomBuilder.buildDom(serviceCalendarFrame, state);
			LOGGER.info("availability conditions: {}", state.getAvailabilityConditions().size());
		}

		for (final Frame timetableFrame : timetableFrames) {
			TimetableCommonDomBuilder.buildDom(timetableFrame, state);
			LOGGER.info("service facility sets: {}", state.getServiceFacilitySets().size());
		}
	}

	/**
	 * Imports service Journeys from the given files.
	 */
	private void importServiceJourneys(List<File> files, ImportState state) throws XMLStreamException {
		final Consumer<Object> serviceJourneyConsumer = object -> processJourney(TimetableJourneyDomBuilder.buildServiceJourney(ObjectTree.of(object), state));

		final Parser trainNumberParser = ParserDefinitions.createPublicationDeliveryParser(
				ParserDefinitions.createTimetableFramesTrainNumberParser());
		final Parser vehicleJourneyParser = ParserDefinitions.createPublicationDeliveryParser(
				ParserDefinitions.createTimetableFramesVehicleJourneyParser(serviceJourneyConsumer));

		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();

		// iterate over all files
		for (final File file : files) {
			try {
				// first read the train numbers (and only those)
				final Object result = trainNumberParser.parse(factory.createXMLStreamReader(file));
				final ObjectTree root = ObjectTree.of(result);

				// if the file contains a timetable frame, import the vehicle numbers content
				final Frame timetableFrame = BuilderHelper.getFrame(root, BuilderHelper.TIMETABLE_FRAME_NAME);
				if (timetableFrame != null) {
					// read the journeys from the frame and pass them to this::processJourney (callback) for further processing
					TimetableTrainNumberDomBuilder.buildDom(timetableFrame, state);
				}

				// then read the file again to parse the vehicle journeys (and only those) and pass them to the consumer
				vehicleJourneyParser.parse(factory.createXMLStreamReader(file));

				// clear vehicle numbers because they are not needed for the next file
				state.getTrainNumbers().clear();
			} catch (RuntimeException e) {
				LOGGER.error("failed to import journeys from " + file, e);
			}
		}
	}

	/**
	 * Callback function. Is called by TimetableJourneyDomBuilder.buildDom() when a new journey is imported from XML.
	 * This function passes the journey to a set of worker threads which will transform the NeTEx-Journey to an
	 * internal Journey-Model and then write it to MongoDB.
	 */
	private void processJourney(NetexServiceJourney journey) {
		statistics.countImport(journey);

		// wait some time if queue is filling up (filling the queue needs memory ->  throttle import instead)
		if (executor.getQueue().size() > Constants.MAX_ENTRIES_IN_QUEUE) {
			try {
				Thread.sleep(Constants.SLEEP_MS_WHEN_QUEUE_FULL);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("thread interrupted", e);
			}
		}

		// push imported journey into queue. will be transformed and exported to MongoDB by a bunch of parallel worker threads
		executor.execute(() -> {
			try {
				transformAndExport(journey);
			} catch (Throwable t) {
				LOGGER.error("failed to transform and export journey " + journey.id, t);
			}
		});
	}

	private void transformAndExport(NetexServiceJourney journey) {
		final List<Journey> results = JourneyTransformer.transform(journey);
		aggregateValues(results);

		try {
			mongoDbWriter.writeJourneys(results);
		} catch (MongoException e) {
			LOGGER.error("exporting journey to MongoDB failed", e);
			LOGGER.error("stop import");
			System.exit(1);
		}

		statistics.countExport(journey);
	}

	private void aggregateValues(List<Journey> journeys) {
		aggregator.aggregateJourneys(journeys);

		// flush aggregations to mongoDB every now and then to avoid using too much memory
		final List<JourneyAggregation> journeyAggregations = aggregator.resetJourneyAggregationsIfNecessary();
		if (journeyAggregations != null) {
			mongoDbWriter.writeJourneyAggregations(journeyAggregations);
		}
		final List<CallAggregation> callAggregations = aggregator.resetCallAggregationsIfNecessary();
		if (callAggregations != null) {
			mongoDbWriter.writeCallAggregations(callAggregations);
		}
		final List<RouteAggregation> routeAggregations = aggregator.resetRouteAggregationsIfNecessary();
		if (routeAggregations != null) {
			mongoDbWriter.writeRouteAggregations(routeAggregations);
		}
	}

	private static class Statistics {
		private AtomicLong importedJourneys = new AtomicLong();
		private AtomicLong importedCalls = new AtomicLong();
		private AtomicLong importedJourneysTimesDays = new AtomicLong();
		private AtomicLong importedCallsTimesDays = new AtomicLong();
		private AtomicLong exportedJourneys = new AtomicLong();

		public void countImport(NetexServiceJourney journey) {
			importedCalls.addAndGet(journey.calls.size());
			importedJourneysTimesDays.addAndGet(journey.availabilityCondition.validDays.size());
			importedCallsTimesDays.addAndGet(journey.calls.size() * journey.availabilityCondition.validDays.size());
			if (importedJourneys.addAndGet(1) % Constants.LOG_STATISTICS_EVERY_N_JOURNEYS == 0) {
				logImports();
			}
		}

		public void countExport(NetexServiceJourney journey) {
			if (exportedJourneys.addAndGet(1) % Constants.LOG_STATISTICS_EVERY_N_JOURNEYS == 0) {
				logExports();
			}
		}

		public void logImports() {
			LOGGER.info("Imported {} journeys and {} calls ({} and {} respectively when multiplied by valid days)",
					importedJourneys, importedCalls, importedJourneysTimesDays, importedCallsTimesDays);
		}

		public void logExports() {
			LOGGER.info("Exported {} journeys", exportedJourneys);
		}
	}
}
