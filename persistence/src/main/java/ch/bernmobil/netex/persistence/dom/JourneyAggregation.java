package ch.bernmobil.netex.persistence.dom;

import org.bson.codecs.pojo.annotations.BsonId;

public class JourneyAggregation {

	public String calendarDay;
	public String operatorCode;
	public String lineCode;
	public long journeys;

	@BsonId
	public String getId() {
		return calendarDay + "_" + operatorCode + "_" + lineCode;
	}
}
