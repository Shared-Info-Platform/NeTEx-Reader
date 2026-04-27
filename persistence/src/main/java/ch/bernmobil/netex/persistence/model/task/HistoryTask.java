package ch.bernmobil.netex.persistence.model.task;

import java.time.LocalDate;

public class HistoryTask extends Task {

	public static final String TASK_ID = "history-task";

	private LocalDate historyExportedUntil;

	public HistoryTask() {
		super(TASK_ID);
	}

	public LocalDate getHistoryExportedUntil() {
		return historyExportedUntil;
	}

	public void setHistoryExportedUntil(LocalDate historyExportedUntil) {
		this.historyExportedUntil = historyExportedUntil;
	}
}
