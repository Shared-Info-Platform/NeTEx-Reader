package ch.bernmobil.netex.api.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Route {

	private String operatorCode;
	private String lineCode;
	private DirectionType directionType;
	private List<StopPlace> stopPlaces = new ArrayList<>();
	private long numberOfJourneys;
	private BigDecimal percentage;

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

	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	public enum DirectionType {
		inbound, outbound
	}

	public record StopPlace(String code, String name) {
	}
}
