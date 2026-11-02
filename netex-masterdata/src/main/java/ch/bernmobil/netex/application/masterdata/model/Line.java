package ch.bernmobil.netex.application.masterdata.model;

public class Line {

	private String lineId;
	private String slnid;
	private String text;
	private String organisationNumber;
	private String regionNumber;

	public String getLineId() {
		return lineId;
	}

	public void setLineId(String lineId) {
		this.lineId = lineId;
	}

	public String getSlnid() {
		return slnid;
	}

	public void setSlnid(String slnid) {
		this.slnid = slnid;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getOrganisationNumber() {
		return organisationNumber;
	}

	public void setOrganisationNumber(String organisationNumber) {
		this.organisationNumber = organisationNumber;
	}

	public String getRegionNumber() {
		return regionNumber;
	}

	public void setRegionNumber(String regionNumber) {
		this.regionNumber = regionNumber;
	}
}
