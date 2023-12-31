package ch.bernmobil.netex.importer;

import ch.bernmobil.netex.importer.parser.ElementParser;
import ch.bernmobil.netex.importer.parser.MultilingualStringParser;
import ch.bernmobil.netex.importer.parser.Parser;
import ch.bernmobil.netex.importer.parser.TextParser;

public class ParserDefinitions {


	public static Parser createPublicationDeliveryParser(Parser framesParser) {
		return new ElementParser(true)
				.withChild("PublicationDelivery", new ElementParser()
					.withChild("PublicationTimestamp", new TextParser())
					.withChild("ParticipantRef", new TextParser())
					.withChild("Description", new TextParser())
					.withChild("dataObjects", new ElementParser()
						.withChild("CompositeFrame", new ElementParser()
							.withChild("ValidBetween", createValidBetweenParser())
							.withChild("FrameDefaults", new ElementParser()
								.withChild("DefaultLocale", new ElementParser()
									.withChild("TimeZoneOffset", new TextParser())
									.withChild("SummerTimeZoneOffset", new TextParser())
									.withChild("DefaultLanguage", new TextParser())
								)
							)
							.withChild("frames", framesParser)
						)
					)
				)
		;
	}

	public static Parser createCommonFramesParser() {
		return new ElementParser()
				.withChild("ResourceFrame", createResourceFrameParser())
				.withChild("SiteFrame", createSiteFrameParser())
				.withChild("ServiceFrame", createServiceFrameParser())
				.withChild("ServiceCalendarFrame", createServiceCalendarFrameParser())
				.withChild("TimetableFrame", createTimetableFrameCommonParser());
	}

	public static Parser createTimetableFramesParser() {
		return new ElementParser()
				.withChild("TimetableFrame", createTimetableFrameTimetableParser());
	}

	private static Parser createResourceFrameParser() {
		return new ElementParser()
				.withChild("responsibilitySets", null) // TODO
				.withChild("typesOfValue", new ElementParser()
					.withChild("ValueSet", new ElementParser()
						.withAttribute("id")
						.withAttribute("nameOfClass")
						.withChild("Name", new TextParser())
						.withChild("values", new ElementParser()
							.withChild("TypeOfNotice", new ElementParser()
								.withAttribute("id")
								.withChild("Name", new TextParser())
								.withChild("PrivateCode", new TextParser())
							)
							.withChild("TypeOfProductCategory", new ElementParser()
								.withAttribute("id")
								.withChild("Name", new MultilingualStringParser())
								.withChild("ShortName", new MultilingualStringParser())
								.withChild("alternativeTexts", createAlternativeTextsParser())
							)
						)
					)
				)
				.withChild("organisations", new ElementParser()
					.withChild("Operator", new ElementParser()
						.withAttribute("id")
						.withChild("PrivateCode", new TextParser())
						.withChild("Name", new TextParser())
						.withChild("ShortName", new TextParser())
					)
				)
				.withChild("vehicleTypes", new ElementParser()
					.withChild("VehicleType", new ElementParser()
						.withAttribute("id")
						.withChild("ShortName", new TextParser())
						.withChild("LowFloor", new TextParser())
						.withChild("HasLiftOrRamp", new TextParser())
						.withChild("HasHoist", new TextParser())
					)
				)
		;
	}

	private static Parser createSiteFrameParser() {
		return new ElementParser()
				.withChild("topographicPlaces", new ElementParser()
					.withChild("TopographicPlace", new ElementParser()
						.withAttribute("id")
						.withChild("keyList", createKeyListParser())
						.withChild("Descriptor", new ElementParser()
							.withChild("Name", new TextParser())
						)
						.withChild("TopographicPlaceType", new TextParser())
					)
				)
				.withChild("stopPlaces", new ElementParser()
					.withChild("StopPlace", new ElementParser()
						.withAttribute("id")
						.withChild("ValidBetween", createValidBetweenParser())
						.withChild("alternativeTexts", createAlternativeTextsParser())
						.withChild("keyList", createKeyListParser())
						.withChild("Extensions", null) // ignore
						.withChild("Name", new TextParser())
						.withChild("ShortName", new MultilingualStringParser())
						.withChild("PrivateCode", new TextParser())
						.withChild("Centroid", new ElementParser()
							.withChild("Location", createLocationParser())
						)
						.withChild("alternativeNames", createAlternativeNamesParser())
						.withChild("TopographicPlaceRef", createRefParser())
						.withChild("Weighting", new TextParser())
						.withChild("quays", new ElementParser()
							.withChild("Quay", new ElementParser()
								.withAttribute("id")
								.withChild("Centroid", new ElementParser()
									.withChild("Location", createLocationParser())
								)
								.withChild("PublicCode", new TextParser())
							)
						)
					)
				)
		;
	}

