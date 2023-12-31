package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexScheduledStopPoint {

	public String id; // required
	public String name; // optional
	public String shortName; // optional
	public String didok; // optional
	public List<NetexPassengerStopAssignment> assignments = new ArrayList<>(); // required - should be size 1 but is currently not
}
