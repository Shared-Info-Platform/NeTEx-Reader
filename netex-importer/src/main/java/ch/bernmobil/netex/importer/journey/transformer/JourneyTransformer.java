package ch.bernmobil.netex.importer.journey.transformer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import ch.bernmobil.netex.importer.Constants;
import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.Call.Arrival;
import ch.bernmobil.netex.importer.journey.dom.Call.Departure;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.netex.dom.NetexCall;
import ch.bernmobil.netex.importer.netex.dom.NetexServiceJourney;

public class JourneyTransformer {

	public static List<Journey> transform(NetexServiceJourney journey) {
		return journey.availabilityCondition.validDays.stream().map(date -> transform(journey, date)).toList();
	}

	private static Journey transform(NetexServiceJourney journey, LocalDate date) {
		final Journey result = new Journey();
		result.id = (journey.sjyid != null ? journey.sjyid : journey.id);
		result.operatingDay = date;
		result.transportMode = (journey.transportMode != null ? journey.transportMode : (journey.line != null ? journey.line.transportMode : null));
		result.serviceAlteration = journey.serviceAlteration;
		result.directionType = journey.directionType;

		if (journey.responsibilitySet != null && journey.responsibilitySet.operator != null) {
			result.operatorCode = journey.responsibilitySet.operator.privateCode;
			result.operatorName = journey.responsibilitySet.operator.name;
			result.operatorShortName = journey.responsibilitySet.operator.shortName;
		}

		if (journey.line != null) {
			result.lineCode = journey.line.publicCode;
			result.lineName = journey.line.name;
			result.lineShortName = journey.line.shortName;
		}

		final ZonedDateTime noon = ZonedDateTime.of(date, LocalTime.of(12, 0), Constants.ZONE_ID);
		final ZonedDateTime noonMinus12Hours = noon.minusHours(12);
		result.calls = journey.calls.stream().map(call -> transform(call, noonMinus12Hours)).toList();

		return result;
	}

	private static Call transform(NetexCall call, ZonedDateTime noonMinus12Hours) {
		final Call result = new Call();
		result.id = call.id;
		result.order = call.order;
		result.requestStop = call.requestStop;
		result.stopUse = call.stopUse;

		if (call.scheduledStopPoint != null) {
			result.stopPlaceCode = call.scheduledStopPoint.didok;
			result.stopPlaceName = call.scheduledStopPoint.name;
			result.stopPointName = call.scheduledStopPoint.shortName;
		}
		if (call.destinationDisplay != null) {
			result.destinationDisplayName = call.destinationDisplay.name;
		}

		if (call.arrival != null) {
			result.arrival = new Arrival();
			result.arrival.time = createTime(call.arrival.time, noonMinus12Hours, call.arrival.dayOffset);
			result.arrival.forAlighting = call.arrival.forAlighting;
			result.arrival.isFlexible = call.arrival.isFlexible;
		}
		if (call.departure != null) {
			result.departure = new Departure();
			result.departure.time = createTime(call.departure.time, noonMinus12Hours, call.departure.dayOffset);
			result.departure.forBoarding = call.departure.forBoarding;
			result.departure.isFlexible = call.departure.isFlexible;
		}

		return result;
	}

	/**
	 * Use the "GTFS-way" of handling transition between summer- and winter-time. The given time of a call is
	 * added to "noon of the operating day minus 12 hours" (which is usually midnight, but not when the time offset
	 * changes). A day offset of 1 is simply considered as 24 additional hours (like in GTFS where there can be times
	 * like "26:00:00").
	 */
	private static ZonedDateTime createTime(LocalTime time, ZonedDateTime noonMinus12Hours, int dayOffset) {
		final int hours = time.getHour() + (dayOffset * 24);
		final int minutes = time.getMinute();
		final int seconds = time.getSecond();
		return noonMinus12Hours.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
	}
}
