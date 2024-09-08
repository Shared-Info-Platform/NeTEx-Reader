package ch.bernmobil.netex.importer.journey.dom;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RouteAggregation {

	public Id id;
	public long journeys;

	public RouteAggregation(Id id) {
		this.id = id;
	}

	public static class Id {
		public LocalDate calendarDay;
		public String operatorCode;
		public String lineCode;
		public String directionType;
		public List<StopPlace> stopPlaces = new ArrayList<>();

		@Override
		public int hashCode() {
			return Objects.hash(calendarDay, operatorCode, lineCode, directionType, stopPlaces);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null ||  getClass() != obj.getClass()) {
				return false;
			}
			Id other = (Id) obj;
			return Objects.equals(calendarDay, other.calendarDay)
					&& Objects.equals(operatorCode, other.operatorCode)
					&& Objects.equals(lineCode, other.lineCode)
					&& Objects.equals(directionType, other.directionType)
					&& Objects.equals(stopPlaces, other.stopPlaces);
		}
	}

	public record StopPlace(String code, String name) {}
}
