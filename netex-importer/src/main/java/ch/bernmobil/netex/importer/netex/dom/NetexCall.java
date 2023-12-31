package ch.bernmobil.netex.importer.netex.dom;

import java.time.LocalTime;

public class NetexCall {

	public String id;
	public int order;
	public NetexScheduledStopPoint scheduledStopPoint;
	public NetexDestinationDisplay destinationDisplay;
	public boolean requestStop;
	public boolean stopUse;
	public Arrival arrival;
	public Departure departure;

	public static class Arrival {
		public LocalTime time;
		public int dayOffset;
		public boolean forAlighting;
		public boolean isFlexible;
	}

	public static class Departure {
		public LocalTime time;
		public int dayOffset;
		public boolean forBoarding;
		public boolean isFlexible;
	}
}
