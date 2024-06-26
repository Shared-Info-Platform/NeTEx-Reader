package ch.bernmobil.netex.importer;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.netex.dom.NetexAvailabilityCondition;
import ch.bernmobil.netex.importer.netex.dom.NetexDestinationDisplay;
import ch.bernmobil.netex.importer.netex.dom.NetexLine;
import ch.bernmobil.netex.importer.netex.dom.NetexNotice;
import ch.bernmobil.netex.importer.netex.dom.NetexOperator;
import ch.bernmobil.netex.importer.netex.dom.NetexPassengerStopAssignment;
import ch.bernmobil.netex.importer.netex.dom.NetexQuay;
import ch.bernmobil.netex.importer.netex.dom.NetexResponsibilitySet;
import ch.bernmobil.netex.importer.netex.dom.NetexScheduledStopPoint;
import ch.bernmobil.netex.importer.netex.dom.NetexServiceFacilitySet;
import ch.bernmobil.netex.importer.netex.dom.NetexStopPlace;
import ch.bernmobil.netex.importer.netex.dom.NetexTrainNumber;
import ch.bernmobil.netex.importer.netex.dom.NetexTypeOfNotice;
import ch.bernmobil.netex.importer.netex.dom.NetexTypeOfProductCategory;
import ch.bernmobil.netex.importer.netex.dom.NetexVehicleType;

/**
 * This class is a cache for all common entities that are defined in different NeTEx files and are referenced
 * by journeys (plus some of them also have references between each other).
 */
public class ImportState {

	private static final Logger LOGGER = LoggerFactory.getLogger(Importer.class);

	private final Map<String, NetexOperator> operators = new HashMap<>();
	private final Map<String, NetexResponsibilitySet> responsibilitySets = new HashMap<>();
	private final Map<String, NetexTypeOfNotice> typeOfNotices = new HashMap<>();
	private final Map<String, NetexTypeOfProductCategory> typeOfProductCategories = new HashMap<>();
	private final Map<String, NetexVehicleType> vehicleTypes = new HashMap<>();
	private final Map<String, NetexStopPlace> stopPlaces = new HashMap<>();
	private final Map<String, NetexQuay> quays = new HashMap<>();
	private final Map<String, NetexLine> lines = new HashMap<>();
	private final Map<String, NetexDestinationDisplay> destinationDisplays = new HashMap<>();
	private final Map<String, NetexPassengerStopAssignment> passengerStopAssignments = new HashMap<>();
	private final Map<String, NetexScheduledStopPoint> scheduledStopPoints = new HashMap<>();
	private final Map<String, NetexNotice> notices = new HashMap<>();
	private final Map<String, NetexAvailabilityCondition> availabilityConditions = new HashMap<>();
	private final Map<String, NetexServiceFacilitySet> serviceFacilitySets = new HashMap<>();
	private final Map<String, NetexTrainNumber> trainNumbers = new HashMap<>();

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

	public void addTypeOfNotice(NetexTypeOfNotice typeOfNotice) {
		if (typeOfNotices.put(typeOfNotice.id, typeOfNotice) != null) {
			LOGGER.warn("duplicate entry for {}", typeOfNotice.id);
		}
	}

	public Map<String, NetexTypeOfNotice> getTypeOfNotices() {
		return typeOfNotices;
	}

	public void addTypeOfProductCategory(NetexTypeOfProductCategory typeOfProductCategory) {
		if (typeOfProductCategories.put(typeOfProductCategory.id, typeOfProductCategory) != null) {
			LOGGER.warn("duplicate entry for {}", typeOfProductCategory.id);
		}
	}

	public Map<String, NetexTypeOfProductCategory> getTypeOfProductCategories() {
		return typeOfProductCategories;
	}

	public void addVehicleType(NetexVehicleType vehicleType) {
		if (vehicleTypes.put(vehicleType.id, vehicleType) != null) {
			LOGGER.warn("duplicate entry for {}", vehicleType.id);
		}
	}

	public Map<String, NetexVehicleType> getVehicleTypes() {
		return vehicleTypes;
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

	public void addNotice(NetexNotice notice) {
		if (notices.put(notice.id, notice) != null) {
			LOGGER.warn("duplicate entry for {}", notice.id);
		}
	}

	public Map<String, NetexNotice> getNotices() {
		return notices;
	}

	public void addAvailabilityCondition(NetexAvailabilityCondition availabilityCondition) {
		if (availabilityConditions.put(availabilityCondition.id, availabilityCondition) != null) {
			LOGGER.warn("duplicate entry for {}", availabilityCondition.id);
		}
	}

	public Map<String, NetexAvailabilityCondition> getAvailabilityConditions() {
		return availabilityConditions;
	}

	public void addServiceFacilitySet(NetexServiceFacilitySet serviceFacilitySet) {
		if (serviceFacilitySets.put(serviceFacilitySet.id, serviceFacilitySet) != null) {
			LOGGER.warn("duplicate entry for {}", serviceFacilitySet.id);
		}
	}

	public Map<String, NetexServiceFacilitySet> getServiceFacilitySets() {
		return serviceFacilitySets;
	}

	public void addTrainNumber(NetexTrainNumber trainNumber) {
		if (trainNumbers.put(trainNumber.id, trainNumber) != null) {
			LOGGER.warn("duplicate entry for {}", trainNumber.id);
		}
	}

	public Map<String, NetexTrainNumber> getTrainNumbers() {
		return trainNumbers;
	}
}
