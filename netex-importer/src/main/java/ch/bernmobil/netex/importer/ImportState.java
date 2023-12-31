package ch.bernmobil.netex.importer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.netex.dom.NetexAvailabilityCondition;
import ch.bernmobil.netex.importer.netex.dom.NetexDestinationDisplay;
import ch.bernmobil.netex.importer.netex.dom.NetexLine;
import ch.bernmobil.netex.importer.netex.dom.NetexOperator;
import ch.bernmobil.netex.importer.netex.dom.NetexPassengerStopAssignment;
import ch.bernmobil.netex.importer.netex.dom.NetexQuay;
import ch.bernmobil.netex.importer.netex.dom.NetexResponsibilitySet;
import ch.bernmobil.netex.importer.netex.dom.NetexScheduledStopPoint;
import ch.bernmobil.netex.importer.netex.dom.NetexStopPlace;

public class ImportState {

	private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

	private final Map<String, NetexOperator> operators = new HashMap<>();
	private final Map<String, NetexResponsibilitySet> responsibilitySets = new HashMap<>();
	private final Map<String, NetexStopPlace> stopPlaces = new HashMap<>();
	private final Map<String, NetexQuay> quays = new HashMap<>();
	private final Map<String, NetexLine> lines = new HashMap<>();
	private final Map<String, NetexDestinationDisplay> destinationDisplays = new HashMap<>();
	private final Map<String, NetexPassengerStopAssignment> passengerStopAssignments = new HashMap<>();
	private final Map<String, NetexScheduledStopPoint> scheduledStopPoints = new HashMap<>();
	private final Map<String, NetexAvailabilityCondition> availabilityConditions = new HashMap<>();

	public void addOperator(NetexOperator operator) {
		if (operators.put(operator.id, operator) != null) {
			LOGGER.warn("duplicate entry for {}", operator.id);
		}
	}

	public Map<String, NetexOperator> getOperators() {
		return operators;
	}

	public void addResponsibilitySet(NetexResponsibilitySet responsibilitySet) {
		if (responsibilitySets.put(responsibilitySet.id, responsibilitySet) != null) {
			LOGGER.warn("duplicate entry for {}", responsibilitySet.id);
		}
	}

	public Map<String, NetexResponsibilitySet> getResponsibilitySets() {
		return responsibilitySets;
	}

	public void addStopPlace(NetexStopPlace stopPlace) {
		if (stopPlaces.put(stopPlace.id, stopPlace) != null) {
			LOGGER.warn("duplicate entry for {}", stopPlace.id);
		}
		for (final NetexQuay quay : stopPlace.quays) {
			if (quays.put(quay.id, quay) != null) {
				LOGGER.warn("duplicate entry for {}", quay.id);
			}
		}
	}

	public Map<String, NetexStopPlace> getStopPlaces() {
		return stopPlaces;
	}

	public Map<String, NetexQuay> getQuays() {
		return quays;
	}

	public void addLine(NetexLine line) {
		if (lines.put(line.id, line) != null) {
			LOGGER.warn("duplicate entry for {}", line.id);
		}
	}

	public Map<String, NetexLine> getLines() {
		return lines;
	}

	public void addDestinationDisplay(NetexDestinationDisplay destinationDisplay) {
		if (destinationDisplays.put(destinationDisplay.id, destinationDisplay) != null) {
			LOGGER.warn("duplicate entry for {}", destinationDisplay.id);
		}
	}

	public Map<String, NetexDestinationDisplay> getDestinationDisplays() {
		return destinationDisplays;
	}

	public void addPassengerStopAssignment(NetexPassengerStopAssignment passengerStopAssignment) {
		if (passengerStopAssignments.put(passengerStopAssignment.id, passengerStopAssignment) != null) {
			LOGGER.warn("duplicate entry for {}", passengerStopAssignment.id);
		}
	}

	public Map<String, NetexPassengerStopAssignment> getPassengerStopAssignments() {
		return passengerStopAssignments;
	}

	public void addScheduledStopPoint(NetexScheduledStopPoint scheduledStopPoint) {
		if (scheduledStopPoints.put(scheduledStopPoint.id, scheduledStopPoint) != null) {
			LOGGER.warn("duplicate entry for {}", scheduledStopPoint.id);
		}
	}

	public Map<String, NetexScheduledStopPoint> getScheduledStopPoints() {
		return scheduledStopPoints;
	}

	public void addAvailabilityCondition(NetexAvailabilityCondition availabilityCondition) {
		if (availabilityConditions.put(availabilityCondition.id, availabilityCondition) != null) {
			LOGGER.warn("duplicate entry for {}", availabilityCondition.id);
		}
	}

	public Map<String, NetexAvailabilityCondition> getAvailabilityConditions() {
		return availabilityConditions;
	}
}
