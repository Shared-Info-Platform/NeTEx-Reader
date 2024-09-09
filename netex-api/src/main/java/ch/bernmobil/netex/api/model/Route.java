package ch.bernmobil.netex.api.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A route is an ordered list of stop places for a specific combination of operator, line, and direction.")
public class Route {

	@Schema(description = "Identifies the organisation that operates the line of this route.")
	private String operatorCode;
	@Schema(description = "Identifies the line of this route.")
	private String lineCode;
	@Schema(description = "Identifies the direction of this route. There are only two valid direction types.")
	private DirectionType directionType;
	@Schema(description = "The stop places that a vehicle on this route passes.")
	private List<StopPlace> stopPlaces = new ArrayList<>();
	@Schema(description = "The number of journeys for this route in the requested time range.")
	private long numberOfJourneys;
	@Schema(description = "The percentage of `numberOfJourneys` compared to the sum of journeys of all routes of this direction.")
	private BigDecimal percentagePerDirection;

	public String getOperatorCode() {
		return operatorCode;
	}

	public void setOperatorCode(String operatorCode) {
		this.operatorCode = operatorCode;
	}

	public String getLineCode() {
		return lineCode;
	}

	public void setLineCode(String lineCode) {
		this.lineCode = lineCode;
	}

	public DirectionType getDirectionType() {
		return directionType;
	}

	public void setDirectionType(DirectionType directionType) {
		this.directionType = directionType;
	}

	public List<StopPlace> getStopPlaces() {
		return stopPlaces;
	}

	public void setStopPlaces(List<StopPlace> stopPlaces) {
		this.stopPlaces = stopPlaces;
	}

	public long getNumberOfJourneys() {
		return numberOfJourneys;
	}

	public void setNumberOfJourneys(long numberOfJourneys) {
		this.numberOfJourneys = numberOfJourneys;
	}

	public BigDecimal getPercentagePerDirection() {
		return percentagePerDirection;
	}

	public void setPercentagePerDirection(BigDecimal percentagePerDirection) {
		this.percentagePerDirection = percentagePerDirection;
	}

	public enum DirectionType {
		inbound, outbound
	}

	@Schema(description = "A stop place, defined by a code and a human readable name.")
	public record StopPlace(String code, String name) {
	}
}
