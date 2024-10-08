package ch.bernmobil.netex.importer.netex.builder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexDestinationDisplay;
import ch.bernmobil.netex.importer.netex.dom.NetexLine;
import ch.bernmobil.netex.importer.netex.dom.NetexNotice;
import ch.bernmobil.netex.importer.netex.dom.NetexPassengerStopAssignment;
import ch.bernmobil.netex.importer.netex.dom.NetexScheduledStopPoint;
import ch.bernmobil.netex.importer.xml.MultilingualStringParser.MultilingualString;

/**
 * This class reads the object tree of a NeTEx service frame and stores the contained entities in the ImportState.
 */
public class ServiceDomBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceDomBuilder.class);

	public static void buildDom(Frame serviceFrame, ImportState state) {
		final ObjectTree lines = serviceFrame.frameTree.optionalChild("lines");
		if (lines != null) {
			lines.children("Line").stream().map(ServiceDomBuilder::buildLine).forEach(state::addLine);
		}

		final ObjectTree destinationDisplays = serviceFrame.frameTree.optionalChild("destinationDisplays");
		if (destinationDisplays != null) {
			destinationDisplays.children("DestinationDisplay").stream().map(ServiceDomBuilder::buildDestinationDisplay).forEach(state::addDestinationDisplay);
		}

		final ObjectTree scheduledStopPoints = serviceFrame.frameTree.optionalChild("scheduledStopPoints");
		if (scheduledStopPoints != null) {
			scheduledStopPoints.children("ScheduledStopPoint").stream().map(ServiceDomBuilder::buildScheduledStopPoint).forEach(state::addScheduledStopPoint);
		}

		final ObjectTree stopAssignments = serviceFrame.frameTree.optionalChild("stopAssignments");
		if (stopAssignments != null) {
			stopAssignments.children("PassengerStopAssignment").stream().map(child -> buildPassengerStopAssignment(child, state)).forEach(state::addPassengerStopAssignment);
		}

		final ObjectTree notices = serviceFrame.frameTree.optionalChild("notices");
		if (notices != null) {
			notices.children("Notice").stream().map(child -> buildNotice(child, state)).forEach(state::addNotice);
		}
	}

	private static NetexLine buildLine(ObjectTree tree) {
		final NetexLine result = new NetexLine();
		result.id = tree.text("id");
		result.name = tree.multilingualString("Name").getText();
		result.shortName = tree.optionalMultilingualString("ShortName").map(MultilingualString::getText).orElse(null);
		result.transportMode = tree.optionalText("TransportMode");
		result.publicCode = tree.optionalText("PublicCode");

		final ObjectTree transportSubmode = tree.optionalChild("TransportSubmode");
		if (transportSubmode != null) {
			final List<String> transportSubmodes = Arrays.asList(
					transportSubmode.optionalText("RailSubmode"),
					transportSubmode.optionalText("MetroSubmode"),
					transportSubmode.optionalText("TramSubmode"),
					transportSubmode.optionalText("BusSubmode"),
					transportSubmode.optionalText("CoachSubmode"),
					transportSubmode.optionalText("FunicularSubmode"),
					transportSubmode.optionalText("WaterSubmode"),
					transportSubmode.optionalText("TelecabinSubmode"),
					transportSubmode.optionalText("TaxiSubmode"))
					.stream()
					.filter(Objects::nonNull)
					.toList();
			if (transportSubmodes.size() >= 1) {
				if (transportSubmodes.size() > 1) {
					LOGGER.warn("more than one submode defined for line {}: {}", result.id, transportSubmodes);
				}
				result.transportSubmode = transportSubmodes.get(0);
			}
		}
		return result;
	}

	private static NetexDestinationDisplay buildDestinationDisplay(ObjectTree tree) {
		final NetexDestinationDisplay result = new NetexDestinationDisplay();
		result.id = tree.text("id");
		result.name = tree.multilingualString("Name").getText();
		result.driverDisplayText = tree.optionalMultilingualString("DriverDisplayText").map(MultilingualString::getText).orElse(null);
		result.privateCode = tree.optionalText("PrivateCode");
		return result;
	}

	private static NetexScheduledStopPoint buildScheduledStopPoint(ObjectTree tree) {
		final NetexScheduledStopPoint result = new NetexScheduledStopPoint();
		result.id = tree.text("id");
		result.name = tree.optionalMultilingualString("Name").map(MultilingualString::getText).orElse(null);
		result.shortName = tree.optionalMultilingualString("ShortName").map(MultilingualString::getText).orElse(null);

		final Map<String, String> keyValueMap = BuilderHelper.buildMapFromKeyList(tree.optionalChild("keyList"));
		result.didok = keyValueMap.get("DIDOK");

		return result;
	}

	private static NetexPassengerStopAssignment buildPassengerStopAssignment(ObjectTree tree, ImportState state) {
		final NetexPassengerStopAssignment result = new NetexPassengerStopAssignment();
		result.id = tree.text("id");

		final ObjectTree scheduledStopPointRef = tree.optionalChild("ScheduledStopPointRef");
		if (scheduledStopPointRef != null) {
			final String scheduledStopPointId = scheduledStopPointRef.text("ref");
			result.scheduledStopPoint = state.getScheduledStopPoints().get(scheduledStopPointId);
			if (result.scheduledStopPoint == null) {
				throw new IllegalArgumentException("unknown ScheduledStopPoint with id " + scheduledStopPointId);
			}
			result.scheduledStopPoint.assignments.add(result);
		}

		final String stopPlaceId = tree.child("StopPlaceRef").text("ref");
		result.stopPlace = state.getStopPlaces().get(stopPlaceId);
		if (result.stopPlace == null) {
			throw new IllegalArgumentException("unknown StopPlace with id " + stopPlaceId);
		}

		final ObjectTree quayRef = tree.optionalChild("QuayRef");
		if (quayRef != null) {
			final String quayId = quayRef.text("ref");
			result.quay = state.getQuays().get(quayId);
			if (result.quay == null) {
				throw new IllegalArgumentException("unknown Quay with id " + quayId);
			}
		}

		return result;
	}

	private static NetexNotice buildNotice(ObjectTree tree, ImportState state) {
		final NetexNotice result = new NetexNotice();
		result.id = tree.text("id");
		result.text = tree.optionalMultilingualString("Text").map(MultilingualString::getText).orElse(null);
		result.shortCode = tree.optionalText("ShortCode");
		result.privateCode = tree.optionalText("PrivateCode");

		final String canBeAdvertised = tree.optionalText("CanBeAdvertised");
		if (canBeAdvertised != null) {
			result.canBeAdvertised = Boolean.parseBoolean(canBeAdvertised);
		}

		final String typeOfNoticeId = tree.child("TypeOfNoticeRef").text("ref");
		result.typeOfNotice = state.getTypeOfNotices().get(typeOfNoticeId);
		if (result.typeOfNotice == null) {
			throw new IllegalArgumentException("unknown TypeOfNotices with id " + typeOfNoticeId);
		}
		return result;
	}
}
