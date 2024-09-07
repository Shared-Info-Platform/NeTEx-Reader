package ch.bernmobil.netex.persistence;

import java.time.ZoneId;

public class Constants {

	public static final int MAX_NUMBER_OF_CALLS_PER_MONGODB_WRITE = 500;
	public static final int MAX_NUMBER_OF_AGGREGATIONS_PER_MONGODB_WRITE = 5000;

	public static final ZoneId ZONE_ID = ZoneId.of("CET");
}
