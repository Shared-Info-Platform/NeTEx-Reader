package ch.bernmobil.netex.persistence.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertManyOptions;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

import ch.bernmobil.netex.persistence.Constants;
import ch.bernmobil.netex.persistence.dom.CallAggregation;
import ch.bernmobil.netex.persistence.dom.CallWithJourney;
import ch.bernmobil.netex.persistence.dom.JourneyAggregation;
import ch.bernmobil.netex.persistence.dom.JourneyWithCalls;
import ch.bernmobil.netex.persistence.dom.RouteAggregation;
import ch.bernmobil.netex.persistence.dom.RouteAggregation.StopPlace;

/**
 * Opens collections in a MongoDB, creates indexes (if necessary) for these collections, transforms journeys
 * to the model needed by the different collections and writes them to the database.
 */
public class MongoDbWriter {

	private MongoClient mongoClient;
	private MongoCollection<JourneyWithCalls> journeyCollection;
	private MongoCollection<CallWithJourney> callCollection;
	private MongoCollection<JourneyAggregation> journeyAggregationCollection;
	private MongoCollection<CallAggregation> callAggregationCollection;
	private MongoCollection<RouteAggregation> routeAggregationCollection;

	public MongoDbWriter(String connectionString, String databaseName) {
		mongoClient = MongoDbClientHelper.createClient(connectionString);
		final MongoDatabase database = mongoClient.getDatabase(databaseName);

		// Journeys
		journeyCollection = database.getCollection("Journeys", JourneyWithCalls.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("calendarDay", 1);
			journeyCollection.createIndex(new Document(index));
		}
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("sjyid", 1);
			index.put("calendarDay", 1);
			journeyCollection.createIndex(new Document(index));
		}
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("operatorCode", 1);
			index.put("trainNumbers", 1);
			index.put("calendarDay", 1);
			journeyCollection.createIndex(new Document(index));
		}

		// Calls
		callCollection = database.getCollection("Calls", CallWithJourney.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("stopPlaceCode", 1);
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("calendarDay", 1);
			callCollection.createIndex(new Document(index));
		}
