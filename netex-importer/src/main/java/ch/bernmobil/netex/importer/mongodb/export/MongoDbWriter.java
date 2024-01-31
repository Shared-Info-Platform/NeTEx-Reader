package ch.bernmobil.netex.importer.mongodb.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.BsonDocument;
import org.bson.BsonInt64;
import org.bson.BsonString;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;

import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.mongodb.dom.CallAggregation;
import ch.bernmobil.netex.importer.mongodb.dom.CallWithJourney;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyAggregation;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyWithCalls;
import ch.bernmobil.netex.importer.mongodb.mapper.AggregationMapper;
import ch.bernmobil.netex.importer.mongodb.mapper.JourneyMapper;

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
	}

	public void writeJourneys(List<Journey> journeys) {
		final List<JourneyWithCalls> mappedJourneys = journeys.stream().map(JourneyMapper.INSTANCE::mapJourney).toList();
		final List<CallWithJourney> mappedCalls = new ArrayList<>();
		for (final Journey journey : journeys) {
			mappedCalls.addAll(journey.calls.stream().map(call -> JourneyMapper.INSTANCE.mapCalls(call, journey)).toList());
		}
		journeyCollection.insertMany(mappedJourneys);
		callCollection.insertMany(mappedCalls);
	}

	public void writeJourneyAggregations(Collection<ch.bernmobil.netex.importer.journey.dom.JourneyAggregation> aggregations) {
		final List<JourneyAggregation> mappedAggregations = new ArrayList<>();
		for (final ch.bernmobil.netex.importer.journey.dom.JourneyAggregation aggregation : aggregations) {
			mappedAggregations.add(AggregationMapper.INSTANCE.mapJourneyAggregation(aggregation));
		}

		final List<UpdateOneModel<JourneyAggregation>> updates = new ArrayList<>();
		for (final JourneyAggregation journeyAggregation : mappedAggregations) {
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
		}
		journeyAggregationCollection.bulkWrite(updates);
	}

	public void writeCallAggregations(Collection<ch.bernmobil.netex.importer.journey.dom.CallAggregation> aggregations) {
		final List<CallAggregation> mappedAggregations = new ArrayList<>();
		for (final ch.bernmobil.netex.importer.journey.dom.CallAggregation aggregation : aggregations) {
			mappedAggregations.add(AggregationMapper.INSTANCE.mapCallAggregation(aggregation));
		}

		final List<UpdateOneModel<CallAggregation>> updates = new ArrayList<>();
		for (final CallAggregation callAggregation : mappedAggregations) {
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
		}
		callAggregationCollection.bulkWrite(updates);
	}

	public void close() {
		mongoClient.close();
	}
}
