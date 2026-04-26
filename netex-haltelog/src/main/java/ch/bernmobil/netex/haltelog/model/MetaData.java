package ch.bernmobil.netex.haltelog.model;

import java.time.Instant;

/**
 * Note: Keep this in sync with MetaData in SIP-Hub.
 */
public class MetaData {

	private Instant logTime;
	private Service service;
	private Dataflow dataflow;
	private Dataview dataview;
	private String correlationId;

	public Instant getLogTime() {
		return logTime;
	}

	public void setLogTime(Instant logTime) {
		this.logTime = logTime;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Dataflow getDataflow() {
		return dataflow;
	}

	public void setDataflow(Dataflow dataflow) {
		this.dataflow = dataflow;
	}

	public Dataview getDataview() {
		return dataview;
	}

	public void setDataview(Dataview dataview) {
		this.dataview = dataview;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
}
