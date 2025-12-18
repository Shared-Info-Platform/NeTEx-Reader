package ch.bernmobil.netex.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Import;

import ch.bernmobil.netex.api.NetexApiConfig;
import ch.bernmobil.netex.application.cli.Cli;
import ch.bernmobil.netex.persistence.PersistenceConfig;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import picocli.CommandLine;

@OpenAPIDefinition(info = @Info(description = "API to access data that was imported by the NeTEx-Reader", title = "NeTEx API", version = "1.0"))
@SpringBootConfiguration
@EnableAutoConfiguration
@Import({ NetexApiConfig.class, PersistenceConfig.class })
public class NetexApplication {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			// Run as spring boot application if there are no command line arguments
			SpringApplication.run(NetexApplication.class, args);
		} else {
			// Run as command-line utility to import NETEX data if command line arguments are present
			new CommandLine(new Cli()).execute(args);
		}
	}
}