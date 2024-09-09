package ch.bernmobil.netex.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration
@Import(NetexApiConfig.class)
public class NetexApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NetexApiApplication.class, args);
	}
}