package ch.bernmobil.netex.application.scheduler;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.application.helper.FilesystemWrapper;
import ch.bernmobil.netex.application.helper.MongoClientWrapper;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.export.NetexRepository;

@Configuration
@EnableConfigurationProperties(ImportSchedulerProperties.class)
@EnableScheduling
public class ImportSchedulerConfig {

	private final ImportSchedulerProperties properties;

	public ImportSchedulerConfig(ImportSchedulerProperties properties) {
		this.properties = properties;
	}

	@Bean
	public ImportScheduler importScheduler(Downloader downloader, ImporterFactory importerFactory,
			ImportVersionRepository importVersionRepository, NetexRepository historyNetexRepository, MongoClientWrapper mongoClientWrapper,
			FilesystemWrapper filesystemWrapper, Clock clock) {
		return new ImportScheduler(properties, downloader, importerFactory, importVersionRepository, historyNetexRepository,
				mongoClientWrapper, filesystemWrapper, clock);
	}

	@Bean
	public Downloader downloader(FilesystemWrapper filesystemWrapper) {
		return new Downloader(properties.getTemporaryFilesDirectory(), filesystemWrapper);
	}

	@Bean
	public ImporterFactory importerFactory() {
		return new ImporterFactory();
	}

	@Bean
	public MongoClientWrapper mongoClientWrapper(MongoClient mongoClient) {
		return new MongoClientWrapper(mongoClient);
	}

	@Bean
	public FilesystemWrapper filesystemWrapper() {
		return new FilesystemWrapper();
	}

	@Bean
	public Clock clock() {
		return Clock.systemDefaultZone();
	}

	@Bean
	public CustomInfoContributor customInfoContributor(ImportVersionRepository importVersionRepository) {
		return new CustomInfoContributor(properties, importVersionRepository);
	}
}
