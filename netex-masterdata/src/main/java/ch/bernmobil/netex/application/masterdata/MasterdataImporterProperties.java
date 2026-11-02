package ch.bernmobil.netex.application.masterdata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "masterdata")
public class MasterdataImporterProperties {

	/**
	 * A cron expression that triggers the periodic masterdata import job.
	 */
	private String importCronExpression; // default defined directly in @Scheduled in MasterdataImporter

	private String stopsUrl;
	private String linesUrl;
	private String operatorsUrl;

	private int stopsPerRequest = 1000;
	private int linesPerRequest = 1000;
	private int operatorsPerRequest = 1000;

	public String getImportCronExpression() {
		return importCronExpression;
	}

	public void setImportCronExpression(String importCronExpression) {
		this.importCronExpression = importCronExpression;
	}

	public String getStopsUrl() {
		return stopsUrl;
	}

	public void setStopsUrl(String stopsUrl) {
		this.stopsUrl = stopsUrl;
	}

	public String getLinesUrl() {
		return linesUrl;
	}

	public void setLinesUrl(String linesUrl) {
		this.linesUrl = linesUrl;
	}

	public String getOperatorsUrl() {
		return operatorsUrl;
	}

	public void setOperatorsUrl(String operatorsUrl) {
		this.operatorsUrl = operatorsUrl;
	}

	public int getStopsPerRequest() {
		return stopsPerRequest;
	}

	public void setStopsPerRequest(int stopsPerRequest) {
		this.stopsPerRequest = stopsPerRequest;
	}

	public int getLinesPerRequest() {
		return linesPerRequest;
	}

	public void setLinesPerRequest(int linesPerRequest) {
		this.linesPerRequest = linesPerRequest;
	}

	public int getOperatorsPerRequest() {
		return operatorsPerRequest;
	}

	public void setOperatorsPerRequest(int operatorsPerRequest) {
		this.operatorsPerRequest = operatorsPerRequest;
	}
}
