package ch.bernmobil.netex.importer.mongodb.mapper;

import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.journey.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.model.CallAggregation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 22.0.2 (Oracle Corporation)"
)
public class AggregationMapperImpl implements AggregationMapper {

    @Override
    public ch.bernmobil.netex.persistence.model.JourneyAggregation mapJourneyAggregation(JourneyAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        ch.bernmobil.netex.persistence.model.JourneyAggregation journeyAggregation = new ch.bernmobil.netex.persistence.model.JourneyAggregation();

        LocalDate calendarDay = aggregationIdCalendarDay( aggregation );
        if ( calendarDay != null ) {
            journeyAggregation.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( calendarDay );
        }
        journeyAggregation.operatorCode = aggregationIdOperatorCode( aggregation );
        journeyAggregation.lineCode = aggregationIdLineCode( aggregation );
        journeyAggregation.regionCode = aggregationIdRegionCode( aggregation );
        journeyAggregation.journeys = aggregation.journeys;

        return journeyAggregation;
    }

    @Override
    public CallAggregation mapCallAggregation(ch.bernmobil.netex.importer.journey.dom.CallAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        CallAggregation callAggregation = new CallAggregation();

        LocalDate calendarDay = aggregationIdCalendarDay1( aggregation );
        if ( calendarDay != null ) {
            callAggregation.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( calendarDay );
        }
        callAggregation.stopPlaceCode = aggregationIdStopPlaceCode( aggregation );
        callAggregation.operatorCode = aggregationIdOperatorCode1( aggregation );
        callAggregation.lineCode = aggregationIdLineCode1( aggregation );
        callAggregation.regionCode = aggregationIdRegionCode1( aggregation );
        callAggregation.calls = aggregation.calls;

        return callAggregation;
    }

    @Override
    public ch.bernmobil.netex.persistence.model.RouteAggregation mapRouteAggregation(RouteAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        ch.bernmobil.netex.persistence.model.RouteAggregation routeAggregation = new ch.bernmobil.netex.persistence.model.RouteAggregation();

        LocalDate calendarDay = aggregationIdCalendarDay2( aggregation );
        if ( calendarDay != null ) {
            routeAggregation.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( calendarDay );
        }
        routeAggregation.operatorCode = aggregationIdOperatorCode2( aggregation );
        routeAggregation.lineCode = aggregationIdLineCode2( aggregation );
        routeAggregation.regionCode = aggregationIdRegionCode2( aggregation );
        routeAggregation.directionType = aggregationIdDirectionType( aggregation );
        routeAggregation.directionId = aggregationIdDirectionId( aggregation );
        List<RouteAggregation.StopPlace> stopPlaces = aggregationIdStopPlaces( aggregation );
        routeAggregation.stopPlaces = stopPlaceListToStopPlaceList( stopPlaces );
        routeAggregation.journeys = aggregation.journeys;

        return routeAggregation;
    }

    private LocalDate aggregationIdCalendarDay(JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdOperatorCode(JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode(JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode(JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private LocalDate aggregationIdCalendarDay1(ch.bernmobil.netex.importer.journey.dom.CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdStopPlaceCode(ch.bernmobil.netex.importer.journey.dom.CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String stopPlaceCode = id.stopPlaceCode;
        if ( stopPlaceCode == null ) {
            return null;
        }
        return stopPlaceCode;
    }

    private String aggregationIdOperatorCode1(ch.bernmobil.netex.importer.journey.dom.CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode1(ch.bernmobil.netex.importer.journey.dom.CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode1(ch.bernmobil.netex.importer.journey.dom.CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private LocalDate aggregationIdCalendarDay2(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdOperatorCode2(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode2(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode2(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private String aggregationIdDirectionType(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String directionType = id.directionType;
        if ( directionType == null ) {
            return null;
        }
        return directionType;
    }

    private String aggregationIdDirectionId(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String directionId = id.directionId;
        if ( directionId == null ) {
            return null;
        }
        return directionId;
    }

    private List<RouteAggregation.StopPlace> aggregationIdStopPlaces(RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        List<RouteAggregation.StopPlace> stopPlaces = id.stopPlaces;
        if ( stopPlaces == null ) {
            return null;
        }
        return stopPlaces;
    }

    protected ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace stopPlaceToStopPlace(RouteAggregation.StopPlace stopPlace) {
        if ( stopPlace == null ) {
            return null;
        }

        String code = null;
        String name = null;

        code = stopPlace.code();
        name = stopPlace.name();

        ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace stopPlace1 = new ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace( code, name );

        return stopPlace1;
    }

    protected List<ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace> stopPlaceListToStopPlaceList(List<RouteAggregation.StopPlace> list) {
        if ( list == null ) {
            return null;
        }

        List<ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace> list1 = new ArrayList<ch.bernmobil.netex.persistence.model.RouteAggregation.StopPlace>( list.size() );
        for ( RouteAggregation.StopPlace stopPlace : list ) {
            list1.add( stopPlaceToStopPlace( stopPlace ) );
        }

        return list1;
    }
}
