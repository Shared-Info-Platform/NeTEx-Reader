package ch.bernmobil.netex.importer.journey.dom;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

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
		public ZonedDateTime time; // required
		public boolean forAlighting; // required
		public boolean isFlexible; // required
	}

	public static class Departure {
		public ZonedDateTime time; // required
		public boolean forBoarding; // required
		public boolean isFlexible; // required
	}

	public LocalDate getCalendarDay() {
		return (departure != null ? departure.time : arrival.time).withZoneSameInstant(ZoneOffset.UTC).toLocalDate();
	}
}
