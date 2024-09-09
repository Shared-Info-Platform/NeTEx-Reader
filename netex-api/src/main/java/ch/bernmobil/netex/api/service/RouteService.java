package ch.bernmobil.netex.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.api.NetexApiProperties;
import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.api.model.Route.StopPlace;
import ch.bernmobil.netex.persistence.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.search.RouteAggregationRepository;

@Service
public class RouteService {

	private final MongoClient client;
	private final RouteAggregationRepository defaultRepository;

	public RouteService(MongoClient client, NetexApiProperties properties) {
		this.client = client;
		this.defaultRepository = new RouteAggregationRepository(client, properties.getDatabaseName());
	}

	public List<Route> findRoutes(String operatorCode, String lineCode, Optional<String> directionType, Optional<LocalDate> calendarDay,
			Optional<Integer> previewDays, BigDecimal threshold, Optional<String> databaseName) {
		final List<String> directionTypes = directionType.map(List::of).orElse(getDefaultDirectionTypes());
		final List<String> calendarDays = getCalendarDays(calendarDay.orElse(LocalDate.now()), previewDays.orElse(0));

		final RouteAggregationRepository repository = databaseName.map(name -> new RouteAggregationRepository(client, name)).orElse(defaultRepository);
		final List<RouteAggregation> aggregations = repository.findRouteAggregations(operatorCode, lineCode, directionTypes, calendarDays);

		final List<Route> routes = groupRouteAggregations(aggregations).values().stream().map(this::createRoute).collect(Collectors.toCollection(() -> new ArrayList<>()));

		// calculate percentage of journeys compared to total
		final BigDecimal totalNumberOfJourneys = BigDecimal.valueOf(routes.stream().mapToLong(Route::getNumberOfJourneys).sum());
		routes.stream().forEach(route -> route.setPercentage(getPercentageOfTotal(route.getNumberOfJourneys(), totalNumberOfJourneys)));

		// sort by descending number of journeys and return all routes up to the threshold
		routes.sort(Comparator.comparing(Route::getNumberOfJourneys, Comparator.reverseOrder()));

		long includedNumberOfJourneys = 0;
		final List<Route> result = new ArrayList<>();
		for (final Route route : routes) {
			result.add(route);
			includedNumberOfJourneys += route.getNumberOfJourneys();

			if (getPercentageOfTotal(includedNumberOfJourneys, totalNumberOfJourneys).compareTo(threshold) >= 0) {
				break;
			}
		}

		return result;
	}

	private List<String> getDefaultDirectionTypes() {
		return List.of(DirectionType.inbound.toString(), DirectionType.outbound.toString());
	}

	private List<String> getCalendarDays(LocalDate start, int previewDays) {
		final List<LocalDate> calendarDays = new ArrayList<>(previewDays + 1);

		LocalDate day = start;
		calendarDays.add(day);

		for (int i = 0; i < previewDays; ++i) {
			day = day.plusDays(1);
			calendarDays.add(day);
		}

		return calendarDays.stream().map(LocalDate::toString).toList();
	}

	private Map<RouteId, List<RouteAggregation>> groupRouteAggregations(List<RouteAggregation> aggregations) {
		final Map<RouteId, List<RouteAggregation>> result = new HashMap<>();
		for (final RouteAggregation aggregation : aggregations) {
			final RouteId id = RouteId.of(aggregation);
			final List<RouteAggregation> list = result.computeIfAbsent(id, key -> new ArrayList<>());
			list.add(aggregation);
		}
		return result;
	}

	private Route createRoute(List<RouteAggregation> aggregations) {
		final RouteAggregation first = aggregations.get(0);

		final Route result = new Route();
		result.setOperatorCode(first.operatorCode);
		result.setLineCode(first.lineCode);
		result.setDirectionType(DirectionType.valueOf(first.directionType));
		result.setStopPlaces(first.stopPlaces.stream().map(this::createStopPlace).toList());
		result.setNumberOfJourneys(aggregations.stream().mapToLong(ra -> ra.journeys).sum());
		return result;
	}

	private StopPlace createStopPlace(ch.bernmobil.netex.persistence.dom.RouteAggregation.StopPlace stopPlace) {
		return new StopPlace(stopPlace.code(), stopPlace.name());
	}

	private BigDecimal getPercentageOfTotal(long value, BigDecimal total) {
		return BigDecimal.valueOf(value).divide(total, 5, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
	}

	private record RouteId(String operatorCode, String lineCode, String directionType, List<String> stopPlaceCodes) {
		public static RouteId of(RouteAggregation aggregation) {
			final List<String> stopPlaceCodes = aggregation.stopPlaces.stream().map(ch.bernmobil.netex.persistence.dom.RouteAggregation.StopPlace::code).toList();
			return new RouteId(aggregation.operatorCode, aggregation.lineCode, aggregation.directionType, stopPlaceCodes);
		}
	}
}
