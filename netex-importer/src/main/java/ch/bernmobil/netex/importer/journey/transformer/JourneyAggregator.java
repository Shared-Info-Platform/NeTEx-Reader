package ch.bernmobil.netex.importer.journey.transformer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ch.bernmobil.netex.importer.Constants;
import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.CallAggregation;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.journey.dom.RouteAggregation;

/**
 * Aggregates the number of journeys and calls for each day, operator, and line (and stopPlace, but only for calls).
 */
public class JourneyAggregator {

	private Map<JourneyAggregation.Id, JourneyAggregation> journeyAggregations = new HashMap<>();
	private Map<CallAggregation.Id, CallAggregation> callAggregations = new HashMap<>();
	private Map<RouteAggregation.Id, RouteAggregation> routeAggregations = new HashMap<>();

	public void aggregateJourneys(List<Journey> journeys) {
		synchronized(this) {
			journeys.stream().forEach(this::aggregateJourney);
		}
	}

	private void aggregateJourney(Journey journey) {
		final JourneyAggregation.Id journeyAggregationId = createJourneyAggregationId(journey);
		final List<CallAggregation.Id> callAggregationIds = journey.calls.stream().map(call -> createCallAggregationId(call, journey)).toList();
		final RouteAggregation.Id routeAggregationId = createRouteAggregationId(journey);

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

		RouteAggregation routeAggregation = routeAggregations.get(routeAggregationId);
		if (routeAggregation == null) {
			routeAggregation = new RouteAggregation(routeAggregationId);
			routeAggregations.put(routeAggregationId, routeAggregation);
		}
		++routeAggregation.journeys;
	}

	public Collection<JourneyAggregation> getJourneyAggregations() {
		return journeyAggregations.values();
	}

	public Collection<CallAggregation> getCallAggregations() {
		return callAggregations.values();
	}

	public Collection<RouteAggregation> getRouteAggregations() {
		return routeAggregations.values();
	}

	public List<JourneyAggregation> resetJourneyAggregationsIfNecessary() {
		synchronized (this) {
			if (journeyAggregations.size() > Constants.MAX_NUMBER_OF_AGGREGATIONS_IN_MEMORY) {
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
			if (callAggregations.size() > Constants.MAX_NUMBER_OF_AGGREGATIONS_IN_MEMORY) {
				final List<CallAggregation> copy = new ArrayList<>(callAggregations.values());
				callAggregations.clear();
				return copy;
			} else {
				return null;
			}
		}
	}

	public List<RouteAggregation> resetRouteAggregationsIfNecessary() {
		synchronized (this) {
			if (routeAggregations.size() > Constants.MAX_NUMBER_OF_AGGREGATIONS_IN_MEMORY) {
				final List<RouteAggregation> copy = new ArrayList<>(routeAggregations.values());
				routeAggregations.clear();
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

	private RouteAggregation.Id createRouteAggregationId(Journey journey) {
		final RouteAggregation.Id result = new RouteAggregation.Id();
		result.calendarDay = journey.getCalendarDay();
		result.operatorCode = journey.operatorCode;
		result.lineCode = journey.lineCode;
		result.directionType = journey.directionType;
		result.stopPlaceCodes = journey.calls.stream().map(call -> call.stopPlaceCode).filter(Objects::nonNull).toList();
		return result;
	}
}
