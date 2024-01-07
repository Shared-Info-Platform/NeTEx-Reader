package ch.bernmobil.netex.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.journey.dom.Journey;
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
import ch.bernmobil.netex.importer.netex.dom.NetexServiceJourney;
import ch.bernmobil.netex.importer.xml.Parser;

public class Importer {

	private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

	private MongoDbWriter mongoDbWriter = new MongoDbWriter();
	private ExecutorService executorService = Executors.newFixedThreadPool(10);
	private JourneyAggregator aggregator = new JourneyAggregator();

	public static void main(String[] args) throws XMLStreamException, InterruptedException {
		final File directory = new File("C:\\projects\\bernmobil\\files\\netex\\prod_netex_tt_1.10_che_ski_2024_oev-schweiz__1_1_202312200612");
//		final File file1 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_RESOURCE_1_1_202312200612.xml");
//		final File file2 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SITE_1_1_202312200612.xml");
//		final File file3 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SERVICE_1_1_202312200612.xml");
//		final File file4 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_SERVICECALENDAR_1_1_202312200612.xml");
//		final File file5 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_COMMON_1_1_202312200612.xml");
//		final File file6 = new File(directory, "PROD_NETEX_TT_1.10_CHE_SKI_2024_OEV-SCHWEIZ_TIMETABLE_1_193_202312200612.xml");
//
//		final ImportState state = new ImportState();
//		final Importer importer = new Importer();
//		importer.importCommonEntities(Arrays.asList(file1, file2, file3, file4, file5), state);
//		importer.importServiceJourneys(Arrays.asList(file6), state);

		try {
			new Importer().importDirectory(directory);
		} catch (Throwable t) {
			LOGGER.error("import failed", t);
			System.exit(1);
		}
	}

	public void importDirectory(File directory) throws XMLStreamException, InterruptedException {
		final List<File> filesForCommonEntities = new ArrayList<>();
		final List<File> filesForServiceJourneys = new ArrayList<>();

		final File[] filesArray = directory.listFiles();
		if (filesArray != null) {
			final List<File> xmlFiles = filterXmlFiles(Arrays.asList(filesArray));
			filesForCommonEntities.addAll(filterFilesForCommonEntities(xmlFiles));
			filesForServiceJourneys.addAll(filterFilesForServiceJourneys(xmlFiles));

			// if the files don't contain the necessary tokens to distinguish them, just use all files for both categories
			if (filesForCommonEntities.isEmpty() || filesForServiceJourneys.isEmpty()) {
				filesForCommonEntities.addAll(xmlFiles);
				filesForServiceJourneys.addAll(xmlFiles);
			}
		}

		if (filesForCommonEntities.isEmpty() || filesForServiceJourneys.isEmpty()) {
			throw new IllegalArgumentException("no *.xml files found in directory " + directory);
		}

		final ImportState state = new ImportState();
		importCommonEntities(filesForCommonEntities, state);
		importServiceJourneys(filesForServiceJourneys, state);

		LOGGER.info("reading XML done, wait for output to be written");
		executorService.shutdown();
		executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

		LOGGER.info("write aggregations");
		mongoDbWriter.writeJourneyAggregations(aggregator.getJourneyAggregations());
		mongoDbWriter.writeCallAggregations(aggregator.getCallAggregations());

		LOGGER.info("done");
		mongoDbWriter.close();
	}

