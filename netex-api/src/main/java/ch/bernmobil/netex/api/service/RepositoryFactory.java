package ch.bernmobil.netex.api.service;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.persistence.search.Helper;
import ch.bernmobil.netex.persistence.search.RouteAggregationRepository;

public class RepositoryFactory {

	private final MongoClient client;

	public RepositoryFactory(MongoClient client) {
		this.client = client;
	}

	public RouteAggregationRepository createRepository(String databaseName) {
		if (Helper.doesDatabaseExist(client, databaseName)) {
			return new RouteAggregationRepository(client, databaseName);
		} else {
			return null;
		}
	}
}
