package ch.bernmobil.netex.persistence.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.persistence.model.RouteAggregation;

public class RouteAggregationRepository {

	private MongoCollection<RouteAggregation> collection;

	public RouteAggregationRepository(MongoClient client, String databaseName) {
		collection = client.getDatabase(databaseName).getCollection("RouteAggregations", RouteAggregation.class);
	}

	public List<RouteAggregation> findRouteAggregations(String operatorCode, String lineCode, Optional<String> regionCode, Collection<String> directionTypes, Collection<String> calendarDays) {
		final List<Bson> filters = new ArrayList<>();
		filters.add(Filters.eq(RouteAggregation.FIELDNAME_OPERATOR_CODE, operatorCode));
		filters.add(Filters.eq(RouteAggregation.FIELDNAME_LINE_CODE, lineCode));
		filters.add(Filters.in(RouteAggregation.FIELDNAME_DIRECTION_TYPE, directionTypes));
		filters.add(Filters.in(RouteAggregation.FIELDNAME_CALENDAR_DAY, calendarDays));
		regionCode.ifPresent(code -> filters.add(Filters.eq(RouteAggregation.FIELDNAME_REGION_CODE, code)));
		final Bson filter = Filters.and(filters);
		return Helper.iterableToList(collection.find(filter));
	}
}
