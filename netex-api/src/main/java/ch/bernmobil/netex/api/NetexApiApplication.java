package ch.bernmobil.netex.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info(description = "API to access data that was imported by the NeTEx-Reader", title = "NeTEx API", version = "1.0"))
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(NetexApiConfig.class)
public class NetexApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NetexApiApplication.class, args);
	}
}