package ch.bernmobil.netex.importer.mongodb.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.persistence.dom.CallWithJourney;
import ch.bernmobil.netex.persistence.dom.JourneyWithCalls;

/**
 * This interface maps a Journey from the internal model to the different "views" for MongoDB.
 *
 * The conversion between input- and output-model is done automatically by MapStruct. The annotations contain
 * some cues for non-trivial mappings.
 */
@Mapper(suppressTimestampInGenerated = true)
public interface JourneyMapper {

	JourneyMapper INSTANCE = Mappers.getMapper(JourneyMapper.class);

	@Mapping(target = "id", expression = "java(journey.id + \"_\" + journey.operatingDay)")
	@Mapping(target = "departureTime", expression = "java(journey.calls.get(0).departure.time)")
	@Mapping(target = "departureStopPlaceCode", expression = "java(journey.calls.get(0).stopPlaceCode)")
	@Mapping(target = "arrivalTime", expression = "java(journey.calls.get(journey.calls.size() - 1).arrival.time)")
	@Mapping(target = "arrivalStopPlaceCode", expression = "java(journey.calls.get(journey.calls.size() - 1).stopPlaceCode)")
	JourneyWithCalls mapJourney(Journey journey);

	@Mapping(target = "id", expression = "java(journey.id + \"_\" + journey.operatingDay + \"_\" + call.order)")
	@Mapping(target = "originalId", source = "call.id")
	@Mapping(target = "calendarDay", source = "call.calendarDay")
	CallWithJourney mapCalls(Call call, Journey journey);
}
