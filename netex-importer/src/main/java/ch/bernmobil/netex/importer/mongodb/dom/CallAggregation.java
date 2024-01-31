package ch.bernmobil.netex.importer.mongodb.dom;

import org.bson.codecs.pojo.annotations.BsonId;

public class CallAggregation {

	public String calendarDay;
	public String stopPlaceCode;
	public String operatorCode;
	public String lineCode;
	public long calls;

	@BsonId
	public String getId() {
		return calendarDay + "_" + stopPlaceCode + "_" + operatorCode + "_" + lineCode;
	}
}
