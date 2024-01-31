package ch.bernmobil.netex.importer.journey.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.CallAggregation;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;

/**
 * Aggregates the number of journeys and calls for each day, operator, and line (and stopPlace, but only for calls).
 */
public class JourneyAggregator {

	private static final int MAX_NUMBER_OF_AGGREGATIONS = 250_000;

	private Map<JourneyAggregation.Id, JourneyAggregation> journeyAggregations = new HashMap<>();
	private Map<CallAggregation.Id, CallAggregation> callAggregations = new HashMap<>();

	public void aggregateJourneys(List<Journey> journeys) {
		synchronized(this) {
			journeys.stream().forEach(this::aggregateJourney);
		}
	}

	private void aggregateJourney(Journey journey) {
		final JourneyAggregation.Id journeyAggregationId = createJourneyAggregationId(journey);
		final List<CallAggregation.Id> callAggregationIds = journey.calls.stream().map(call -> createCallAggregationId(call, journey)).toList();

		JourneyAggregation journeyAggregation = journeyAggregations.get(journeyAggregationId);
		if (journeyAggregation == null) {
			journeyAggregation = new JourneyAggregation(journeyAggregationId);
			journeyAggregations.put(journeyAggregationId, journeyAggregation);
		}
		++journeyAggregation.journeys;

		for (final CallAggregation.Id callAggregationId : callAggregationIds) {
			CallAggregation callAggregation = callAggregations.get(callAggregationId);
			if (callAggregation == null) {
				callAggregation = new CallAggregation(callAggregationId);
				callAggregations.put(callAggregationId, callAggregation);
			}
			++callAggregation.calls;
		}
	}

	public Collection<JourneyAggregation> getJourneyAggregations() {
		return journeyAggregations.values();
	}

	public Collection<CallAggregation> getCallAggregations() {
		return callAggregations.values();
	}

	public List<JourneyAggregation> resetJourneyAggregationsIfNecessary() {
		synchronized (this) {
			if (journeyAggregations.size() > MAX_NUMBER_OF_AGGREGATIONS) {
				final List<JourneyAggregation> copy = new ArrayList<>(journeyAggregations.values());
				journeyAggregations.clear();
				return copy;
			} else {
				return null;
			}
		}
	}

	public List<CallAggregation> resetCallAggregationsIfNecessary() {
		synchronized (this) {
			if (callAggregations.size() > MAX_NUMBER_OF_AGGREGATIONS) {
				final List<CallAggregation> copy = new ArrayList<>(callAggregations.values());
				callAggregations.clear();
				return copy;
			} else {
				return null;
			}
		}
	}

	private JourneyAggregation.Id createJourneyAggregationId(Journey journey) {
		final JourneyAggregation.Id result = new JourneyAggregation.Id();
		result.calendarDay = journey.getCalendarDay();
		result.operatorCode = journey.operatorCode;
		result.lineCode = journey.lineCode;
		return result;
	}

	private CallAggregation.Id createCallAggregationId(Call call, Journey journey) {
		final CallAggregation.Id result = new CallAggregation.Id();
		result.calendarDay = call.getCalendarDay();
		result.stopPlaceCode = call.stopPlaceCode;
		result.operatorCode = journey.operatorCode;
		result.lineCode = journey.lineCode;
		return result;
	}
}
