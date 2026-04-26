package ch.bernmobil.netex.haltelog.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.bernmobil.netex.haltelog.model.Dataflow;
import ch.bernmobil.netex.haltelog.model.Dataview;
import ch.bernmobil.netex.haltelog.model.FahrtStatus;
import ch.bernmobil.netex.haltelog.model.HaltelogEntry;
import ch.bernmobil.netex.haltelog.model.MetaData;
import ch.bernmobil.netex.haltelog.model.PositionInTrip;
import ch.bernmobil.netex.haltelog.model.Service;
import ch.bernmobil.netex.haltelog.model.ServiceAttribut;
import ch.bernmobil.netex.persistence.model.Call;
import ch.bernmobil.netex.persistence.model.JourneyWithCalls;

/**
 * Note: Keep this in sync with NetexDataTransformer in departures-datenpool.
 */
public class HaltelogEntryMapper {

	public static HaltelogEntry createHalteLogEntry(JourneyWithCalls journey, Call call, int callIndex, Instant importTime) {
		final HaltelogEntry result = new HaltelogEntry();

		result.setMetaData(createMetaData(importTime));
		result.setPositionInTrip(getPositionInTrip(journey, callIndex));
		result.setHaltIdx(callIndex);
		result.setFahrtBezeichner(journey.sjyid != null ? journey.sjyid : journey.id);
		result.setBetriebstag(LocalDate.parse(journey.operatingDay));
		result.setKalendertag(LocalDate.parse(journey.calendarDay));
		result.setHaltId(call.stopPointCode != null ? call.stopPointCode : call.stopPlaceCode);
		result.setBetreiberId(journey.operatorCode);
		result.setLinienId(journey.lineCode);
		result.setRichtungsId(journey.directionType);
		result.setAbfahrtszeitPlan(call.departure != null ? call.departure.time : null);
		result.setAnkunftszeitPlan(call.arrival != null ? call.arrival.time : null);
		if (!Objects.equals(call.stopPointName, call.stopPlaceName)) { // netex uses stopPlaceName as default for stopPointName; ignore it in this case
			result.setHaltePositionsText(call.stopPointName);
		}
		result.setLinienText(journey.lineName);
		result.setRichtungsText(call.destinationDisplayName);
		result.setVerkehrsmittelText(journey.productCategoryCode);
		result.setFahrtStatus(FahrtStatus.PLAN);
		result.setHaltestellenName(call.stopPlaceName);
		result.setEinsteigeVerbot(call.departure != null ? !call.departure.forBoarding : false);
		result.setAussteigeVerbot(call.arrival != null ? !call.arrival.forAlighting : false);
		result.setDurchfahrt("passthrough".equalsIgnoreCase(call.stopUse) || "noboarding".equalsIgnoreCase(call.stopUse));
		result.setZielHst(journey.calls.get(journey.calls.size() - 1).stopPlaceName);
		result.setServiceAttribute(toServiceAttribute(journey.serviceFacilities));
		result.setProduktId(transformProduktId(journey.transportMode, journey.productCategoryCode));

		return result;
	}

	private static MetaData createMetaData(Instant importTime) {
		final MetaData result = new MetaData();
		result.setLogTime(importTime);
		result.setService(Service.NETEX);
		result.setDataflow(Dataflow.INCOMING);
		result.setDataview(Dataview.EXTERNAL);
		// result.setCorrelationId(); only needed when there is an internal and an external dataview
		return result;
	}

	private static PositionInTrip getPositionInTrip(JourneyWithCalls journey, int callIndex) {
		if (callIndex == 0) {
			return PositionInTrip.FIRST;
		} else if (callIndex == journey.calls.size() - 1) {
			return PositionInTrip.LAST;
		} else {
			return PositionInTrip.MIDDLE;
		}
	}

	private static String transformProduktId(String transportMode, String productCategoryCode) {
		if (transportMode == null) {
			return null;
		}
		switch (transportMode) {
			case "rail":
				return "Zug";
			case "tram":
				return "Tram";
			case "metro":
				return "Metro";
			case "coach":
			case "bus":
				return "Bus";
			case "funicular":
				if ("CC".equalsIgnoreCase(productCategoryCode)) {
					return "Zahnradbahn";
				} else {
					return "Standseilbahn";
				}
			case "cableway":
				if ("SL".equalsIgnoreCase(productCategoryCode)) {
					return "Sesselbahn";
				} else if ("ASC".equalsIgnoreCase(productCategoryCode)) {
					return "Aufzug";
				} else {
					return "Kabinenbahn";
				}
			case "water":
				return "Schiff";
			case "taxi":
			default:
				return null;
		}
	}

	static List<ServiceAttribut> toServiceAttribute(List<String> merkmale) {
		List<ServiceAttribut> serviceAttribute = new ArrayList<>();
		for (String merkmal : merkmale) {
			serviceAttribute.add(new ServiceAttribut(merkmal, "1"));
		}
		return serviceAttribute;
	}
}
