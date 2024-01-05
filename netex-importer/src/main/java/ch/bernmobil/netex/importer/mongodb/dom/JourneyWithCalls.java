package ch.bernmobil.netex.importer.mongodb.dom;

import java.util.ArrayList;
import java.util.List;

import org.bson.codecs.pojo.annotations.BsonId;

public class JourneyWithCalls {

	@BsonId
	public String id; // required
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

	// Calls
	public List<Call> calls = new ArrayList<>();
}
