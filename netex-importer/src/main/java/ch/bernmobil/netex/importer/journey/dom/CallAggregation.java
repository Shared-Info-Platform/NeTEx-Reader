package ch.bernmobil.netex.importer.journey.dom;

import java.time.LocalDate;
import java.util.Objects;

public class CallAggregation {

	public Id id;
	public long calls;

	public CallAggregation(Id id) {
		this.id = id;
	}

	public static class Id {
		public LocalDate calendarDay;
		public String stopPlaceCode;
		public String operatorCode;
		public String lineCode;
		public String regionCode;

		@Override
		public int hashCode() {
			return Objects.hash(calendarDay, lineCode, regionCode, operatorCode, stopPlaceCode);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			Id other = (Id) obj;
			return Objects.equals(calendarDay, other.calendarDay)
					&& Objects.equals(lineCode, other.lineCode)
					&& Objects.equals(regionCode, other.regionCode)
					&& Objects.equals(operatorCode, other.operatorCode)
					&& Objects.equals(stopPlaceCode, other.stopPlaceCode);
		}
	}
}
