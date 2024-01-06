package ch.bernmobil.netex.importer.mongodb.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import ch.bernmobil.netex.importer.journey.dom.Journey;
import ch.bernmobil.netex.importer.mongodb.dom.CallWithJourney;
import ch.bernmobil.netex.importer.mongodb.dom.JourneyWithCalls;
import ch.bernmobil.netex.importer.mongodb.mapper.JourneyMapper;

public class MongoDbWriter {

	private MongoClient mongoClient;
	private MongoCollection<JourneyWithCalls> journeyCollection;
	private MongoCollection<CallWithJourney> callCollection;

	public MongoDbWriter() {
		mongoClient = MongoDbClientHelper.createClient();
		final MongoDatabase database = mongoClient.getDatabase("netex");

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

	public void close() {
		mongoClient.close();
	}
}
