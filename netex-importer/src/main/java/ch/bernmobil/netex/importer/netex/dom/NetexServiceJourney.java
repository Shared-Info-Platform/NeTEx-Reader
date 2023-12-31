package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexServiceJourney {

	public String id; // required
	public String sjyid; // optional
	public String privateCode; // optional
	public String transportMode; // optional
	public String serviceAlteration; // optional
	public NetexAvailabilityCondition availabilityCondition; // required
	public NetexResponsibilitySet responsibilitySet; // required
	public NetexLine line; // optional
	public String directionType; // required
	public List<NetexCall> calls = new ArrayList<>(); // required
}
