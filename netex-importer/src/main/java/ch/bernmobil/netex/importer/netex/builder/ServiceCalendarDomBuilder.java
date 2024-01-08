package ch.bernmobil.netex.importer.netex.builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.builder.Frame.CompositeFrameHeader;
import ch.bernmobil.netex.importer.netex.dom.NetexAvailabilityCondition;

/**
 * This class reads the object tree of a NeTEx service calendar frame and stores the contained entities in the ImportState.
 */
public class ServiceCalendarDomBuilder {

	public static void buildDom(Frame serviceCalendarFrame, ImportState state) {
		final CompositeFrameHeader header = serviceCalendarFrame.compositeFrameHeader;
		final ObjectTree validityConditions = serviceCalendarFrame.frameTree.optionalChild("validityConditions");
		if (validityConditions != null) {
			validityConditions.children("AvailabilityCondition").stream().map(child -> buildAvailabilityCondition(child, header)).forEach(state::addAvailabilityCondition);
		}
	}

	private static NetexAvailabilityCondition buildAvailabilityCondition(ObjectTree tree, CompositeFrameHeader header) {
		final NetexAvailabilityCondition result = new NetexAvailabilityCondition();
		result.id = tree.text("id");

		final String fromDateTime = tree.optionalText("FromDate");
		final String toDateTime = tree.optionalText("ToDate");
		result.from = (fromDateTime != null ? LocalDateTime.parse(fromDateTime) : header.validFrom);
		result.to = (toDateTime != null ? LocalDateTime.parse(toDateTime) : header.validTo);

		final String validDayBits = tree.optionalText("ValidDayBits");
		final LocalDate fromDateInclusive = result.from.toLocalDate();
		final LocalDate toDateExclusive = result.to.toLocalDate().plusDays(2); // TODO: there seems to be a bug in the data, the last day is missing; should only be +1
		final long numberOfDays = fromDateInclusive.until(toDateExclusive, ChronoUnit.DAYS);
		if (validDayBits == null || validDayBits.length() == numberOfDays) {
			LocalDate date = fromDateInclusive;
			for (int i = 0; i < numberOfDays; ++i) {
				if (validDayBits == null || validDayBits.charAt(i) == '1') {
					result.validDays.add(date);
				}
				date = date.plusDays(1);
			}
		} else {
			throw new IllegalArgumentException("ValidDayBits in " + result.id + " has unexpected length " + validDayBits.length());
		}

		return result;
	}
}
