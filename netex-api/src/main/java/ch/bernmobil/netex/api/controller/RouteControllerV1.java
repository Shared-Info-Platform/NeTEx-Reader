package ch.bernmobil.netex.api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ch.bernmobil.netex.api.model.Route;
import ch.bernmobil.netex.api.model.Route.DirectionType;
import ch.bernmobil.netex.api.service.RouteService;

@RestController
@RequestMapping(value = "/routes/v1")
public class RouteControllerV1 {

	private final RouteService routeService;

	public RouteControllerV1(RouteService routeService) {
		this.routeService = routeService;
	}

    @RequestMapping(
            value = "find",
            method = RequestMethod.GET,
            produces = { "application/json" }
    )
    public Map<DirectionType, List<Route>> findRoutesByDirection(
    		@RequestParam(required = true) String operatorCode,
    		@RequestParam(required = true) String lineCode,
    		@RequestParam(required = false) String directionType,
    		@RequestParam(required = false) LocalDate calendarDay,
    		@RequestParam(required = false) Integer previewDays,
    		@RequestParam(required = false, defaultValue = "90") BigDecimal threshold,
    		@RequestParam(required = false) String databaseName) {
    	return routeService.findRoutesByDirection(operatorCode, lineCode, Optional.ofNullable(directionType), Optional.ofNullable(calendarDay),
    			Optional.ofNullable(previewDays), threshold, Optional.ofNullable(databaseName));
    }
}
