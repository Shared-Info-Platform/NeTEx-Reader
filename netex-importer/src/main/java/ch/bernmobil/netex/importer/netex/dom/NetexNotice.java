package ch.bernmobil.netex.importer.netex.dom;

public class NetexNotice {

	public String id; // required
	public String text; // optional
	public String shortCode; // ?
	public String privateCode; // ?
	public Boolean canBeAdvertised; // optional
	public NetexTypeOfNotice typeOfNotice; // required
}
