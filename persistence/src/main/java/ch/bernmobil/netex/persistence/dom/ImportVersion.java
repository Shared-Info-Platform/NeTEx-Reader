package ch.bernmobil.netex.persistence.dom;

import java.time.Instant;
import java.time.LocalDate;

import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

public class ImportVersion {

	public static final String FIELDNAME_TIMETABLE = "timetable";
	public static final String FIELDNAME_VERSION = "version";
	public static final String FIELDNAME_CREATED_AT = "createdAt";
	public static final String FIELDNAME_SCHEMA_VERSION = "schemaVersion";

	public static final long CURRENT_SCHEMA_VERSION = 1;

	@BsonId
	public String getId() {
		return schemaVersion + "_" + timetable + "_" + version;
	}

	@BsonProperty(FIELDNAME_TIMETABLE)
	public String timetable;
	@BsonProperty(FIELDNAME_VERSION)
	public String version;
	@BsonProperty(FIELDNAME_CREATED_AT)
	public Instant createdAt;

	public String uri;
	public String etag;
	public String zipFile;
	public String directory;

	public String databaseName;

	public LocalDate firstDate;
	public LocalDate lastDate;

	public boolean complete;

	@BsonProperty(FIELDNAME_SCHEMA_VERSION)
	public long schemaVersion = CURRENT_SCHEMA_VERSION;
}
