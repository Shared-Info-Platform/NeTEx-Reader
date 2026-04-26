package ch.bernmobil.netex.haltelog;

import java.io.IOException;
import java.time.Clock;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.bernmobil.netex.haltelog.properties.HaltelogProperties;
import ch.bernmobil.netex.haltelog.repository.ElasticSingletons;
import ch.bernmobil.netex.haltelog.repository.HaltelogRepository;
import ch.bernmobil.netex.haltelog.writer.HaltelogWriter;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.admin.TaskRepository;
import ch.bernmobil.netex.persistence.helper.MongoClientWrapper;

@Configuration
@EnableConfigurationProperties(HaltelogProperties.class)
@EnableAutoConfiguration(exclude = {ElasticsearchRestClientAutoConfiguration.class})
public class HaltelogConfig {

	@Bean
	public HaltelogWriter haltelogWriter(HaltelogProperties properties, HaltelogRepository haltelogRepository, TaskRepository taskRepository,
			ImportVersionRepository importVersionRepository, MongoClientWrapper mongoClientWrapper, Clock clock) {
		return new HaltelogWriter(properties, haltelogRepository, taskRepository, importVersionRepository, mongoClientWrapper, clock);
	}

	@Bean
	public HaltelogRepository haltelogRepository(ElasticSingletons elasticSingletons, HaltelogProperties loggingProperties)
			throws IOException {
		return new HaltelogRepository(elasticSingletons.getClient(), elasticSingletons.getBulkIngester(), loggingProperties);
	}

	@Bean
	public ElasticSingletons elasticSingletons(HaltelogProperties loggingProperties) {
		return new ElasticSingletons(loggingProperties.getElastic());
	}
}
