package ch.bernmobil.netex.application.scheduler;

import java.io.File;
import java.net.URI;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties
public class ImportSchedulerProperties {

	/**
	 * A cron expression that triggers the periodic download & import job
	 */
	private String importCronExpression; // default defined directly in @Scheduled in ImportScheduler

	/**
	 * If true then the import job is run at startup, independently of the cron expression. Attention: This blocks the startup until the job
	 * is done, therefore the API of the application is not accessible until the import job is finished.
	 */
	private boolean runImportAtStartup = false;

	/**
	 * Contains an URI per timetable. Timetable is e.g. 2026, 2027, etc. Normally this map will only include an entry for the current
	 * timetable, but when timetables change, there can be two for a while.
	 */
	private Map<String, URI> uriPerTimetable = new HashMap<>();

	/**
	 * How many calendar days before the current day should be imported. Should be more than the equivalent setting in the service that
	 * reads netex data for the departures service.
	 */
	private int importDaysInPast = 3;

	/**
	 * How many calendar days after the current day should be imported. Should be more than the equivalent setting in the service that reads
	 * netex data for the departures service.
	 */
	private int importDaysInFuture = 10;

	/**
	 * The departures service only needs data from the Journeys collection, so with this property the export of data into the Calls
	 * collection can be suppressed.
	 */
	private boolean writeCallsCollection = false;

	/**
	 * The prefix that will be used for databases that contain netex data. It should be a distinct prefix that is not used by any other
	 * database in the system because the prefix will also be used to find and delete unreferenced leftover databases.
	 */
	private String importDatabasePrefix = "netex-autoimport";

	/**
	 * The directory where zip files with netex data will be downloaded and extracted to. It should be a directory that contains no other
	 * files or directories because files/directories that are not referenced by any known import version will be deleted periodically.
	 */
	private File temporaryFilesDirectory = new File("tmp");

	/**
	 * If there are more import versions for one timetable than the number specified in this property then they are deleted (except when
	 * they are forcibly active or marked to be kept).
	 */
	private int maxVersionsToKeep = 3;

	/**
	 * When a new version is imported then it is validated by comparing it to the previous version. For each imported day the number of
	 * journeys in the database is evaluated, both for the new and the previous version. Then the ratio between the difference and the
	 * average of these two numbers is calculated. The new version is valid if the ratio is smaller than the limit defined by this property.
	 *
	 * Example: Previous version 100'000 journeys, new version 80'000 journeys => ratio = 20'000 / 90'000 = 0.2222... (i.e. roughly 22%), so
	 * with a maxRelativeDifference of 0.25 this would still be valid but with a maxRelativeDifference of 0.20 it would be invalid.
	 */
	private float maxRelativeDifference = 0.1f;

	/**
	 * Whether databases and files/directories that are not referenced by any known import version should be deleted (see comments to
	 * {@link #importDatabasePrefix} and {@link #temporaryFilesDirectory}).
	 */
	private boolean deleteUnknownResources = true;

	/**
	 * Defines the number of days that should be stored in the history database.
	 */
	private int historyNumberOfDays = 30;

	/**
	 * Defines the time of day when the history is exported. It affects which version is exported as the "active" version for a day,
	 * depending on whether a new netex version was imported before or after this time.
	 */
	private LocalTime historyExportTimeOfDay = LocalTime.of(12, 0);

	public String getImportCronExpression() {
		return importCronExpression;
	}

	public void setImportCronExpression(String importCronExpression) {
		this.importCronExpression = importCronExpression;
	}

	public boolean isRunImportAtStartup() {
		return runImportAtStartup;
	}

	public void setRunImportAtStartup(boolean runImportAtStartup) {
		this.runImportAtStartup = runImportAtStartup;
	}

	public Map<String, URI> getUriPerTimetable() {
		return uriPerTimetable;
	}

	public void setUriPerTimetable(Map<String, URI> uriPerTimetable) {
		this.uriPerTimetable = uriPerTimetable;
	}

	public int getImportDaysInPast() {
		return importDaysInPast;
	}

	public void setImportDaysInPast(int importDaysInPast) {
		this.importDaysInPast = importDaysInPast;
	}

	public int getImportDaysInFuture() {
		return importDaysInFuture;
	}

	public void setImportDaysInFuture(int importDaysInFuture) {
		this.importDaysInFuture = importDaysInFuture;
	}

	public boolean isWriteCallsCollection() {
		return writeCallsCollection;
	}

	public void setWriteCallsCollection(boolean writeCallsCollection) {
		this.writeCallsCollection = writeCallsCollection;
	}

	public String getImportDatabasePrefix() {
		return importDatabasePrefix;
	}

	public void setImportDatabasePrefix(String importDatabasePrefix) {
		this.importDatabasePrefix = importDatabasePrefix;
	}

	public File getTemporaryFilesDirectory() {
		return temporaryFilesDirectory;
	}

	public void setTemporaryFilesDirectory(File temporaryFilesDirectory) {
		this.temporaryFilesDirectory = temporaryFilesDirectory;
	}

	public int getMaxVersionsToKeep() {
		return maxVersionsToKeep;
	}

	public void setMaxVersionsToKeep(int maxVersionsToKeep) {
		this.maxVersionsToKeep = maxVersionsToKeep;
	}

	public float getMaxRelativeDifference() {
		return maxRelativeDifference;
	}

	public void setMaxRelativeDifference(float maxRelativeDifference) {
		this.maxRelativeDifference = maxRelativeDifference;
	}

	public boolean isDeleteUnknownResources() {
		return deleteUnknownResources;
	}

	public void setDeleteUnknownResources(boolean deleteUnknownResources) {
		this.deleteUnknownResources = deleteUnknownResources;
	}

	public int getHistoryNumberOfDays() {
		return historyNumberOfDays;
	}

	public void setHistoryNumberOfDays(int historyNumberOfDays) {
		this.historyNumberOfDays = historyNumberOfDays;
	}

	public LocalTime getHistoryExportTimeOfDay() {
		return historyExportTimeOfDay;
	}

	public void setHistoryExportTimeOfDay(LocalTime historyExportTimeOfDay) {
		this.historyExportTimeOfDay = historyExportTimeOfDay;
	}
}
