package ch.bernmobil.netex.persistence.dom;

import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

public class RouteAggregation {

	public String calendarDay;
	public String operatorCode;
	public String lineCode;
	public String directionType;
	public List<StopPlace> stopPlaces = new ArrayList<>();
	public long journeys;

	@BsonId
	public String getId() {
		return calendarDay + "_" + operatorCode + "_" + lineCode + "_" + directionType + "_" + stopPlaces.hashCode();
	}

	public record StopPlace(String code, String name) {}
}
