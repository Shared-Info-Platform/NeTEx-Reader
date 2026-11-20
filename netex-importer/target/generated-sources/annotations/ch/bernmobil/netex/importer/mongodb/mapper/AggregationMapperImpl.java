package ch.bernmobil.netex.importer.mongodb.mapper;

import ch.bernmobil.netex.importer.journey.dom.CallAggregation;
import ch.bernmobil.netex.persistence.model.JourneyAggregation;
import ch.bernmobil.netex.persistence.model.RouteAggregation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 25.0.3 (Oracle Corporation)"
)
public class AggregationMapperImpl implements AggregationMapper {

    @Override
    public JourneyAggregation mapJourneyAggregation(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        JourneyAggregation journeyAggregation = new JourneyAggregation();

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
    public ch.bernmobil.netex.persistence.model.CallAggregation mapCallAggregation(CallAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        ch.bernmobil.netex.persistence.model.CallAggregation callAggregation = new ch.bernmobil.netex.persistence.model.CallAggregation();

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
    public RouteAggregation mapRouteAggregation(ch.bernmobil.netex.importer.journey.dom.RouteAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        RouteAggregation routeAggregation = new RouteAggregation();

        LocalDate calendarDay = aggregationIdCalendarDay2( aggregation );
        if ( calendarDay != null ) {
            routeAggregation.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( calendarDay );
        }
        routeAggregation.operatorCode = aggregationIdOperatorCode2( aggregation );
        routeAggregation.lineCode = aggregationIdLineCode2( aggregation );
        routeAggregation.regionCode = aggregationIdRegionCode2( aggregation );
        routeAggregation.directionId = aggregationIdDirectionId( aggregation );
        List<ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace> stopPlaces = aggregationIdStopPlaces( aggregation );
        routeAggregation.stopPlaces = stopPlaceListToStopPlaceList( stopPlaces );
        routeAggregation.journeys = aggregation.journeys;

        return routeAggregation;
    }

    private LocalDate aggregationIdCalendarDay(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdOperatorCode(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode(ch.bernmobil.netex.importer.journey.dom.JourneyAggregation journeyAggregation) {
        if ( journeyAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.JourneyAggregation.Id id = journeyAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private LocalDate aggregationIdCalendarDay1(CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdStopPlaceCode(CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String stopPlaceCode = id.stopPlaceCode;
        if ( stopPlaceCode == null ) {
            return null;
        }
        return stopPlaceCode;
    }

    private String aggregationIdOperatorCode1(CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode1(CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode1(CallAggregation callAggregation) {
        if ( callAggregation == null ) {
            return null;
        }
        CallAggregation.Id id = callAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private LocalDate aggregationIdCalendarDay2(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        LocalDate calendarDay = id.calendarDay;
        if ( calendarDay == null ) {
            return null;
        }
        return calendarDay;
    }

    private String aggregationIdOperatorCode2(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String operatorCode = id.operatorCode;
        if ( operatorCode == null ) {
            return null;
        }
        return operatorCode;
    }

    private String aggregationIdLineCode2(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String lineCode = id.lineCode;
        if ( lineCode == null ) {
            return null;
        }
        return lineCode;
    }

    private String aggregationIdRegionCode2(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String regionCode = id.regionCode;
        if ( regionCode == null ) {
            return null;
        }
        return regionCode;
    }

    private String aggregationIdDirectionId(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        String directionId = id.directionId;
        if ( directionId == null ) {
            return null;
        }
        return directionId;
    }

    private List<ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace> aggregationIdStopPlaces(ch.bernmobil.netex.importer.journey.dom.RouteAggregation routeAggregation) {
        if ( routeAggregation == null ) {
            return null;
        }
        ch.bernmobil.netex.importer.journey.dom.RouteAggregation.Id id = routeAggregation.id;
        if ( id == null ) {
            return null;
        }
        List<ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace> stopPlaces = id.stopPlaces;
        if ( stopPlaces == null ) {
            return null;
        }
        return stopPlaces;
    }

    protected RouteAggregation.StopPlace stopPlaceToStopPlace(ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace stopPlace) {
        if ( stopPlace == null ) {
            return null;
        }

        String code = null;
        String name = null;

        code = stopPlace.code();
        name = stopPlace.name();

        RouteAggregation.StopPlace stopPlace1 = new RouteAggregation.StopPlace( code, name );

        return stopPlace1;
    }

    protected List<RouteAggregation.StopPlace> stopPlaceListToStopPlaceList(List<ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace> list) {
        if ( list == null ) {
            return null;
        }

        List<RouteAggregation.StopPlace> list1 = new ArrayList<RouteAggregation.StopPlace>( list.size() );
        for ( ch.bernmobil.netex.importer.journey.dom.RouteAggregation.StopPlace stopPlace : list ) {
            list1.add( stopPlaceToStopPlace( stopPlace ) );
        }

        return list1;
    }
}
