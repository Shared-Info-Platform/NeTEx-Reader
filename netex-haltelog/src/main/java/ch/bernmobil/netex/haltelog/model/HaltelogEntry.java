package ch.bernmobil.netex.haltelog.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Note: Keep this in sync with HalteLogEntry in SIP-Hub.
 */
public class HaltelogEntry {

	// Metadata
	@JsonUnwrapped
	private MetaData metaData;

	// Content
	private PositionInTrip positionInTrip;
	private Integer haltIdx;
	private String fahrtBezeichner;
	private LocalDate betriebstag;
	private LocalDate kalendertag;
	private String haltId;
	private String betreiberId;
	private String linienId;
	private String richtungsId;
	private Instant abfahrtszeitPlan;
	private Instant ankunftszeitPlan;
	private String haltePositionsText;
	private String linienText;
	private String richtungsText;
	private String verkehrsmittelText;
	private FahrtStatus fahrtStatus;
	private String haltestellenName;
	private Boolean einsteigeVerbot;
	private Boolean aussteigeVerbot;
	private Boolean durchfahrt;
	private String zielHst;
	private List<ServiceAttribut> serviceAttribute;
	private String produktId;

	public MetaData getMetaData() {
		return metaData;
	}

	public void setMetaData(MetaData metaData) {
		this.metaData = metaData;
	}

	public PositionInTrip getPositionInTrip() {
		return positionInTrip;
	}

	public void setPositionInTrip(PositionInTrip positionInTrip) {
		this.positionInTrip = positionInTrip;
	}

	public Integer getHaltIdx() {
		return haltIdx;
	}

	public void setHaltIdx(Integer haltIdx) {
		this.haltIdx = haltIdx;
	}

	public String getFahrtBezeichner() {
		return fahrtBezeichner;
	}

	public void setFahrtBezeichner(String fahrtBezeichner) {
		this.fahrtBezeichner = fahrtBezeichner;
	}

	public LocalDate getBetriebstag() {
		return betriebstag;
	}

	public void setBetriebstag(LocalDate betriebstag) {
		this.betriebstag = betriebstag;
	}

	public LocalDate getKalendertag() {
		return kalendertag;
	}

	public void setKalendertag(LocalDate kalendertag) {
		this.kalendertag = kalendertag;
	}

	public String getHaltId() {
		return haltId;
	}

	public void setHaltId(String haltId) {
		this.haltId = haltId;
	}

	public String getBetreiberId() {
		return betreiberId;
	}

	public void setBetreiberId(String betreiberId) {
		this.betreiberId = betreiberId;
	}

	public String getLinienId() {
		return linienId;
	}

	public void setLinienId(String linienId) {
		this.linienId = linienId;
	}

	public String getRichtungsId() {
		return richtungsId;
	}

	public void setRichtungsId(String richtungsId) {
		this.richtungsId = richtungsId;
	}

	public Instant getAbfahrtszeitPlan() {
		return abfahrtszeitPlan;
	}

	public void setAbfahrtszeitPlan(Instant abfahrtszeitPlan) {
		this.abfahrtszeitPlan = abfahrtszeitPlan;
	}

	public Instant getAnkunftszeitPlan() {
		return ankunftszeitPlan;
	}

	public void setAnkunftszeitPlan(Instant ankunftszeitPlan) {
		this.ankunftszeitPlan = ankunftszeitPlan;
	}

	public String getHaltePositionsText() {
		return haltePositionsText;
	}

	public void setHaltePositionsText(String haltePositionsText) {
		this.haltePositionsText = haltePositionsText;
	}

	public String getLinienText() {
		return linienText;
	}

	public void setLinienText(String linienText) {
		this.linienText = linienText;
	}

	public String getRichtungsText() {
		return richtungsText;
	}

	public void setRichtungsText(String richtungsText) {
		this.richtungsText = richtungsText;
	}

	public String getVerkehrsmittelText() {
		return verkehrsmittelText;
	}

	public void setVerkehrsmittelText(String verkehrsmittelText) {
		this.verkehrsmittelText = verkehrsmittelText;
	}

	public FahrtStatus getFahrtStatus() {
		return fahrtStatus;
	}

	public void setFahrtStatus(FahrtStatus fahrtStatus) {
		this.fahrtStatus = fahrtStatus;
	}

	public String getHaltestellenName() {
		return haltestellenName;
	}

	public void setHaltestellenName(String haltestellenName) {
		this.haltestellenName = haltestellenName;
	}

	public Boolean getEinsteigeVerbot() {
		return einsteigeVerbot;
	}

	public void setEinsteigeVerbot(Boolean einsteigeVerbot) {
		this.einsteigeVerbot = einsteigeVerbot;
	}

	public Boolean getAussteigeVerbot() {
		return aussteigeVerbot;
	}

	public void setAussteigeVerbot(Boolean aussteigeVerbot) {
		this.aussteigeVerbot = aussteigeVerbot;
	}

	public Boolean getDurchfahrt() {
		return durchfahrt;
	}

	public void setDurchfahrt(Boolean durchfahrt) {
		this.durchfahrt = durchfahrt;
	}

	public String getZielHst() {
		return zielHst;
	}

	public void setZielHst(String zielHst) {
		this.zielHst = zielHst;
	}

	public void setServiceAttribute(List<ServiceAttribut> serviceAttribute) {
		this.serviceAttribute = serviceAttribute;
	}

	public List<ServiceAttribut> getServiceAttribute() {
		return serviceAttribute;
	}

	public String getProduktId() {
		return produktId;
	}

	public void setProduktId(String produktId) {
		this.produktId = produktId;
	}
}
