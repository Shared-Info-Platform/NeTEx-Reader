package ch.bernmobil.netex.importer;

import java.time.LocalDate;

public class ImporterProperties {

	private LocalDate firstCalendarDay;
	private LocalDate lastCalendarDay;
	private boolean writeCalls = true;

	public LocalDate getFirstCalendarDay() {
		return firstCalendarDay;
	}

	public void setFirstCalendarDay(LocalDate firstCalendarDay) {
		this.firstCalendarDay = firstCalendarDay;
	}

	public LocalDate getLastCalendarDay() {
		return lastCalendarDay;
	}

	public void setLastCalendarDay(LocalDate lastCalendarDay) {
		this.lastCalendarDay = lastCalendarDay;
	}

	public boolean isWriteCalls() {
		return writeCalls;
	}

	public void setWriteCalls(boolean writeCalls) {
		this.writeCalls = writeCalls;
	}
}
