package ch.bernmobil.netex.persistence.export;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDbClientHelper {

	public static MongoClient createClient(String connectionString) {
		final ConnectionString connection = new ConnectionString(connectionString);
		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		final MongoClientSettings clientSettings = MongoClientSettings.builder()
				.applyConnectionString(connection)
				.codecRegistry(codecRegistry)
				.build();
		return MongoClients.create(clientSettings);
	}
}
