package ch.bernmobil.netex.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class NetexApiProperties {

	private String apiDatabaseName;

	public String getApiDatabaseName() {
		return apiDatabaseName;
	}

	public void setApiDatabaseName(String apiDatabaseName) {
		this.apiDatabaseName = apiDatabaseName;
	}
}