	private List<File> filterXmlFiles(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().endsWith(".XML")).toList();
	}

	private List<File> filterFilesForCommonEntities(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().contains("_RESOURCE_") ||
											 file.getName().toUpperCase().contains("_SITE_") ||
											 file.getName().toUpperCase().contains("_SERVICE_") ||
											 file.getName().toUpperCase().contains("_SERVICECALENDAR_") ||
											 file.getName().toUpperCase().contains("_COMMON_")).toList();
	}

	private List<File> filterFilesForServiceJourneys(List<File> files) {
		return files.stream().filter(file -> file.getName().toUpperCase().contains("_TIMETABLE_")).toList();
	}

	private void importCommonEntities(List<File> files, ImportState state) throws XMLStreamException {
		final Parser parser = ParserDefinitions.createPublicationDeliveryParser(ParserDefinitions.createCommonFramesParser());
		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();

		final List<ObjectTree> trees = new ArrayList<>();
		for (final File file : files) {
			final XMLStreamReader2 reader = factory.createXMLStreamReader(file);
			final Object result = parser.parse(reader);
			trees.add(ObjectTree.of(result));
		}

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

		for (final Frame resourceFrame : resourceFrames) {
			ResourceDomBuilder.buildDom(resourceFrame, state);
			LOGGER.info("operators: " + state.getOperators().size());
			LOGGER.info("responsibility sets: " + state.getResponsibilitySets().size());
			LOGGER.info("type of notices: " + state.getTypeOfNotices().size());
			LOGGER.info("type of product categories: " + state.getTypeOfProductCategories().size());
			LOGGER.info("vehicle types: " + state.getVehicleTypes().size());
		}

		for (final Frame siteFrame : siteFrames) {
			SiteDomBuilder.buildDom(siteFrame, state);
			LOGGER.info("stop places: " + state.getStopPlaces().size());
			LOGGER.info("quays: " + state.getQuays().size());
		}

		for (final Frame serviceFrame : serviceFrames) {
			ServiceDomBuilder.buildDom(serviceFrame, state);
			LOGGER.info("lines: " + state.getLines().size());
			LOGGER.info("destination displays: " + state.getDestinationDisplays().size());
			LOGGER.info("scheduled stop points: " + state.getScheduledStopPoints().size());
			LOGGER.info("passenger stop assignments: " + state.getPassengerStopAssignments().size());
			LOGGER.info("notices: " + state.getNotices().size());

			// TODO: there seems to be a bug in the data - PassengerStopAssignments for quays does not reference ScheduledStopPoint for quays but instead the generic (quay-less) entity
//				for (ScheduledStopPoint s : state.getScheduledStopPoints().values()) {
//					if (s.assignments.size() != 1) {
//						LOGGER.warn("ScheduledStopPoint {} is referenced by {} PassengerStopAssignments", s.id, s.assignments.size());
//					}
//				}
		}

		for (final Frame serviceCalendarFrame : serviceCalendarFrames) {
			ServiceCalendarDomBuilder.buildDom(serviceCalendarFrame, state);
			LOGGER.info("availability conditions: " + state.getAvailabilityConditions().size());
		}

		for (final Frame timetableFrame : timetableFrames) {
			TimetableCommonDomBuilder.buildDom(timetableFrame, state);
			LOGGER.info("service facility sets: " + state.getServiceFacilitySets().size());
		}
	}

	private void importServiceJourneys(List<File> files, ImportState state) throws XMLStreamException {
		final Parser parser = ParserDefinitions.createPublicationDeliveryParser(ParserDefinitions.createTimetableFramesParser());
		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();

		for (final File file : files) {
			final XMLStreamReader2 reader = factory.createXMLStreamReader(file);
			final Object result = parser.parse(reader);
			final ObjectTree root = ObjectTree.of(result);
//			final CompositeFrameHeader header = BuilderHelper.getCompositeFrameHeader(root);

			final Frame timetableFrame = BuilderHelper.getFrame(root, BuilderHelper.TIMETABLE_FRAME_NAME);
			if (timetableFrame != null) {
				TimetableJourneyDomBuilder.buildDom(timetableFrame, state, this::processJourney);
			}
		}
	}

	private int count1;
	private int count2;
	private int count3;
	private int count4;
	private int exported;

	private void processJourney(NetexServiceJourney journey) {
//		System.out.println("new journey " + journey.id);
		++count1;
		count2 += journey.calls.size();
		count3 += journey.availabilityCondition.validDays.size();
		count4 += (journey.calls.size() * journey.availabilityCondition.validDays.size());
		if (count1 % 10000 == 0) {
			LOGGER.info("Imported {} / {} / {} / {}", count1, count2, count3, count4);
		}

		executorService.execute(() -> {
			final List<Journey> results = JourneyTransformer.transform(journey);
			aggregator.aggregateJourneys(results);

//			if (!results.isEmpty() && "820".equals(results.get(0).operatorCode)) {
				mongoDbWriter.writeJourneys(results);
				++exported;
				if (exported % 10000 == 0) {
					LOGGER.info("Exported {}", exported);
				}
//			}
		});
//		for (final Journey result : results) {
//			if ("VBZ".equals(result.operatorShortName) && "N9".equals(result.lineName) && result.operatingDay.equals(LocalDate.of(2024, 3, 30))) {
//				System.out.println(result.operatingDay + " - " + result.id);
//			}
//		}
	}

	private void importTest(File file) throws XMLStreamException {
		System.out.println("import " + file);

		final XMLInputFactory2 factory = (XMLInputFactory2)XMLInputFactory2.newInstance();
		final XMLStreamReader2 reader = factory.createXMLStreamReader(file);

		while (reader.hasNext()) {
			final int eventType = reader.next();
			switch (eventType) {
				case XMLStreamConstants.START_DOCUMENT:
					System.out.println("start document");
					break;
				case XMLStreamConstants.END_DOCUMENT:
					System.out.println("end document");
					break;
				case XMLStreamConstants.START_ELEMENT:
					System.out.println("start element " + reader.getDepth() + " " + reader.getName() + " / " + reader.getLocalName());
					for (int i = 0; i < reader.getAttributeCount(); ++i) {
						System.out.println("  attribute: " + reader.getAttributeName(i) + " / " + reader.getAttributeLocalName(i) + " / " + reader.getAttributeValue(i));
					}
					break;
				case XMLStreamConstants.END_ELEMENT:
					System.out.println("end element");
					break;
				case XMLStreamConstants.ATTRIBUTE:
					System.out.println("attribute");
					break;
				case XMLStreamConstants.CHARACTERS:
					final String text = reader.getText();
					System.out.println("characters " + (text.isBlank() ? "" : text));
					break;
				case XMLStreamConstants.COMMENT:
					System.out.println("comment");
					break;
				default:
					System.out.println("something else: " + eventType);
					break;
			}
		}
	}
}
