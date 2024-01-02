package ch.bernmobil.netex.importer.netex.builder;

import java.util.Map;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexDestinationDisplay;
import ch.bernmobil.netex.importer.netex.dom.NetexLine;
import ch.bernmobil.netex.importer.netex.dom.NetexPassengerStopAssignment;
import ch.bernmobil.netex.importer.netex.dom.NetexScheduledStopPoint;
import ch.bernmobil.netex.importer.xml.MultilingualStringParser.MultilingualString;

public class ServiceDomBuilder {

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
	}

	private static NetexLine buildLine(ObjectTree tree) {
		final NetexLine result = new NetexLine();
		result.id = tree.text("id");
		result.name = tree.multilingualString("Name").getText();
		result.shortName = tree.optionalMultilingualString("ShortName").map(MultilingualString::getText).orElse(null);
		result.transportMode = tree.optionalText("TransportMode");
		result.publicCode = tree.optionalText("PublicCode");
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
}
