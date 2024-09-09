package ch.bernmobil.netex.persistence.search;

import java.util.Collection;
import java.util.List;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.persistence.dom.RouteAggregation;

public class RouteAggregationRepository {

	private MongoCollection<RouteAggregation> collection;

	public RouteAggregationRepository(MongoClient client, String databaseName) {
		collection = client.getDatabase(databaseName).getCollection("RouteAggregations", RouteAggregation.class);
	}

	public List<RouteAggregation> findRouteAggregations(String operatorCode, String lineCode, Collection<String> directionTypes, Collection<String> calendarDays) {
		final Bson filter = Filters.and(Filters.eq(RouteAggregation.FIELDNAME_OPERATOR_CODE, operatorCode),
										Filters.eq(RouteAggregation.FIELDNAME_LINE_CODE, lineCode),
										Filters.in(RouteAggregation.FIELDNAME_DIRECTION_TYPE, directionTypes),
										Filters.in(RouteAggregation.FIELDNAME_CALENDAR_DAY, calendarDays));
		return Helper.iterableToList(collection.find(filter));
	}
}
