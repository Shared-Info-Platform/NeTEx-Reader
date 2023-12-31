package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexScheduledStopPoint {

	public String id;
	public String name;
	public String shortName;
	public String didok;
	public List<NetexPassengerStopAssignment> assignments = new ArrayList<>();
}
