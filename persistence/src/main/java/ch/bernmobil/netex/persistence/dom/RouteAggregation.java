package ch.bernmobil.netex.persistence.dom;

import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class RouteAggregation {

	public static final String FIELDNAME_CALENDAR_DAY = "calendarDay";
	public static final String FIELDNAME_OPERATOR_CODE = "operatorCode";
	public static final String FIELDNAME_LINE_CODE = "lineCode";
	public static final String FIELDNAME_DIRECTION_TYPE = "directionType";
	public static final String FIELDNAME_STOP_PLACES = "stopPlaces";
	public static final String FIELDNAME_JOURNEYS = "journeys";

	@BsonProperty(FIELDNAME_CALENDAR_DAY)
	public String calendarDay;

	@BsonProperty(FIELDNAME_OPERATOR_CODE)
	public String operatorCode;

	@BsonProperty(FIELDNAME_LINE_CODE)
	public String lineCode;

	@BsonProperty(FIELDNAME_DIRECTION_TYPE)
	public String directionType;

	@BsonProperty(FIELDNAME_STOP_PLACES)
	public List<StopPlace> stopPlaces = new ArrayList<>();

	@BsonProperty(FIELDNAME_JOURNEYS)
	public long journeys;

	@BsonId
	public String getId() {
		return calendarDay + "_" + operatorCode + "_" + lineCode + "_" + directionType + "_" + stopPlaces.hashCode();
	}

	public record StopPlace(String code, String name) {}
}
