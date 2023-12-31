package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexServiceJourney {

	public String id;
	public String sjyid;
	public String privateCode;
	public String transportMode;
	public String serviceAlteration;
	public NetexAvailabilityCondition availabilityCondition;
	public NetexResponsibilitySet responsibilitySet;
	public NetexLine line;
	public String directionType;
	public List<NetexCall> calls = new ArrayList<>();
}
