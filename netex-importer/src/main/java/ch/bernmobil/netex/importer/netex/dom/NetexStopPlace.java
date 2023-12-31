package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexStopPlace {

	public String id; // required
	public String didok; // optional
	public String sloid; // optional
	public String name; // optional
	public String shortName; // optional
	public String privateCode; // optional
	public final List<NetexQuay> quays = new ArrayList<>(); // required
}
