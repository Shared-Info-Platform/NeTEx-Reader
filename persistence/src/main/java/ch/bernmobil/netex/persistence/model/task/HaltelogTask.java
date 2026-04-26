package ch.bernmobil.netex.persistence.model.task;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class HaltelogTask extends Task {

	public static final String TASK_ID = "haltelog-task";

	private LocalDate haltelogExportedUntil;
	private Set<String> lastExportedVersions = new HashSet<>();

	public HaltelogTask() {
		super(TASK_ID);
	}

	public LocalDate getHaltelogExportedUntil() {
		return haltelogExportedUntil;
	}

	public void setHaltelogExportedUntil(LocalDate haltelogExportedUntil) {
		this.haltelogExportedUntil = haltelogExportedUntil;
	}

	public Set<String> getLastExportedVersions() {
		return lastExportedVersions;
	}

	public void setLastExportedVersions(Set<String> lastExportedVersions) {
		this.lastExportedVersions = lastExportedVersions;
	}
}
