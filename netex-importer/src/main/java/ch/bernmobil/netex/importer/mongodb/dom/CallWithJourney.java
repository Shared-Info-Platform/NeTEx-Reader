package ch.bernmobil.netex.importer.mongodb.dom;

import org.bson.codecs.pojo.annotations.BsonId;

public class CallWithJourney extends Call {

	public String originalId; // required
	public String operatingDay; // required
	public String transportMode; // optional (from ServiceJourney or fallback from Line)
	public String serviceAlteration; // optional

	// operator
	public String operatorCode; // optional
	public String operatorName; // optional
	public String operatorShortName; // optional

	// Line
	public String lineCode; // optional
	public String lineName; // optional (required when Line is defined)
	public String lineShortName; // optional

	// Direction
	public String directionType; // required

	@BsonId
	public String getId() {
		return id;
	}
}
