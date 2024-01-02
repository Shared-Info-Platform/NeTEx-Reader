package ch.bernmobil.netex.importer.netex.builder;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.dom.NetexCall;
import ch.bernmobil.netex.importer.netex.dom.NetexCall.Arrival;
import ch.bernmobil.netex.importer.netex.dom.NetexCall.Departure;
import ch.bernmobil.netex.importer.netex.dom.NetexServiceJourney;

public class TimetableJourneyDomBuilder {

	public static void buildDom(Frame timetableFrame, ImportState state, Consumer<NetexServiceJourney> consumer) {
		final ObjectTree vehicleJourneys = timetableFrame.frameTree.optionalChild("vehicleJourneys");
		if (vehicleJourneys != null) {
			vehicleJourneys.children("ServiceJourney").stream().map(child -> buildServiceJourney(child, state)).forEach(consumer::accept);
		}
	}

	private static NetexServiceJourney buildServiceJourney(ObjectTree tree, ImportState state) {
		final NetexServiceJourney result = new NetexServiceJourney();
		result.id = tree.text("id");
		result.privateCode = tree.optionalText("PrivateCode");
		result.transportMode = tree.optionalText("TransportMode");
		result.serviceAlteration = tree.optionalText("ServiceAlteration");
		result.directionType = tree.text("DirectionType");

		final Map<String, String> keyValueMap = BuilderHelper.buildMapFromKeyList(tree.optionalChild("keyList"));
		result.sjyid = keyValueMap.get("SJYID");

		final ObjectTree validityConditions = tree.optionalChild("validityConditions");
		if (validityConditions == null) {
			throw new IllegalArgumentException("ServiceJourney without validityConditions - don't know how to handle this");
		} else {
			final List<ObjectTree> availabilityConditionRefs = validityConditions.children("AvailabilityConditionRef");
			if (availabilityConditionRefs.isEmpty()) {
				throw new IllegalArgumentException("ServiceJourney with empty validityConditions - don't know how to handle this");
			} else if (availabilityConditionRefs.size() > 1) {
				throw new IllegalArgumentException("ServiceJourney with multiple validityConditions - don't know how to handle this");
			} else {
				final String availabilityConditionId = availabilityConditionRefs.get(0).text("ref");
				result.availabilityCondition = state.getAvailabilityConditions().get(availabilityConditionId);
				if (result.availabilityCondition == null) {
					throw new IllegalArgumentException("unknown AvailabilityCondition with id " + availabilityConditionId);
				}
			}
		}

		final String responsibilitySetId = tree.text("responsibilitySetRef");
		result.responsibilitySet = state.getResponsibilitySets().get(responsibilitySetId);
		if (result.responsibilitySet == null) {
			throw new IllegalArgumentException("unknown ResponsibilitySet with id " + responsibilitySetId);
		}

		final ObjectTree lineRef = tree.optionalChild("LineRef");
		if (lineRef != null) {
			final String lineId = lineRef.text("ref");
			result.line = state.getLines().get(lineId);
			if (result.line == null) {
				throw new IllegalArgumentException("unknown Line with id " + lineId);
			}
		}

		final ObjectTree calls = tree.optionalChild("calls");
		if (calls == null) {
			throw new IllegalArgumentException("ServiceJourney " + result.id + " has no calls");
		} else {
			final List<ObjectTree> callList = calls.children("Call");
			if (callList.isEmpty()) {
				throw new IllegalArgumentException("ServiceJourney " + result.id + " has empty calls");
			} else {
				result.calls = callList.stream().map(call -> buildCall(call, state))
												.sorted(Comparator.comparing(TimetableJourneyDomBuilder::getCallOrder))
												.toList();
			}
		}

		return result;
	}

	private static NetexCall buildCall(ObjectTree tree, ImportState state) {
		final NetexCall result = new NetexCall();
		result.id = tree.text("id");
		result.order = Integer.parseInt(tree.text("order"));
		result.requestStop = Boolean.parseBoolean(tree.text("RequestStop"));
		result.stopUse = Boolean.parseBoolean(tree.text("StopUse"));

		final ObjectTree scheduledStopPointRef = tree.optionalChild("ScheduledStopPointRef");
		if (scheduledStopPointRef != null) {
			final String scheduledStopPointId = scheduledStopPointRef.text("ref");
			result.scheduledStopPoint = state.getScheduledStopPoints().get(scheduledStopPointId);
			if (result.scheduledStopPoint == null) {
				throw new IllegalArgumentException("unknown ScheduledStopPoint with id " + scheduledStopPointId);
			}
		}

		final ObjectTree destinationDisplayRef = tree.optionalChild("DestinationDisplayRef");
		if (destinationDisplayRef != null) {
			final String destinationDisplayId = destinationDisplayRef.text("ref");
			result.destinationDisplay = state.getDestinationDisplays().get(destinationDisplayId);
			if (result.destinationDisplay == null) {
				throw new IllegalArgumentException("unknown DestinationDisplay with id " + destinationDisplayId);
			}
		}

		final ObjectTree arrival = tree.optionalChild("Arrival");
		if (arrival != null) {
			result.arrival = new Arrival();
			result.arrival.time = LocalTime.parse(arrival.text("Time"));
			result.arrival.dayOffset = Optional.ofNullable(arrival.optionalText("DayOffset")).map(Integer::parseInt).orElse(0);
			result.arrival.forAlighting = Boolean.parseBoolean(arrival.text("ForAlighting"));
			result.arrival.isFlexible = Boolean.parseBoolean(arrival.text("IsFlexible"));
		}

		final ObjectTree departure = tree.optionalChild("Departure");
		if (departure != null) {
			result.departure = new Departure();
			result.departure.time = LocalTime.parse(departure.text("Time"));
			result.departure.dayOffset = Optional.ofNullable(departure.optionalText("DayOffset")).map(Integer::parseInt).orElse(0);
			result.departure.forBoarding = Boolean.parseBoolean(departure.text("ForBoarding"));
			result.departure.isFlexible = Boolean.parseBoolean(departure.text("IsFlexible"));
		}

		if (arrival == null && departure == null) {
			throw new IllegalArgumentException("Call " + result.id  + " has neither arrival nor departure");
		}

		return result;
	}

	private static int getCallOrder(NetexCall call) {
		return call.order;
	}
}
