package ch.bernmobil.netex.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ch.bernmobil.netex.api.NetexApiProperties;
import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.api.service.RouteService.NotFoundException;
import ch.bernmobil.netex.persistence.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.dom.RouteAggregation.StopPlace;
import ch.bernmobil.netex.persistence.search.RouteAggregationRepository;

public class RouteServiceTest {

	private RouteService routeService;
	private NetexApiProperties properties = new NetexApiProperties();
	private RepositoryFactory repositoryFactory;
	private RouteAggregationRepository repository;
	private List<RouteAggregation> aggregations = new ArrayList<>();

	@BeforeEach
	public void setup () {
		repository = Mockito.mock(RouteAggregationRepository.class);
		Mockito.when(repository.findRouteAggregations(anyString(), anyString(), anyCollection(), anyCollection())).thenReturn(aggregations);

		repositoryFactory = Mockito.mock(RepositoryFactory.class);
		Mockito.when(repositoryFactory.createRepository(properties.getDatabaseName())).thenReturn(repository);

		routeService = new RouteService(properties, repositoryFactory);
	}

	@Test
	public void testUsesCorrectOperatorInRepository() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.empty(),
				BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(eq("operator"), anyString(), anyCollection(), anyCollection());
	}

	@Test
	public void testUsesCorrectLineInRepository() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.empty(),
				BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), eq("line"), anyCollection(), anyCollection());
	}

	@Test
	public void testUsesCorrectDirectionInRepository() {
		routeService.findRoutesByDirection("operator", "line", Optional.of(DirectionType.inbound), Optional.empty(),
				Optional.empty(), BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), anyString(), eq(List.of("inbound")), anyCollection());
	}

	@Test
	public void testFallsBackToAllDirectionsWhenNoneProvided() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.empty(),
				BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), anyString(), eq(List.of("inbound", "outbound")),
				anyCollection());
	}

	@Test
	public void testUsesCorrectDaysInRepository() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.of(LocalDate.of(2024, 9, 9)),
				Optional.of(2), BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), anyString(), anyCollection(),
				eq(List.of("2024-09-09", "2024-09-10", "2024-09-11")));
	}

	@Test
	public void testFallsBackToTodayWhenNoDayProvided() {
		final LocalDate today = LocalDate.now();
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.of(2),
				BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), anyString(), anyCollection(),
				eq(List.of(today.toString(), today.plusDays(1).toString(), today.plusDays(2).toString())));
	}

	@Test
	public void testFallsBackToNoAdditionalDaysWhenNoneProvided() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.of(LocalDate.of(2024, 9, 9)),
				Optional.empty(), BigDecimal.valueOf(90), Optional.empty());
		verify(repository).findRouteAggregations(anyString(), anyString(), anyCollection(), eq(List.of("2024-09-09")));
	}

	@Test
	public void testUsesDataFromCorrectDatabase() {
		Mockito.when(repositoryFactory.createRepository("database")).thenReturn(repository);
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.empty(),
				BigDecimal.valueOf(90), Optional.of("database"));
		verify(repositoryFactory).createRepository("database");
	}

	@Test
	public void testThrowsWhenDatabaseDoesNotExist() {
		assertThatExceptionOfType(NotFoundException.class)
				.isThrownBy(() -> routeService.findRoutesByDirection("operator", "line", Optional.empty(),
						Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.of("database")));
	}

	@Test
	public void testFallsBackToDefaultDatabaseWhenNoneProvided() {
		routeService.findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(), Optional.empty(),
				null, Optional.empty());
		verify(repositoryFactory).createRepository(properties.getDatabaseName());
	}

	@Test
	public void testReturnsRouteAggregationAsRoute() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(1);

		final Route route = result.get(DirectionType.inbound).get(0);
		assertThat(route.getOperatorCode()).isEqualTo("operator");
		assertThat(route.getLineCode()).isEqualTo("line");
		assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
		assertThat(route.getNumberOfJourneys()).isEqualTo(1);
		assertThat(route.getStopPlaces()).hasSize(2);
		assertThat(route.getStopPlaces().get(0).code()).isEqualTo("1");
		assertThat(route.getStopPlaces().get(0).name()).isEqualTo("1");
		assertThat(route.getStopPlaces().get(1).code()).isEqualTo("2");
		assertThat(route.getStopPlaces().get(1).name()).isEqualTo("2");
	}

	@Test
	public void testCombinesMultipleRouteAggregationsInOneRoute() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-10", 2, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-11", 3, List.of("1", "2")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(1);

		final Route route = result.get(DirectionType.inbound).get(0);
		assertThat(route.getOperatorCode()).isEqualTo("operator");
		assertThat(route.getLineCode()).isEqualTo("line");
		assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
		assertThat(route.getNumberOfJourneys()).isEqualTo(6);
		assertThat(route.getStopPlaces()).hasSize(2);
		assertThat(route.getStopPlaces().get(0).code()).isEqualTo("1");
		assertThat(route.getStopPlaces().get(0).name()).isEqualTo("1");
		assertThat(route.getStopPlaces().get(1).code()).isEqualTo("2");
		assertThat(route.getStopPlaces().get(1).name()).isEqualTo("2");
	}

	@Test
	public void testReturnsSeveralRoutes() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-10", 2, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 2, List.of("1", "2", "3")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-10", 3, List.of("1", "2", "3")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(2);

		{
			final Route route = result.get(DirectionType.inbound).get(0);
			assertThat(route.getOperatorCode()).isEqualTo("operator");
			assertThat(route.getLineCode()).isEqualTo("line");
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
			assertThat(route.getNumberOfJourneys()).isEqualTo(5);
			assertThat(route.getStopPlaces()).hasSize(3);
		}
		{
			final Route route = result.get(DirectionType.inbound).get(1);
			assertThat(route.getOperatorCode()).isEqualTo("operator");
			assertThat(route.getLineCode()).isEqualTo("line");
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
			assertThat(route.getNumberOfJourneys()).isEqualTo(3);
			assertThat(route.getStopPlaces()).hasSize(2);
		}
	}

	@Test
	public void testGroupsRoutesByDirection() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-10", 2, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 2, List.of("1", "2", "3")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-10", 3, List.of("1", "2", "3")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result).containsKey(DirectionType.outbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(1);
		assertThat(result.get(DirectionType.outbound)).hasSize(1);

		{
			final Route route = result.get(DirectionType.inbound).get(0);
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
			assertThat(route.getNumberOfJourneys()).isEqualTo(3);
			assertThat(route.getStopPlaces()).hasSize(2);
		}
		{
			final Route route = result.get(DirectionType.outbound).get(0);
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.outbound);
			assertThat(route.getNumberOfJourneys()).isEqualTo(5);
			assertThat(route.getStopPlaces()).hasSize(3);
		}
	}

	@Test
	public void testOrdersRoutesByFrequency() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 5, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 3, List.of("1", "2", "3")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(3);

		assertThat(result.get(DirectionType.inbound).get(0).getNumberOfJourneys()).isEqualTo(5);
		assertThat(result.get(DirectionType.inbound).get(1).getNumberOfJourneys()).isEqualTo(3);
		assertThat(result.get(DirectionType.inbound).get(2).getNumberOfJourneys()).isEqualTo(1);
	}

	@Test
	public void testCalculatesCorrectPercentageForOneDirection() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 5, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 3, List.of("1", "2", "3")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(1);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(3);

		assertThat(result.get(DirectionType.inbound).get(0).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("55.556"));
		assertThat(result.get(DirectionType.inbound).get(1).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("33.333"));
		assertThat(result.get(DirectionType.inbound).get(2).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("11.111"));
	}

	@Test
	public void testCalculatesCorrectPercentageForAllDirections() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 5, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 3, List.of("1", "2", "3")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 3, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 3, List.of("1", "2")));

		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
				Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result).containsKey(DirectionType.outbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(3);
		assertThat(result.get(DirectionType.outbound)).hasSize(2);

		assertThat(result.get(DirectionType.inbound).get(0).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("55.556"));
		assertThat(result.get(DirectionType.inbound).get(1).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("33.333"));
		assertThat(result.get(DirectionType.inbound).get(2).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("11.111"));
		assertThat(result.get(DirectionType.outbound).get(0).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("50.000"));
		assertThat(result.get(DirectionType.outbound).get(1).getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("50.000"));
	}

	@Test
	public void testCutsOffRoutesAtCorrectThresholdForOneDirection() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 5, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 3, List.of("1", "2", "3")));

		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(100), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(3);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(3);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(80), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(2);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(50), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(1);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(0), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(1);
		}
	}

	@Test
	public void testCutsOffRoutesAtCorrectThresholdForAllDirections() {
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 5, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 3, List.of("1", "2", "3")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 3, List.of("1")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 3, List.of("1", "2")));

		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(100), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(3);
			assertThat(result.get(DirectionType.outbound)).hasSize(2);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(90), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(3);
			assertThat(result.get(DirectionType.outbound)).hasSize(2);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(80), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(2);
			assertThat(result.get(DirectionType.outbound)).hasSize(2);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(55), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(1);
			assertThat(result.get(DirectionType.outbound)).hasSize(2);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(50), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(1);
			assertThat(result.get(DirectionType.outbound)).hasSize(1);
		}
		{
			final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line",
					Optional.empty(), Optional.empty(), Optional.empty(), BigDecimal.valueOf(0), Optional.empty());
			assertThat(result.get(DirectionType.inbound)).hasSize(1);
			assertThat(result.get(DirectionType.outbound)).hasSize(1);
		}
	}

	private RouteAggregation createRouteAggregation(String operatorCode, String lineCode, String directionType, String calendarDay, long journeys, List<String> stopPlaces) {
		final RouteAggregation result = new RouteAggregation();
		result.calendarDay = calendarDay;
		result.operatorCode = operatorCode;
		result.lineCode = lineCode;
		result.directionType = directionType;
		result.stopPlaces = stopPlaces.stream().map(code -> new StopPlace(code, code)).toList();
		result.journeys = journeys;
		return result;
	}
}
