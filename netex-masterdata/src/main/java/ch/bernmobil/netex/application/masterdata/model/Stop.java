package ch.bernmobil.netex.application.masterdata.model;

import java.util.ArrayList;
import java.util.List;


public class Stop {

	private String sloid;
	private String didok;
	private List<StopPoint> stopPoints = new ArrayList<>();

	public String getSloid() {
		return sloid;
	}

	public void setSloid(String sloid) {
		this.sloid = sloid;
	}

	public String getDidok() {
		return didok;
	}

	public void setDidok(String didok) {
		this.didok = didok;
	}

	public List<StopPoint> getStopPoints() {
		return stopPoints;
	}

	public void setStopPoints(List<StopPoint> stopPoints) {
		this.stopPoints = stopPoints;
	}
}
