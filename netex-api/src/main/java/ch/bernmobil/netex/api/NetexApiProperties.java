package ch.bernmobil.netex.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class NetexApiProperties {

	private String mongoConnectionString = "mongodb://localhost:27017/";
	private String databaseName = "netex";

	public String getMongoConnectionString() {
		return mongoConnectionString;
	}

	public void setMongoConnectionString(String mongoConnectionString) {
		this.mongoConnectionString = mongoConnectionString;
	}

	public String getDatabaseName() {
		return databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}
}
