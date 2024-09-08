package ch.bernmobil.netex.persistence.dom;

import java.time.Instant;

public class Call {

	public String id; // required
	public int order; // required
	public boolean requestStop; // required
	public String stopUse; // optional

	// ScheduledStopPoint
	public String stopPlaceCode; // optional
	public String stopPlaceName; // optional
	public String stopPointName; // optional

	// NetexDestinationDisplay
	public String destinationDisplayName; // optional (required when DestinationDisplay is defined)

	public Arrival arrival; // optional
	public Departure departure; // optional

	public static class Arrival {
		public Instant time; // required
		public boolean forAlighting; // required
		public boolean isFlexible; // required
	}

	public static class Departure {
		public Instant time; // required
		public boolean forBoarding; // required
		public boolean isFlexible; // required
	}
}
