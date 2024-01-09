package ch.bernmobil.netex.importer.netex.builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.ImportState;
import ch.bernmobil.netex.importer.netex.builder.Frame.CompositeFrameHeader;
import ch.bernmobil.netex.importer.netex.dom.NetexAvailabilityCondition;

/**
 * This class reads the object tree of a NeTEx service calendar frame and stores the contained entities in the ImportState.
 */
public class ServiceCalendarDomBuilder {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCalendarDomBuilder.class);

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
		final LocalDate toDateExclusive = result.to.toLocalDate().plusDays(1); // "to" is inclusive in NeTEx, add +1 to make it exclusive
		final long numberOfDays = fromDateInclusive.until(toDateExclusive, ChronoUnit.DAYS);

		// validate length of validDayBits, its length (if defined) should equal numberOfDays
		if (validDayBits != null && validDayBits.length() > numberOfDays) {
			// this shouldn't happen (bug in the data), but we can use it anyway
			LOGGER.warn("ValidDayBits in {} has length {} instead of {}", result.id, validDayBits.length(), numberOfDays);
		} else if (validDayBits != null && validDayBits.length() < numberOfDays) {
			// this also shouln't happen but we cannot use it in this case
			throw new IllegalArgumentException("ValidDayBits in " + result.id + " has length " + validDayBits.length() + " instead of " + numberOfDays);
		}

		LocalDate date = fromDateInclusive;
		for (int i = 0; i < numberOfDays; ++i) {
			if (validDayBits == null || validDayBits.charAt(i) == '1') {
				result.validDays.add(date);
			}
			date = date.plusDays(1);
		}

		return result;
	}
}
