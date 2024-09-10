package ch.bernmobil.netex.api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.api.model.Route.StopPlace;
import ch.bernmobil.netex.api.service.RouteService;
import ch.bernmobil.netex.api.service.RouteService.NotFoundException;

@WebMvcTest(RouteControllerV1.class)
@ActiveProfiles("test")
public class RouteControllerV1Test {

    @Autowired
    private MockMvc mvc;

    private static RouteService routeService = Mockito.mock(RouteService.class);

    @BeforeAll
    public static void setup() {
    	final Route route1 = new Route();
    	route1.setOperatorCode("operator");
    	route1.setLineCode("line");
    	route1.setDirectionType(DirectionType.inbound);
    	route1.setStopPlaces(List.of(new StopPlace("1", "1"), new StopPlace("2", "2")));
    	route1.setNumberOfJourneys(2);
    	route1.setPercentagePerDirection(new BigDecimal("66.667"));

    	final Route route2 = new Route();
    	route2.setOperatorCode("operator");
    	route2.setLineCode("line");
    	route2.setDirectionType(DirectionType.inbound);
    	route2.setStopPlaces(List.of(new StopPlace("1", "1")));
    	route2.setNumberOfJourneys(1);
    	route2.setPercentagePerDirection(new BigDecimal("33.333"));

    	final Route route3 = new Route();
    	route3.setOperatorCode("operator");
    	route3.setLineCode("line");
    	route3.setDirectionType(DirectionType.outbound);
    	route3.setStopPlaces(List.of(new StopPlace("4", "4")));
    	route3.setNumberOfJourneys(3);
    	route3.setPercentagePerDirection(new BigDecimal("100.000"));

    	final Map<DirectionType, List<Route>> routes = new TreeMap<>(Map.of(DirectionType.inbound, List.of(route1, route2), DirectionType.outbound, List.of(route3)));

		Mockito.when(routeService.findRoutesByDirection(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(routes);
		Mockito.when(routeService.findRoutesByDirection(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any(),
				Mockito.any(), Mockito.any(), Mockito.eq(Optional.of("not existing")))).thenThrow(NotFoundException.class);
    }

    @Configuration
    public static class TestConfiguration {
    	@Bean
    	public RouteControllerV1 createRouteControllerV1() {
    		return new RouteControllerV1(routeService);
    	}
    }

    @Test
    public void testReturns400WhenOperatorIsMissing() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
        		.queryParam("lineCode", "line"))
                .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testReturns400WhenLineIsMissing() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
        		.queryParam("operatorCode", "operator"))
        		.andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testReturns200WhenOperatorAndLineAreAvailable() throws Exception {
    	mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line"))
    			.andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    public void testReturns400WhenCalendarDayIsMalformed() throws Exception {
    	mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line").queryParam("calendarDay", "1-2-3"))
        		.andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    @Test
    public void testPassesRequiredArgumentsToRouteService() throws Exception {
    	mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line"))
        		.andExpect(MockMvcResultMatchers.status().isOk());
		Mockito.verify(routeService).findRoutesByDirection("operator", "line", Optional.empty(), Optional.empty(),
				Optional.empty(), BigDecimal.valueOf(90), Optional.empty());
    }

    @Test
    public void testPassesAllArgumentsToRouteService() throws Exception {
    	mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line").queryParam("directionType", "inbound")
    			.queryParam("calendarDay", "2024-09-09").queryParam("previewDays", "2").queryParam("threshold", "50.05").queryParam("databaseName", "database"))
        		.andExpect(MockMvcResultMatchers.status().isOk());
		Mockito.verify(routeService).findRoutesByDirection("operator", "line", Optional.of(DirectionType.inbound),
				Optional.of(LocalDate.of(2024, 9, 9)), Optional.of(2), new BigDecimal("50.05"), Optional.of("database"));
    }

    @Test
    public void testReturns404WhenDatabaseIsMissing() throws Exception {
    	mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line").queryParam("databaseName", "not existing"))
    			.andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    public void testReturnsCorrectJson() throws Exception {
    	MvcResult result = mvc.perform(MockMvcRequestBuilders.get("/routes/v1/find").accept(MediaType.APPLICATION_JSON)
    			.queryParam("operatorCode", "operator").queryParam("lineCode", "line"))
    			.andExpect(MockMvcResultMatchers.status().isOk())
    			.andReturn();
        final String json = result.getResponse().getContentAsString();
        assertThat(json).isEqualTo(expectedJson());
    }

    private String expectedJson() {
        return "{"
        		+ "\"inbound\":["
        			+ "{"
        				+ "\"operatorCode\":\"operator\","
        				+ "\"lineCode\":\"line\","
        				+ "\"directionType\":\"inbound\","
        				+ "\"stopPlaces\":["
        					+ "{\"code\":\"1\",\"name\":\"1\"},"
        					+ "{\"code\":\"2\",\"name\":\"2\"}"
    					+ "],"
    					+ "\"numberOfJourneys\":2,"
    					+ "\"percentagePerDirection\":66.667"
					+ "},"
					+ "{"
						+ "\"operatorCode\":\"operator\","
						+ "\"lineCode\":\"line\","
						+ "\"directionType\":\"inbound\","
						+ "\"stopPlaces\":["
							+ "{\"code\":\"1\",\"name\":\"1\"}"
						+ "],"
						+ "\"numberOfJourneys\":1,"
						+ "\"percentagePerDirection\":33.333"
					+ "}"
				+ "],"
				+ "\"outbound\":["
					+ "{"
						+ "\"operatorCode\":\"operator\","
						+ "\"lineCode\":\"line\","
						+ "\"directionType\":\"outbound\","
						+ "\"stopPlaces\":["
							+ "{\"code\":\"4\",\"name\":\"4\"}"
						+ "],"
						+ "\"numberOfJourneys\":3,"
						+ "\"percentagePerDirection\":100.000"
					+ "}"
				+ "]"
			+ "}";
    }
}
