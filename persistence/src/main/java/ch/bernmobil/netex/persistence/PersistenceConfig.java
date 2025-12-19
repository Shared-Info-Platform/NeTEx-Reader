package ch.bernmobil.netex.persistence;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.export.MongoDbClientHelper;

@Configuration
@EnableConfigurationProperties(PersistenceProperties.class)
public class PersistenceConfig {

	private final PersistenceProperties properties;

	public PersistenceConfig(PersistenceProperties properties) {
		this.properties = properties;
	}

	@Bean
	public MongoClient mongoClient() {
		return MongoDbClientHelper.createClient(properties.getMongoConnectionString());
	}

	@Bean
	public ImportVersionRepository importVersionRepository(MongoClient mongoClient) {
		return new ImportVersionRepository(mongoClient, properties.getAdminDatabaseName());
	}
}
