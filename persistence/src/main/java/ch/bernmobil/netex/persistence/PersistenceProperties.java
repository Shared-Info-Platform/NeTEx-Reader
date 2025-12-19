package ch.bernmobil.netex.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class PersistenceProperties {

	private String mongoConnectionString = "mongodb://localhost:27017/";
	private String adminDatabaseName = "netex-admin";

	public String getMongoConnectionString() {
		return mongoConnectionString;
	}

	public void setMongoConnectionString(String mongoConnectionString) {
		this.mongoConnectionString = mongoConnectionString;
	}

	public String getAdminDatabaseName() {
		return adminDatabaseName;
	}

	public void setAdminDatabaseName(String adminDatabaseName) {
		this.adminDatabaseName = adminDatabaseName;
	}
}
