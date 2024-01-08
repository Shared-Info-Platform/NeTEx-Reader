package ch.bernmobil.netex.importer;

import ch.bernmobil.netex.importer.netex.builder.BuilderHelper;
import ch.bernmobil.netex.importer.xml.ElementParser;
import ch.bernmobil.netex.importer.xml.MultilingualStringParser;
import ch.bernmobil.netex.importer.xml.Parser;
import ch.bernmobil.netex.importer.xml.TextParser;

/**
 * This class contains definitions for how to parse the NeTEx files with a StAX parser. A StAX parser is different
 * to a "regular" DOM parser: A DOM parser reads a whole XML file at once and builds a model of all its content in
 * memory. The in-memory model then allows random access to all entities of the XML file in any order. A StAX parser
 * however iterates over all elements in an XML file one by one and lets the application handle it. There is no
 * in-memory model and no random access is possible. All elements have to be parsed in the same order they appear in
 * the XML. Also unnecessary elements cannot just be ignored in a StAX parser, the application must handle them as
 * well and ignore them "actively".
 *
 * The definitions in this class tell the StAX parser which elements are expected at which position in the node
 * tree and how to parse them. All elements that are not defined here are ignored.
 */
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
				.withChild(BuilderHelper.RESOURCE_FRAME_NAME, createResourceFrameParser())
				.withChild(BuilderHelper.SITE_FRAME_NAME, createSiteFrameParser())
				.withChild(BuilderHelper.SERVICE_FRAME_NAME, createServiceFrameParser())
				.withChild(BuilderHelper.SERVICE_CALENDAR_FRAME_NAME, createServiceCalendarFrameParser())
				.withChild(BuilderHelper.TIMETABLE_FRAME_NAME, createTimetableFrameCommonParser());
	}

	public static Parser createTimetableFramesParser() {
		return new ElementParser()
				.withChild("TimetableFrame", createTimetableFrameTimetableParser());
	}

	private static Parser createResourceFrameParser() {
		return new ElementParser()
				.withChild("responsibilitySets", new ElementParser()
					.withChild("ResponsibilitySet", new ElementParser()
						.withAttribute("id")
						.withChild("Name", new MultilingualStringParser())
						.withChild("PrivateCode", new TextParser())
						.withChild("roles", new ElementParser()
							.withChild("ResponsibilityRoleAssignment", new ElementParser()
								.withAttribute("id")
								.withChild("StakeholderRoleType", new TextParser())
								.withChild("ResponsibleOrganisationRef", createRefParser())
							)
						)
					)
				)
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
						.withChild("ShortName", new MultilingualStringParser())
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
						.withChild("ShortName", new TextParser()) // multilingual?
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
						.withAttribute("id")
						.withChild("keyList", createKeyListParser())
						.withChild("Name", new MultilingualStringParser())
						.withChild("Location", createLocationParser())
						.withChild("ShortName", new MultilingualStringParser())
					)
				)
				.withChild("connections", null)
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
						.withChild("Text", new MultilingualStringParser())
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
				.withChild("dayTypes", null)
				.withChild("timebands", null)
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
				.withChild("journeyMeetings", null)
				.withChild("interchangeRules", null)
		;
	}

	private static Parser createTimetableFrameTimetableParser() {
		return new ElementParser()
				.withChild("vehicleJourneys", new ElementParser()
					.withChild("ServiceJourney", new ElementParser()
						.withAttribute("id")
						.withAttribute("responsibilitySetRef")
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
									.withChild("CheckConstraint", null) // ignore
								)
								.withChild("Departure", new ElementParser()
									.withChild("Time", new TextParser())
									.withChild("DayOffset", new TextParser())
									.withChild("ForBoarding", new TextParser())
									.withChild("IsFlexible", new TextParser())
									.withChild("CheckConstraint", null) // ignore
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
