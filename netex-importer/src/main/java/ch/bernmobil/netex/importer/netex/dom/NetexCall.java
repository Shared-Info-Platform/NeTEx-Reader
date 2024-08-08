package ch.bernmobil.netex.importer.netex.dom;

import java.time.LocalTime;

public class NetexCall {

	public String id; // required
	public int order; // required
	public NetexScheduledStopPoint scheduledStopPoint; // optional
	public NetexDestinationDisplay destinationDisplay; // optional
	public boolean requestStop; // required
	public String stopUse; // optional
	public Arrival arrival; // optional
	public Departure departure; // optional

	public static class Arrival {
		public LocalTime time; // required
		public int dayOffset; // required
		public boolean forAlighting; // required
		public boolean isFlexible; // required
	}

	public static class Departure {
		public LocalTime time; // required
		public int dayOffset; // required
		public boolean forBoarding; // required
		public boolean isFlexible; // required
	}
}
