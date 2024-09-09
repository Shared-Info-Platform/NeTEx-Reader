package ch.bernmobil.netex.api;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;

import ch.bernmobil.netex.api.controller.RouteControllerV1;
import ch.bernmobil.netex.api.service.RouteService;
import ch.bernmobil.netex.persistence.export.MongoDbClientHelper;

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
	public RouteService createRouteService(MongoClient client) {
		return new RouteService(client, properties);
	}

	@Bean
	public MongoClient createMongoClient() {
		return MongoDbClientHelper.createClient(properties.getMongoConnectionString());
	}
}
