package ch.bernmobil.netex.importer.journey.dom;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Journey {

	public String id; // required
	public LocalDate operatingDay; // required
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
