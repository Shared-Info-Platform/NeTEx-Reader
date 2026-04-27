package ch.bernmobil.netex.persistence;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.TaskRepository;
import ch.bernmobil.netex.persistence.export.MongoDbClientHelper;
import ch.bernmobil.netex.persistence.export.NetexRepository;

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

	@Bean
	public TaskRepository taskRepository(MongoClient mongoClient) {
		return new TaskRepository(mongoClient, properties.getAdminDatabaseName());
	}

	@Bean
	public NetexRepository historyNetexRepository(MongoClient mongoClient) {
		return new NetexRepository(mongoClient, properties.getHistoryDatabaseName());
	}
}
