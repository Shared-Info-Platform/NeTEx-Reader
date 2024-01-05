package ch.bernmobil.netex.importer.mongodb.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.mongodb.dom.CallWithJourney;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyWithCalls;

@Mapper
public interface JourneyMapper {

	JourneyMapper INSTANCE = Mappers.getMapper(JourneyMapper.class);

	@Mapping(target = "id", expression = "java(journey.id + \"_\" + journey.operatingDay)")
	JourneyWithCalls mapJourney(Journey journey);

	@Mapping(target = "id", expression = "java(journey.id + \"_\" + journey.operatingDay + \"_\" + call.order)")
	@Mapping(target = "originalId", source = "call.id")
	CallWithJourney mapCalls(Call call, Journey journey);
}
