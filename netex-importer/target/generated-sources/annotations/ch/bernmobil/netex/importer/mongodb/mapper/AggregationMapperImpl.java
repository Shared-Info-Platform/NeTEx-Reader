package ch.bernmobil.netex.importer.mongodb.mapper;

import ch.bernmobil.netex.importer.journey.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.mongodb.dom.CallAggregation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-01-06T23:20:32+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.7 (Oracle Corporation)"
)
public class AggregationMapperImpl implements AggregationMapper {

    @Override
    public ch.bernmobil.netex.importer.mongodb.dom.JourneyAggregation mapJourneyAggregation(JourneyAggregation aggregation) {
        if ( aggregation == null ) {
            return null;
        }

        ch.bernmobil.netex.importer.mongodb.dom.JourneyAggregation journeyAggregation = new ch.bernmobil.netex.importer.mongodb.dom.JourneyAggregation();

        LocalDate calendarDay = aggregationIdCalendarDay( aggregation );
        if ( calendarDay != null ) {
            journeyAggregation.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( calendarDay );
        }
        journeyAggregation.operatorCode = aggregationIdOperatorCode( aggregation );
        journeyAggregation.lineCode = aggregationIdLineCode( aggregation );
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
        callAggregation.calls = aggregation.calls;

        return callAggregation;
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
}
