package ch.bernmobil.netex.haltelog.properties;

import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging")
public class HaltelogProperties {

	private ElasticProperties elastic = new ElasticProperties();

	/**
	 * Enables/disables the export of Haltelog entries.
	 */
	private boolean writeHaltelog = false;

	/**
	 * How many calendar days in the future the haltelog is written. Zero means that the haltelog is only written for the current day.
	 */
	private int haltelogExportDaysInFuture;

	/**
	 * Defines the time of day when the haltelog is exported. It affects which version is exported as the "active" version for a day,
	 * depending on whether a new netex version was imported before or after this time. Only really makes a difference if
	 * {@link #haltelogExportDaysInFuture} is zero. Otherwise the haltelog is written for a day in the future and it will be deleted,
	 * if a new version becomes active later.
	 */
	private LocalTime haltelogExportTimeOfDay = LocalTime.of(12, 0);

	public ElasticProperties getElastic() {
		return elastic;
	}

	public void setElastic(ElasticProperties elastic) {
		this.elastic = elastic;
	}

	public boolean isWriteHaltelog() {
		return writeHaltelog;
	}

	public void setWriteHaltelog(boolean writeHaltelog) {
		this.writeHaltelog = writeHaltelog;
	}

	public int getHaltelogExportDaysInFuture() {
		return haltelogExportDaysInFuture;
	}

	public void setHaltelogExportDaysInFuture(int haltelogExportDaysInFuture) {
		this.haltelogExportDaysInFuture = haltelogExportDaysInFuture;
	}

	public LocalTime getHaltelogExportTimeOfDay() {
		return haltelogExportTimeOfDay;
	}

	public void setHaltelogExportTimeOfDay(LocalTime haltelogExportTimeOfDay) {
		this.haltelogExportTimeOfDay = haltelogExportTimeOfDay;
	}
}
