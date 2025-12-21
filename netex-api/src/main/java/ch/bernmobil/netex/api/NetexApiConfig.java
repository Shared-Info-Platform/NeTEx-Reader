package ch.bernmobil.netex.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.api.controller.RouteControllerV1;
import ch.bernmobil.netex.api.service.RepositoryFactory;
import ch.bernmobil.netex.api.service.RouteService;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;

@Configuration
@EnableConfigurationProperties(NetexApiProperties.class)
public class NetexApiConfig {

	private final NetexApiProperties properties;

	public NetexApiConfig(NetexApiProperties properties) {
		this.properties = properties;
	}

	@Bean
	public RouteControllerV1 createControllerV1(RouteService routeService) {
		return new RouteControllerV1(routeService);
	}

	@Bean
	public RouteService createRouteService(RepositoryFactory repositoryFactory, ImportVersionRepository importVersionRepository) {
		return new RouteService(properties, repositoryFactory, importVersionRepository);
	}

	@Bean
	public RepositoryFactory createRepositoryFactory(MongoClient client) {
		return new RepositoryFactory(client);
	}
}
