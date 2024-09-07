package ch.bernmobil.netex.importer.mongodb.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import ch.bernmobil.netex.importer.journey.dom.CallAggregation;
import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.journey.dom.RouteAggregation;

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
	ch.bernmobil.netex.persistence.dom.JourneyAggregation mapJourneyAggregation(JourneyAggregation aggregation);

	@Mapping(target = "calendarDay", source = "id.calendarDay")
	@Mapping(target = "stopPlaceCode", source = "id.stopPlaceCode")
	@Mapping(target = "operatorCode", source = "id.operatorCode")
	@Mapping(target = "lineCode", source = "id.lineCode")
	ch.bernmobil.netex.persistence.dom.CallAggregation mapCallAggregation(CallAggregation aggregation);

	@Mapping(target = "calendarDay", source = "id.calendarDay")
	@Mapping(target = "operatorCode", source = "id.operatorCode")
	@Mapping(target = "lineCode", source = "id.lineCode")
	@Mapping(target = "directionType", source = "id.directionType")
	@Mapping(target = "stopPlaceCodes", source = "id.stopPlaceCodes")
	ch.bernmobil.netex.persistence.dom.RouteAggregation mapRouteAggregation(RouteAggregation aggregation);
}
