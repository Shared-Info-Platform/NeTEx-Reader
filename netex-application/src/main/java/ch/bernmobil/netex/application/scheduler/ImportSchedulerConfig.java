package ch.bernmobil.netex.application.scheduler;

import java.time.Clock;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;

@Configuration
@EnableConfigurationProperties(ImportSchedulerProperties.class)
@EnableScheduling
public class ImportSchedulerConfig {

	private final ImportSchedulerProperties properties;

	public ImportSchedulerConfig(ImportSchedulerProperties properties) {
		this.properties = properties;
	}

	@Bean
	public ImportScheduler importScheduler(Downloader downloader, ImportVersionRepository importVersionRepository, MongoClient mongoClient,
			Clock clock) {
		return new ImportScheduler(properties, downloader, importVersionRepository, mongoClient, clock);
	}

	@Bean
	public Downloader downloader() {
		return new Downloader(properties.getTemporaryFilesDirectory());
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}
}
