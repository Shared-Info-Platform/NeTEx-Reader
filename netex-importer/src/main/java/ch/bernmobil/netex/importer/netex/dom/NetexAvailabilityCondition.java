package ch.bernmobil.netex.importer.netex.dom;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NetexAvailabilityCondition {

	public String id; // required
	public LocalDateTime from; // required
	public LocalDateTime to; // required
	public List<LocalDate> validDays = new ArrayList<>(); // required
}
