package ch.bernmobil.netex.importer.mongodb.mapper;

import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.mongodb.dom.CallWithJourney;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyWithCalls;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-01-06T20:42:33+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.7 (Oracle Corporation)"
)
public class JourneyMapperImpl implements JourneyMapper {

    @Override
    public JourneyWithCalls mapJourney(Journey journey) {
        if ( journey == null ) {
            return null;
        }

        JourneyWithCalls journeyWithCalls = new JourneyWithCalls();

        if ( journey.operatingDay != null ) {
            journeyWithCalls.operatingDay = DateTimeFormatter.ISO_LOCAL_DATE.format( journey.operatingDay );
        }
        journeyWithCalls.transportMode = journey.transportMode;
        journeyWithCalls.serviceAlteration = journey.serviceAlteration;
        journeyWithCalls.operatorCode = journey.operatorCode;
        journeyWithCalls.operatorName = journey.operatorName;
        journeyWithCalls.operatorShortName = journey.operatorShortName;
        journeyWithCalls.lineCode = journey.lineCode;
        journeyWithCalls.lineName = journey.lineName;
        journeyWithCalls.lineShortName = journey.lineShortName;
        journeyWithCalls.directionType = journey.directionType;
        journeyWithCalls.calls = callListToCallList( journey.calls );

        journeyWithCalls.id = journey.id + "_" + journey.operatingDay;

        return journeyWithCalls;
    }

    @Override
    public CallWithJourney mapCalls(Call call, Journey journey) {
        if ( call == null && journey == null ) {
            return null;
        }

        CallWithJourney callWithJourney = new CallWithJourney();

        if ( call != null ) {
            callWithJourney.originalId = call.id;
            callWithJourney.order = call.order;
            callWithJourney.requestStop = call.requestStop;
            callWithJourney.stopUse = call.stopUse;
            callWithJourney.stopPlaceCode = call.stopPlaceCode;
            callWithJourney.stopPlaceName = call.stopPlaceName;
            callWithJourney.stopPointName = call.stopPointName;
            callWithJourney.destinationDisplayName = call.destinationDisplayName;
            callWithJourney.arrival = arrivalToArrival( call.arrival );
            callWithJourney.departure = departureToDeparture( call.departure );
        }
        if ( journey != null ) {
            if ( journey.operatingDay != null ) {
                callWithJourney.operatingDay = DateTimeFormatter.ISO_LOCAL_DATE.format( journey.operatingDay );
            }
            callWithJourney.transportMode = journey.transportMode;
            callWithJourney.serviceAlteration = journey.serviceAlteration;
            callWithJourney.operatorCode = journey.operatorCode;
            callWithJourney.operatorName = journey.operatorName;
            callWithJourney.operatorShortName = journey.operatorShortName;
            callWithJourney.lineCode = journey.lineCode;
            callWithJourney.lineName = journey.lineName;
            callWithJourney.lineShortName = journey.lineShortName;
            callWithJourney.directionType = journey.directionType;
        }
        callWithJourney.id = journey.id + "_" + journey.operatingDay + "_" + call.order;

        return callWithJourney;
    }

    protected ch.bernmobil.netex.importer.mongodb.dom.Call.Arrival arrivalToArrival(Call.Arrival arrival) {
        if ( arrival == null ) {
            return null;
        }

        ch.bernmobil.netex.importer.mongodb.dom.Call.Arrival arrival1 = new ch.bernmobil.netex.importer.mongodb.dom.Call.Arrival();

        arrival1.time = arrival.time;
        arrival1.forAlighting = arrival.forAlighting;
        arrival1.isFlexible = arrival.isFlexible;

        return arrival1;
    }

    protected ch.bernmobil.netex.importer.mongodb.dom.Call.Departure departureToDeparture(Call.Departure departure) {
        if ( departure == null ) {
            return null;
        }

        ch.bernmobil.netex.importer.mongodb.dom.Call.Departure departure1 = new ch.bernmobil.netex.importer.mongodb.dom.Call.Departure();

        departure1.time = departure.time;
        departure1.forBoarding = departure.forBoarding;
        departure1.isFlexible = departure.isFlexible;

        return departure1;
    }

    protected ch.bernmobil.netex.importer.mongodb.dom.Call callToCall(Call call) {
        if ( call == null ) {
            return null;
        }

        ch.bernmobil.netex.importer.mongodb.dom.Call call1 = new ch.bernmobil.netex.importer.mongodb.dom.Call();

        call1.id = call.id;
        call1.order = call.order;
        call1.requestStop = call.requestStop;
        call1.stopUse = call.stopUse;
        call1.stopPlaceCode = call.stopPlaceCode;
        call1.stopPlaceName = call.stopPlaceName;
        call1.stopPointName = call.stopPointName;
        call1.destinationDisplayName = call.destinationDisplayName;
        call1.arrival = arrivalToArrival( call.arrival );
        call1.departure = departureToDeparture( call.departure );

        return call1;
    }

    protected List<ch.bernmobil.netex.importer.mongodb.dom.Call> callListToCallList(List<Call> list) {
        if ( list == null ) {
            return null;
        }

        List<ch.bernmobil.netex.importer.mongodb.dom.Call> list1 = new ArrayList<ch.bernmobil.netex.importer.mongodb.dom.Call>( list.size() );
        for ( Call call : list ) {
            list1.add( callToCall( call ) );
        }

        return list1;
    }
}
