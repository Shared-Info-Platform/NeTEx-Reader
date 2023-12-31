package ch.bernmobil.netex.importer.netex.dom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetexAvailabilityCondition {

	public String id;
	public LocalDateTime from;
	public LocalDateTime to;
	public String validDayBits;
	public List<LocalDate> validDays = new ArrayList<>();
}
