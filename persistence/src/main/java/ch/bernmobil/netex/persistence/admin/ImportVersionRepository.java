package ch.bernmobil.netex.persistence.admin;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;

import ch.bernmobil.netex.persistence.dom.ImportVersion;
import ch.bernmobil.netex.persistence.search.Helper;

public class ImportVersionRepository {

	private final MongoCollection<ImportVersion> importVersionCollection;

	public ImportVersionRepository(MongoClient mongoClient, String databaseName) {
		final MongoDatabase database = mongoClient.getDatabase(databaseName);

		importVersionCollection = database.getCollection("ImportVersions", ImportVersion.class);
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put(ImportVersion.FIELDNAME_SCHEMA_VERSION, 1);
			index.put(ImportVersion.FIELDNAME_TIMETABLE, 1);
			index.put(ImportVersion.FIELDNAME_VERSION, 1);
			importVersionCollection.createIndex(new Document(index), new IndexOptions().unique(true));
		}
		{
			final Map<String, Integer> index = new LinkedHashMap<>();
			index.put(ImportVersion.FIELDNAME_SCHEMA_VERSION, 1);
			index.put(ImportVersion.FIELDNAME_TIMETABLE, 1);
			index.put(ImportVersion.FIELDNAME_CREATED_AT, 1);
			importVersionCollection.createIndex(new Document(index), new IndexOptions());
		}
	}

	public void insertOrUpdate(ImportVersion importVersion) {
		final Bson filter = Filters.and(Filters.eq(ImportVersion.FIELDNAME_SCHEMA_VERSION, importVersion.schemaVersion),
										Filters.eq(ImportVersion.FIELDNAME_TIMETABLE, importVersion.timetable),
										Filters.eq(ImportVersion.FIELDNAME_VERSION, importVersion.version));
		importVersionCollection.replaceOne(filter, importVersion, new ReplaceOptions().upsert(true));
	}

	public void deleteImportVersion(ImportVersion importVersion) {
		final Bson filter = Filters.eq(importVersion.getId());
		importVersionCollection.deleteOne(filter);
	}

	public Optional<ImportVersion> getImportVersion(String timetable, String version) {
		final Bson filter = Filters.and(Filters.eq(ImportVersion.FIELDNAME_SCHEMA_VERSION, ImportVersion.CURRENT_SCHEMA_VERSION),
										Filters.eq(ImportVersion.FIELDNAME_TIMETABLE, timetable),
										Filters.eq(ImportVersion.FIELDNAME_VERSION, version));
		return Optional.ofNullable(importVersionCollection.find(filter).limit(1).first());
	}

	public Optional<ImportVersion> getLastImportVersion(String timetable) {
		final Bson filter = Filters.and(Filters.eq(ImportVersion.FIELDNAME_SCHEMA_VERSION, ImportVersion.CURRENT_SCHEMA_VERSION),
										Filters.eq(ImportVersion.FIELDNAME_TIMETABLE, timetable));
		final Bson sort = Sorts.descending(ImportVersion.FIELDNAME_CREATED_AT);
		return Optional.ofNullable(importVersionCollection.find(filter).sort(sort).limit(1).first());
	}

	public List<ImportVersion> getImportVersions(String timetable) {
		final Bson filter = Filters.and(Filters.eq(ImportVersion.FIELDNAME_SCHEMA_VERSION, ImportVersion.CURRENT_SCHEMA_VERSION),
										Filters.eq(ImportVersion.FIELDNAME_TIMETABLE, timetable));
		final Bson sort = Sorts.descending(ImportVersion.FIELDNAME_CREATED_AT);
		return Helper.iterableToList(importVersionCollection.find(filter).sort(sort));
	}

	/**
	 * Gets the active version for each timetable. The active version is the last created complete version unless there's a forced version.
	 */
	public Collection<ImportVersion> getActiveImportVersions() {
		// Note: instead of filtering everything in MongoDB we just get all versions and filter in java
		final Bson filter = Filters.eq(ImportVersion.FIELDNAME_SCHEMA_VERSION, ImportVersion.CURRENT_SCHEMA_VERSION);
		final Bson sort = Sorts.descending(ImportVersion.FIELDNAME_CREATED_AT);
		final List<ImportVersion> versions = Helper.iterableToList(importVersionCollection.find(filter).sort(sort));

		final Map<String, ImportVersion> activeVersionPerTimetable = new HashMap<>();
		for (final ImportVersion version : versions) {
			if (version.complete) {
				if (version.force) {
					activeVersionPerTimetable.put(version.timetable, version);
				} else if (!activeVersionPerTimetable.containsKey(version.timetable)) {
					// the versions are ordered by descending createdAt, so the first complete version per timetable is active unless
					// there's a forced version
					activeVersionPerTimetable.put(version.timetable, version);
				}
			}
		}

		return activeVersionPerTimetable.values();
	}

	/**
	 * This also includes versions for a different schema version.
	 */
	public List<ImportVersion> getAllImportVersions() {
		return Helper.iterableToList(importVersionCollection.find());
	}
}
