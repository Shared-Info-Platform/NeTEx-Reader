package ch.bernmobil.netex.importer.mongodb.dom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.codecs.pojo.annotations.BsonId;

public class CallWithJourney extends Call {

	public String originalId; // required
	public String operatingDay; // required
	public String calendarDay; // required

	public String transportMode; // optional (from ServiceJourney or fallback from Line)
	public String serviceAlteration; // optional
	public String vehicleType; // optional
	public String productCategoryName; // optional
	public String productCategoryCode; // optional
	public List<String> trainNumbers = new ArrayList<>(); // optional
	public List<String> serviceFacilities = new ArrayList<>(); // optional
	public Map<String, String> notices = new LinkedHashMap<>(); // optional

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