	private static Parser createServiceFrameParser() {
		return new ElementParser()
				.withChild("directions", new ElementParser()
					.withChild("Direction", new ElementParser()
						.withAttribute("id")
						.withChild("DirectionType", new TextParser())
					)
				)
				.withChild("lines", new ElementParser()
					.withChild("Line", new ElementParser()
						.withAttribute("id")
						.withChild("ValidBetween", createValidBetweenParser())
						.withChild("Name", new MultilingualStringParser())
						.withChild("ShortName", new MultilingualStringParser())
						.withChild("TransportMode", new TextParser())
						.withChild("TransportSubmode", null) // TODO (enum)
						.withChild("PublicCode", new TextParser())
						.withChild("OperatorRef", createRefParser())
					)
				)
				.withChild("destinationDisplays", new ElementParser()
					.withChild("DestinationDisplay", new ElementParser()
						.withAttribute("id")
						.withChild("Name", new MultilingualStringParser())
						.withChild("DriverDisplayText", new MultilingualStringParser())
						.withChild("PrivateCode", new TextParser())
					)
				)
				.withChild("scheduledStopPoints", new ElementParser()
					.withChild("ScheduledStopPoint", new ElementParser()
						.withChild("keyList", createKeyListParser())
						.withChild("Name", new MultilingualStringParser())
						.withChild("Location", createLocationParser())
						.withChild("ShortName", new MultilingualStringParser())
					)
				)
				.withChild("connections", null) // TODO
				.withChild("stopAssignments", new ElementParser()
					.withChild("PassengerStopAssignment", new ElementParser()
						.withAttribute("id")
						.withChild("ScheduledStopPointRef", createRefParser())
						.withChild("StopPlaceRef", createRefParser())
						.withChild("QuayRef", createRefParser())
					)
				)
				.withChild("notices", new ElementParser()
					.withChild("Notice", new ElementParser()
						.withAttribute("id")
						.withChild("alternativeTexts", createAlternativeTextsParser())
						.withChild("Text", new TextParser())
						.withChild("ShortCode", new TextParser())
						.withChild("PrivateCode", new TextParser())
						.withChild("TypeOfNoticeRef", createRefParser())
						.withChild("CanBeAdvertised", new TextParser())
					)
				)
		;
	}

	private static Parser createServiceCalendarFrameParser() {
		return new ElementParser()
				.withChild("validityConditions", new ElementParser()
					.withChild("AvailabilityCondition", new ElementParser()
						.withAttribute("id")
						.withChild("keyList", createKeyListParser())
						.withChild("FromDate", new TextParser())
						.withChild("ToDate", new TextParser())
						.withChild("ValidDayBits", new TextParser())
					)
				)
				.withChild("ServiceCalendar", new ElementParser()
					.withAttribute("id")
					.withChild("Name", new TextParser())
					.withChild("FromDate", new TextParser())
					.withChild("ToDate", new TextParser())
				)
				.withChild("dayTypes", null) // TODO
				.withChild("timebands", null) // TODO
		;
	}

	private static Parser createTimetableFrameCommonParser() {
		return new ElementParser()
				.withChild("serviceFacilitySets", new ElementParser()
					.withChild("ServiceFacilitySet", new ElementParser()
						.withAttribute("id")
						.withChild("alternativeTexts", createAlternativeTextsParser())
						.withChild("Extensions", new ElementParser()
							.withChild("Priority", new TextParser())
							.withChild("Condition", new TextParser())
						)
						.withChild("Description", new MultilingualStringParser())
						.withChild("PassengerCommsFacilityList", new TextParser())
						.withChild("CateringFacilityList", new TextParser())
						.withChild("NuisanceFacilityList", new TextParser())
						.withChild("MobilityFacilityList", new TextParser())
						.withChild("FareClasses", new TextParser())
					)
				)
				.withChild("typesOfService", new ElementParser()
					.withChild("TypeOfService", new ElementParser()
						.withAttribute("id")
						.withChild("Name", new TextParser())
						.withChild("ShortName", new TextParser())
						.withChild("PrivateCode", new TextParser())
					)
				)
				.withChild("journeyMeetings", null) // TODO
				.withChild("interchangeRules", null) // TODO
		;
	}

