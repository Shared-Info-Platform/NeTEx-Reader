package ch.bernmobil.netex.importer;

import java.time.ZoneId;

public class Constants {

	public static final int NUMBER_OF_THREADS = 10;
	public static final int MAX_ENTRIES_IN_QUEUE = 200;
	public static final int SLEEP_MS_WHEN_QUEUE_FULL = 100;
	public static final int LOG_STATISTICS_EVERY_N_JOURNEYS = 10000;

	public static final int MAX_NUMBER_OF_AGGREGATIONS_IN_MEMORY = 250_000;

	public static final int MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE = 500;
	public static final int MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE = 5000;

	public static final ZoneId ZONE_ID = ZoneId.of("CET");
}
