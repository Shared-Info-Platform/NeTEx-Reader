package ch.bernmobil.netex.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseStatus;

import ch.bernmobil.netex.api.NetexApiProperties;
import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.api.model.Route.StopPlace;
import ch.bernmobil.netex.persistence.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.search.RouteAggregationRepository;

@Service
public class RouteService {

	private final NetexApiProperties properties;
	private final RepositoryFactory repositoryFactory;

	public RouteService(NetexApiProperties properties, RepositoryFactory repositoryFactory) {
		this.properties = properties;
		this.repositoryFactory = repositoryFactory;
	}

	public Map<DirectionType, List<Route>> findRoutesByDirection(String operatorCode, String lineCode, Optional<DirectionType> directionType, Optional<LocalDate> calendarDay,
			Optional<Integer> previewDays, BigDecimal threshold, Optional<String> databaseName) {
		final List<String> directionTypes = directionType.map(DirectionType::name).map(List::of).orElse(getDefaultDirectionTypes());
		final List<String> calendarDays = getCalendarDays(calendarDay.orElse(LocalDate.now()), previewDays.orElse(0));

		final RouteAggregationRepository repository = getRepository(databaseName);
		final List<RouteAggregation> aggregations = repository.findRouteAggregations(operatorCode, lineCode, directionTypes, calendarDays);
		final Map<DirectionType, List<RouteAggregation>> aggregationsByDirection = groupRouteAggregationsByDirectionType(aggregations);

		final Map<DirectionType, List<Route>> result = new TreeMap<>(); // use tree map to sort inbound > outbound
		for (final Entry<DirectionType, List<RouteAggregation>> entry : aggregationsByDirection.entrySet()) {
			final List<Route> routes = groupRouteAggregationsByRoute(entry.getValue()).values().stream().map(this::createRoute).collect(Collectors.toCollection(() -> new ArrayList<>()));
			result.put(entry.getKey(), cutOffAfterThreshold(routes, threshold));
		}
		return result;
	}

	private List<Route> cutOffAfterThreshold(List<Route> routes, BigDecimal threshold) {
		// calculate percentage of journeys compared to total
		final BigDecimal totalNumberOfJourneys = BigDecimal.valueOf(routes.stream().mapToLong(Route::getNumberOfJourneys).sum());
		routes.stream().forEach(route -> route.setPercentagePerDirection(getPercentageOfTotal(route.getNumberOfJourneys(), totalNumberOfJourneys)));

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

	private RouteAggregationRepository getRepository(Optional<String> optionalDatabaseName) {
		final String databaseName = optionalDatabaseName.orElse(properties.getDatabaseName());
		final RouteAggregationRepository repository = repositoryFactory.createRepository(databaseName);
		if (repository != null) {
			return repository;
		} else {
			throw new NotFoundException("no database found with name " + databaseName);
		}
	}

	private Map<DirectionType, List<RouteAggregation>> groupRouteAggregationsByDirectionType(List<RouteAggregation> aggregations) {
		final Map<DirectionType, List<RouteAggregation>> result = new HashMap<>();
		for (final RouteAggregation aggregation : aggregations) {
			final DirectionType directionType = DirectionType.valueOf(aggregation.directionType);
			final List<RouteAggregation> list = result.computeIfAbsent(directionType, key -> new ArrayList<>());
			list.add(aggregation);
		}
		return result;
	}

	private Map<RouteId, List<RouteAggregation>> groupRouteAggregationsByRoute(List<RouteAggregation> aggregations) {
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

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    public class NotFoundException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		public NotFoundException(String message) {
            super(message);
        }
    }
}
