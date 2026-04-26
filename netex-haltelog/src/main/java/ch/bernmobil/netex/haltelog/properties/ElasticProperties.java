package ch.bernmobil.netex.haltelog.properties;

import java.time.Duration;
import java.util.List;

/**
 * Note: Keep this in sync with ElasticProperties in SIP-Hub.
 */
public class ElasticProperties {

	private List<String> hosts = List.of("localhost:9200");
	private SecurityProperties security = new SecurityProperties();
	private boolean sniffingEnabled = false;
	private String halteLogIndexName = "haltelog";
	private String halteLogIndexPattern = "haltelog-*";
	private Duration batchFlushInterval = Duration.ofSeconds(1);
	private int batchMaxCount = 1000;
	private long batchMaxSizeBytes = 5 * 1024 * 1024;

	public List<String> getHosts() {
		return hosts;
	}

	public void setHosts(List<String> hosts) {
		this.hosts = hosts;
	}

	public SecurityProperties getSecurity() {
		return security;
	}

	public void setSecurity(SecurityProperties security) {
		this.security = security;
	}

	public boolean isSniffingEnabled() {
		return sniffingEnabled;
	}

	public void setSniffingEnabled(boolean sniffingEnabled) {
		this.sniffingEnabled = sniffingEnabled;
	}

	public String getHalteLogIndexName() {
		return halteLogIndexName;
	}

	public void setHalteLogIndexName(String halteLogIndexName) {
		this.halteLogIndexName = halteLogIndexName;
	}

	public Duration getBatchFlushInterval() {
		return batchFlushInterval;
	}

	public void setBatchFlushInterval(Duration batchFlushIntervalMillis) {
		this.batchFlushInterval = batchFlushIntervalMillis;
	}

	public int getBatchMaxCount() {
		return batchMaxCount;
	}

	public void setBatchMaxCount(int batchMaxCount) {
		this.batchMaxCount = batchMaxCount;
	}

	public long getBatchMaxSizeBytes() {
		return batchMaxSizeBytes;
	}

	public void setBatchMaxSizeBytes(long batchMaxSizeBytes) {
		this.batchMaxSizeBytes = batchMaxSizeBytes;
	}

	public String getHalteLogIndexPattern() {
		return halteLogIndexPattern;
	}

	public void setHalteLogIndexPattern(String halteLogIndexPattern) {
		this.halteLogIndexPattern = halteLogIndexPattern;
	}
}
