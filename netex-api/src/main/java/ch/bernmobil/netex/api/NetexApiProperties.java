package ch.bernmobil.netex.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class NetexApiProperties {

	private String databaseName = "netex";

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
}
