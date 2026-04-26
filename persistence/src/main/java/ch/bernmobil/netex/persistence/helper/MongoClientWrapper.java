package ch.bernmobil.netex.persistence.helper;

import java.util.List;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.persistence.export.NetexRepository;
import ch.bernmobil.netex.persistence.search.Helper;

public class MongoClientWrapper {

	private final MongoClient mongoClient;

	public MongoClientWrapper(MongoClient mongoClient) {
		this.mongoClient = mongoClient;
	}

	public NetexRepository createNetexRepository(String databaseName) {
		return new NetexRepository(mongoClient, databaseName);
	}

	public List<String> listDatabaseNames() {
		return Helper.iterableToList(mongoClient.listDatabaseNames());
	}

	public void dropDatabase(String databaseName) {
		mongoClient.getDatabase(databaseName).drop();
	}
}
