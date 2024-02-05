package ch.bernmobil.netex.importer.mongodb.mapper;

import ch.bernmobil.netex.importer.journey.dom.Call;
import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.mongodb.dom.CallWithJourney;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyWithCalls;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2024-02-05T10:06:05+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.37.0.v20240103-0614, environment: Java 17.0.9 (Eclipse Adoptium)"
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
        if ( journey.getCalendarDay() != null ) {
            journeyWithCalls.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( journey.getCalendarDay() );
        }
        journeyWithCalls.transportMode = journey.transportMode;
        journeyWithCalls.serviceAlteration = journey.serviceAlteration;
        journeyWithCalls.vehicleType = journey.vehicleType;
        journeyWithCalls.productCategoryName = journey.productCategoryName;
        journeyWithCalls.productCategoryCode = journey.productCategoryCode;
        List<String> list = journey.serviceFacilities;
        if ( list != null ) {
            journeyWithCalls.serviceFacilities = new ArrayList<String>( list );
        }
        Map<String, String> map = journey.notices;
        if ( map != null ) {
            journeyWithCalls.notices = new LinkedHashMap<String, String>( map );
        }
        journeyWithCalls.operatorCode = journey.operatorCode;
        journeyWithCalls.operatorName = journey.operatorName;
        journeyWithCalls.operatorShortName = journey.operatorShortName;
        journeyWithCalls.lineCode = journey.lineCode;
        journeyWithCalls.lineName = journey.lineName;
        journeyWithCalls.lineShortName = journey.lineShortName;
        journeyWithCalls.directionType = journey.directionType;
        journeyWithCalls.calls = callListToCallList( journey.calls );

        journeyWithCalls.id = journey.id + "_" + journey.operatingDay;
        journeyWithCalls.departureTime = journey.calls.get(0).departure.time;
        journeyWithCalls.departureStopPlaceCode = journey.calls.get(0).stopPlaceCode;
        journeyWithCalls.arrivalTime = journey.calls.get(journey.calls.size() - 1).arrival.time;
        journeyWithCalls.arrivalStopPlaceCode = journey.calls.get(journey.calls.size() - 1).stopPlaceCode;

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
            if ( call.getCalendarDay() != null ) {
                callWithJourney.calendarDay = DateTimeFormatter.ISO_LOCAL_DATE.format( call.getCalendarDay() );
            }
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
            callWithJourney.vehicleType = journey.vehicleType;
            callWithJourney.productCategoryName = journey.productCategoryName;
            callWithJourney.productCategoryCode = journey.productCategoryCode;
            List<String> list = journey.serviceFacilities;
            if ( list != null ) {
                callWithJourney.serviceFacilities = new ArrayList<String>( list );
            }
            Map<String, String> map = journey.notices;
            if ( map != null ) {
                callWithJourney.notices = new LinkedHashMap<String, String>( map );
            }
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
