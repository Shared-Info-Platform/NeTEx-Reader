package ch.bernmobil.netex.application.masterdata;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableConfigurationProperties(MasterdataImporterProperties.class)
@EnableScheduling
public class MasterdataImporterConfig {

	@Bean
	public MasterdataImporter masterdataImporter(BernmobilStopFetcher stopFetcher, BernmobilLineFetcher lineFetcher, BernmobilOperatorFetcher operatorFetcher,
			StopRepository stopRepository, LineRepository lineRepository, OperatorRepository operatorRepository) {
		return new MasterdataImporter(stopFetcher, lineFetcher, operatorFetcher, stopRepository, lineRepository, operatorRepository);
	}

	@Bean
	public BernmobilStopFetcher bernmobilStopFetcher(MasterdataImporterProperties properties) {
		return new BernmobilStopFetcher(properties.getStopsUrl(), properties.getStopsPerRequest());
	}

	@Bean
	public BernmobilLineFetcher bernmobilLineFetcher(MasterdataImporterProperties properties) {
		return new BernmobilLineFetcher(properties.getLinesUrl(), properties.getLinesPerRequest());
	}

	@Bean
	public BernmobilOperatorFetcher bernmobilOperatorFetcher(MasterdataImporterProperties properties) {
		return new BernmobilOperatorFetcher(properties.getOperatorsUrl(), properties.getOperatorsPerRequest());
	}

	@Bean
	public StopRepository stopRepository() {
		return new StopRepository();
	}

	@Bean
	public LineRepository lineRepository() {
		return new LineRepository();
	}

	@Bean
	public OperatorRepository operatorRepository() {
		return new OperatorRepository();
	}
}
