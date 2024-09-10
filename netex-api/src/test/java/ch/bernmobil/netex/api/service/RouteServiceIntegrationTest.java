package ch.bernmobil.netex.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.api.NetexApiProperties;
import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.persistence.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.dom.RouteAggregation.StopPlace;
import ch.bernmobil.netex.persistence.export.MongoDbWriter;

@SpringBootTest
@ActiveProfiles("test")
public class RouteServiceIntegrationTest {

	@Autowired
	private NetexApiProperties properties;

	@Autowired
	private MongoClient mongoClient;

	@AfterEach
	public void cleanup() {
		mongoClient.getDatabase(properties.getDatabaseName()).getCollection("RouteAggregations").deleteMany(Filters.empty());
	}

	@Test
	public void testWriteAndReadRoutes() {
		final List<RouteAggregation> aggregations = new ArrayList<>();
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-10", 2, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-11", 3, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "line", "inbound", "2024-09-09", 4, List.of("1", "2", "3")));
		aggregations.add(createRouteAggregation("operator", "line", "outbound", "2024-09-09", 5, List.of("4")));
		aggregations.add(createRouteAggregation("xxxxxxxx", "line", "inbound", "2024-09-09", 1, List.of("1", "2")));
		aggregations.add(createRouteAggregation("operator", "xxxx", "inbound", "2024-09-09", 1, List.of("1", "2")));

		final MongoDbWriter writer = new MongoDbWriter(properties.getMongoConnectionString(), properties.getDatabaseName());
		writer.writeRouteAggregations(aggregations);
		writer.close();

		final RouteService routeService = new RouteService(properties, new RepositoryFactory(mongoClient));
		final Map<DirectionType, List<Route>> result = routeService.findRoutesByDirection("operator", "line", Optional.empty(),
				Optional.of(LocalDate.of(2024, 9, 9)), Optional.of(2), BigDecimal.valueOf(90), Optional.empty());

		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result).containsKey(DirectionType.inbound);
		assertThat(result).containsKey(DirectionType.outbound);
		assertThat(result.get(DirectionType.inbound)).hasSize(2);
		assertThat(result.get(DirectionType.outbound)).hasSize(1);

		{
			final Route route = result.get(DirectionType.inbound).get(0);
			assertThat(route.getOperatorCode()).isEqualTo("operator");
			assertThat(route.getLineCode()).isEqualTo("line");
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
			assertThat(route.getStopPlaces()).hasSize(2);
			assertThat(route.getNumberOfJourneys()).isEqualTo(6);
			assertThat(route.getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("60.000"));
		}
		{
			final Route route = result.get(DirectionType.inbound).get(1);
			assertThat(route.getOperatorCode()).isEqualTo("operator");
			assertThat(route.getLineCode()).isEqualTo("line");
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.inbound);
			assertThat(route.getStopPlaces()).hasSize(3);
			assertThat(route.getNumberOfJourneys()).isEqualTo(4);
			assertThat(route.getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("40.000"));
		}
		{
			final Route route = result.get(DirectionType.outbound).get(0);
			assertThat(route.getOperatorCode()).isEqualTo("operator");
			assertThat(route.getLineCode()).isEqualTo("line");
			assertThat(route.getDirectionType()).isEqualTo(DirectionType.outbound);
			assertThat(route.getStopPlaces()).hasSize(1);
			assertThat(route.getNumberOfJourneys()).isEqualTo(5);
			assertThat(route.getPercentagePerDirection()).isEqualByComparingTo(new BigDecimal("100.000"));
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