//		{
//			final Map<String, Integer> index = new LinkedHashMap<>();
//			index.put("stopPlaceCode", 1);
//			index.put("arrival.time", 1);
//			callCollection.createIndex(new Document(index));
//		}
//		{
//			final Map<String, Integer> index = new LinkedHashMap<>();
//			index.put("stopPlaceCode", 1);
//			index.put("departure.time", 1);
//			callCollection.createIndex(new Document(index));
//		}
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("sjyid", 1);
			index.put("calendarDay", 1);
			journeyCollection.createIndex(new Document(index));
		}

		// Journey Aggregations
		journeyAggregationCollection = database.getCollection("JourneyAggregations", JourneyAggregation.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("calendarDay", 1);
			journeyAggregationCollection.createIndex(new Document(index));
		}

		// Call Aggregations
		callAggregationCollection = database.getCollection("CallAggregations", CallAggregation.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("stopPlaceCode", 1);
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("calendarDay", 1);
			callAggregationCollection.createIndex(new Document(index));
		}
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("calendarDay", 1);
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("stopPlaceCode", 1);
			callAggregationCollection.createIndex(new Document(index));
		}

		// Route Aggregations
		routeAggregationCollection = database.getCollection("RouteAggregations", RouteAggregation.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put("operatorCode", 1);
			index.put("lineCode", 1);
			index.put("directionType", 1);
			index.put("calendarDay", 1);
			routeAggregationCollection.createIndex(new Document(index));
		}
	}

	public boolean isDatabaseEmpty() {
		final long numDocuments = journeyCollection.estimatedDocumentCount ()
				+ callCollection.estimatedDocumentCount ()
				+ journeyAggregationCollection.estimatedDocumentCount ()
				+ callAggregationCollection.estimatedDocumentCount ()
				+ routeAggregationCollection.estimatedDocumentCount ();
		return numDocuments == 0;
	}

	public void writeJourneys(final List<JourneyWithCalls> journeys) {
		final InsertManyOptions options = new InsertManyOptions().ordered(false);

		int numberOfCalls = 0;
		final List<JourneyWithCalls> batch = new ArrayList<>();
		for (final JourneyWithCalls journey : journeys) {
			batch.add(journey);
			numberOfCalls += journey.calls.size();

			// insert frequently before batch becomes too large.
			if (numberOfCalls >= Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE) {
				journeyCollection.insertMany(batch, options);
				batch.clear();
				numberOfCalls = 0;
			}
		}
		// insert the rest of the documents if there are any
		if (batch.size() > 0) {
			journeyCollection.insertMany(batch, options);
		}
	}

	public void writeCalls(final List<CallWithJourney> calls) {
		final InsertManyOptions options = new InsertManyOptions().ordered(false);

		final List<CallWithJourney> batch = new ArrayList<>();
		for (final CallWithJourney call : calls) {
			batch.add(call);

			// insert frequently before batch becomes too large.
			if (batch.size() >= Constants.MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE) {
				callCollection.insertMany(batch, options);
				batch.clear();
			}
		}
		// insert the rest of the documents if there are any
		if (batch.size() > 0) {
			callCollection.insertMany(batch, options);
		}
	}

	public void writeJourneyAggregations(Collection<JourneyAggregation> aggregations) {
		final List<UpdateOneModel<JourneyAggregation>> updates = new ArrayList<>();
		for (final JourneyAggregation journeyAggregation : aggregations) {
			final BsonString id = new BsonString(journeyAggregation.getId());
			final BsonDocument filter = new BsonDocument("_id", id);
			final BsonDocument set = new BsonDocument();
			set.put("_id", id);
			set.put("calendarDay", new BsonString(journeyAggregation.calendarDay));
			set.put("operatorCode", new BsonString(journeyAggregation.operatorCode));
			set.put("lineCode", new BsonString(journeyAggregation.lineCode));
			final BsonDocument update = new BsonDocument();
			update.put("$set", set);
			update.put("$inc", new BsonDocument("journeys", new BsonInt64(journeyAggregation.journeys)));
			final UpdateOptions options = new UpdateOptions();
			options.upsert(true);
			updates.add(new UpdateOneModel<JourneyAggregation>(filter, update, options));

			// insert frequently before batch becomes too large
			if (updates.size() >= Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE) {
				journeyAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
				updates.clear();
			}
		}
		// insert the rest of the documents if there are any
		if (updates.size() > 0) {
			journeyAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
		}
	}

	public void writeCallAggregations(Collection<CallAggregation> aggregations) {
		final List<UpdateOneModel<CallAggregation>> updates = new ArrayList<>();
		for (final CallAggregation callAggregation : aggregations) {
			final BsonString id = new BsonString(callAggregation.getId());
			final BsonDocument filter = new BsonDocument("_id", id);
			final BsonDocument set = new BsonDocument();
			set.put("_id", id);
			set.put("calendarDay", new BsonString(callAggregation.calendarDay));
			set.put("stopPlaceCode", new BsonString(callAggregation.stopPlaceCode));
			set.put("operatorCode", new BsonString(callAggregation.operatorCode));
			set.put("lineCode", new BsonString(callAggregation.lineCode));
			final BsonDocument update = new BsonDocument();
			update.put("$set", set);
			update.put("$inc", new BsonDocument("calls", new BsonInt64(callAggregation.calls)));
			final UpdateOptions options = new UpdateOptions();
			options.upsert(true);
			updates.add(new UpdateOneModel<CallAggregation>(filter, update, options));

			// insert frequently before batch becomes too large
			if (updates.size() >= Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE) {
				callAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
				updates.clear();
			}
		}
		// insert the rest of the documents if there are any
		if (updates.size() > 0) {
			callAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
		}
	}

	public void writeRouteAggregations(Collection<RouteAggregation> aggregations) {
		final List<UpdateOneModel<RouteAggregation>> updates = new ArrayList<>();
		for (final RouteAggregation routeAggregation : aggregations) {
			final BsonString id = new BsonString(routeAggregation.getId());
			final BsonDocument filter = new BsonDocument("_id", id);
			final BsonDocument set = new BsonDocument();
			set.put("_id", id);
			set.put("calendarDay", new BsonString(routeAggregation.calendarDay));
			set.put("operatorCode", new BsonString(routeAggregation.operatorCode));
			set.put("lineCode", new BsonString(routeAggregation.lineCode));
			set.put("directionType", new BsonString(routeAggregation.directionType));
			final List<BsonDocument> stopPlaces = routeAggregation.stopPlaces.stream().map(this::createStopDocument).toList();
			set.put("stopPlaces", new BsonArray(stopPlaces));
			final BsonDocument update = new BsonDocument();
			update.put("$set", set);
			update.put("$inc", new BsonDocument("journeys", new BsonInt64(routeAggregation.journeys)));
			final UpdateOptions options = new UpdateOptions();
			options.upsert(true);
			updates.add(new UpdateOneModel<RouteAggregation>(filter, update, options));

			// insert frequently before batch becomes too large
			if (updates.size() >= Constants.MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE) {
				routeAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
				updates.clear();
			}
		}
		// insert the rest of the documents if there are any
		if (updates.size() > 0) {
			routeAggregationCollection.bulkWrite(updates, new BulkWriteOptions().ordered(false));
		}
	}

	private BsonDocument createStopDocument(StopPlace stopPlace) {
		final BsonDocument result = new BsonDocument();
		result.put("code", new BsonString(stopPlace.code()));
		result.put("name", new BsonString(stopPlace.name()));
		return result;
	}

	public void close() {
		mongoClient.close();
	}
}
