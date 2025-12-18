package ch.bernmobil.netex.persistence;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class PersistenceProperties {

	private String mongoConnectionString = "mongodb://localhost:27017/";

	public String getMongoConnectionString() {
		return mongoConnectionString;
	}

	public void setMongoConnectionString(String mongoConnectionString) {
		this.mongoConnectionString = mongoConnectionString;
	}
}
