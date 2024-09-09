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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping(value = "/routes/v1")
public class RouteControllerV1 {

	private final RouteService routeService;

	public RouteControllerV1(RouteService routeService) {
		this.routeService = routeService;
	}

    @Operation(
            description = "Returns routes that match the given parameters, grouped by direction and ordered by most frequent routes first.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Success."),
                    @ApiResponse(responseCode = "400", description = "Required parameters missing or parameters wrong formatted.", content = {
                            @Content
                    }),
                    @ApiResponse(responseCode = "404", description = "Database not found.", content = {
                            @Content
                    })
            }
    )
    @RequestMapping(
            value = "find",
            method = RequestMethod.GET,
            produces = { "application/json" }
    )
    public Map<DirectionType, List<Route>> findRoutesByDirection(
    		@Parameter(description = "The operator code (e.g. '11' for SBB)")
    		@RequestParam(required = true) String operatorCode,
    		@Parameter(description = "The line code (e.g. '1' or 'IC5')")
    		@RequestParam(required = true) String lineCode,
    		@Parameter(description = "Optional: The direction type. Default: All directions")
    		@RequestParam(required = false) DirectionType directionType,
    		@Parameter(description = "Optional: Get routes for this day (format YYYY-MM-DD). Default: Today")
    		@RequestParam(required = false) LocalDate calendarDay,
    		@Parameter(description = "Optional: Get routes for this amount of additional days after `calendarDay`. Default: 0")
    		@RequestParam(required = false) Integer previewDays,
    		@Parameter(description = "Optional: Limits the number of returned routes. The response contains as many routes as "
    				+ "necessary to cover at least this percentage of journeys per direction, ordered by the most frequent routes first.")
    		@RequestParam(required = false, defaultValue = "90") BigDecimal threshold,
    		@Parameter(description = "Optional: Get routes from a different database than the default")
    		@RequestParam(required = false) String databaseName) {
    	return routeService.findRoutesByDirection(operatorCode, lineCode, Optional.ofNullable(directionType), Optional.ofNullable(calendarDay),
    			Optional.ofNullable(previewDays), threshold, Optional.ofNullable(databaseName));
    }
}
