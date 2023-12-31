package ch.bernmobil.netex.importer.netex.dom;

import java.util.ArrayList;
import java.util.List;

public class NetexStopPlace {

	public String id;
	public String didok;
	public String sloid;
	public String name;
	public String shortName;
	public String privateCode;
	public final List<NetexQuay> quays = new ArrayList<>();
}