	private static Parser createTimetableFrameTimetableParser() {
		return new ElementParser()
				.withChild("vehicleJourneys", new ElementParser()
					.withChild("ServiceJourney", new ElementParser()
						.withAttribute("id")
						.withChild("validityConditions", new ElementParser()
							.withChild("AvailabilityConditionRef", createRefParser())
						)
						.withChild("keyList", createKeyListParser())
						.withChild("Extensions", new ElementParser()
							.withChild("facilities", new ElementParser()
								.withChild("ServiceFacilitySetRef", createRefParser())
							)
						)
						.withChild("PrivateCode", new TextParser())
						.withChild("TransportMode", new TextParser())
						.withChild("TypeOfProductCategoryRef", createRefParser())
						.withChild("TypeOfServiceRef", createRefParser())
						.withChild("noticeAssignments", new ElementParser()
							.withChild("NoticeAssignment", new ElementParser()
								.withAttribute("order")
								.withChild("NoticeRef", createRefParser())
							)
						)
						.withChild("ServiceAlteration", new TextParser())
						.withChild("DepartureTime", new TextParser())
						.withChild("DepartureDayOffset", new TextParser())
						.withChild("VehicleTypeRef", createRefParser())
						.withChild("LineRef", createRefParser())
						.withChild("DirectionType", new TextParser())
						.withChild("trainNumbers", new ElementParser()
							.withChild("TrainNumberRef", createRefParser())
						)
						.withChild("calls", new ElementParser()
							.withChild("Call", new ElementParser()
								.withAttribute("id")
								.withAttribute("order")
								.withChild("Extensions", new ElementParser()
									.withChild("facilities", new ElementParser()
										.withChild("ServiceFacilitySetRef", createRefParser())
									)
								)
								.withChild("ScheduledStopPointRef", createRefParser())
								.withChild("Arrival", new ElementParser()
									.withChild("Time", new TextParser())
									.withChild("DayOffset", new TextParser())
									.withChild("ForAlighting", new TextParser())
									.withChild("IsFlexible", new TextParser())
								)
								.withChild("Departure", new ElementParser()
									.withChild("Time", new TextParser())
									.withChild("DayOffset", new TextParser())
									.withChild("ForBoarding", new TextParser())
									.withChild("IsFlexible", new TextParser())
								)
								.withChild("DestinationDisplayRef", createRefParser())
								.withChild("noticeAssignments", new ElementParser()
									.withChild("NoticeAssignment", new ElementParser()
										.withAttribute("order")
										.withChild("NoticeRef", createRefParser())
									)
								)
								.withChild("RequestStop", new TextParser())
								.withChild("StopUse", new TextParser())
							)
						)
					)
				)
				.withChild("trainNumbers", new ElementParser() // TODO: parsed after Journeys, cannot be referenced
					.withChild("TrainNumber", new ElementParser()
						.withAttribute("id")
						.withChild("ForAdvertisement", new TextParser())
					)
				)
		;
	}

	private static Parser createValidBetweenParser() {
		return new ElementParser()
				.withChild("FromDate", new TextParser())
				.withChild("ToDate", new TextParser());
	}

	private static Parser createLocationParser() {
		return new ElementParser()
				.withChild("Longitude", new TextParser())
				.withChild("Latitude", new TextParser())
				.withChild("Altitude", new TextParser());
	}

	private static Parser createKeyListParser() {
		return new ElementParser()
				.withChild("KeyValue", new ElementParser()
					.withChild("Key", new TextParser())
					.withChild("Value", new TextParser())
				);
	}

	private static Parser createAlternativeTextsParser() {
		return new ElementParser()
				.withChild("AlternativeText", new ElementParser()
					.withAttribute("attributeName")
					.withAttribute("useForLanguage")
					.withChild("Text", new MultilingualStringParser())
				);
	}

	private static Parser createAlternativeNamesParser() {
		return new ElementParser()
				.withChild("AlternativeName", new ElementParser()
					.withChild("NameType", new TextParser())
					.withChild("TypeOfName", new TextParser())
					.withChild("Name", new MultilingualStringParser())
				);
	}

	private static Parser createRefParser() {
		return new ElementParser()
				.withAttribute("ref")
				.withAttribute("versionRef")
				.withAttribute("version");
	}
}
