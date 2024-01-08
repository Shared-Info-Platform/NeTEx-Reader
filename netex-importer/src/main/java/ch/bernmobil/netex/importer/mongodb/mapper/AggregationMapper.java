package ch.bernmobil.netex.importer.mongodb.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.bernmobil.netex.importer.mongodb.dom.CallAggregation;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyAggregation;

/**
 * This interface maps Aggregations from the internal model to the model used in MongoDB.
 *
 * The conversion between input- and output-model is done automatically by MapStruct. The annotations contain
 * some cues for non-trivial mappings.
 */
@Mapper
public interface AggregationMapper {

	AggregationMapper INSTANCE = Mappers.getMapper(AggregationMapper.class);

	@Mapping(target = "calendarDay", source = "id.calendarDay")
	@Mapping(target = "operatorCode", source = "id.operatorCode")
	@Mapping(target = "lineCode", source = "id.lineCode")
	JourneyAggregation mapJourneyAggregation(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation aggregation);

	@Mapping(target = "calendarDay", source = "id.calendarDay")
	@Mapping(target = "stopPlaceCode", source = "id.stopPlaceCode")
	@Mapping(target = "operatorCode", source = "id.operatorCode")
	@Mapping(target = "lineCode", source = "id.lineCode")
	CallAggregation mapCallAggregation(ch.bernmobil.netex.importer.journey.dom.CallAggregation aggregation);
}
