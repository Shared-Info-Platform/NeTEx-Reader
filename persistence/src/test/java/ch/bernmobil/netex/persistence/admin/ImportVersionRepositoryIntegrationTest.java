package ch.bernmobil.netex.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

import ch.bernmobil.netex.persistence.PersistenceConfig;
import ch.bernmobil.netex.persistence.PersistenceProperties;
import ch.bernmobil.netex.persistence.dom.ImportVersion;

@SpringBootTest(classes = PersistenceConfig.class)
@ActiveProfiles("test")
public class ImportVersionRepositoryIntegrationTest {

	@Autowired
	private ImportVersionRepository repository;

	@Autowired
	private MongoClient mongoClient;

	@Autowired
	private PersistenceProperties properties;

	private MongoCollection<ImportVersion> collection;

	@BeforeEach
	private void setup() {
		collection = mongoClient.getDatabase(properties.getAdminDatabaseName()).getCollection("ImportVersions", ImportVersion.class);
		collection.deleteMany(Filters.empty());
	}

	@Test
	public void testCanInsertVersion() {
		assertThat(collection.countDocuments()).isEqualTo(0);
		insertVersion("2025", "version1", 1);
		assertThat(collection.countDocuments()).isEqualTo(1);
	}

	@Test
	public void testCanInsertMultipleVersions() {
		assertThat(collection.countDocuments()).isEqualTo(0);
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		assertThat(collection.countDocuments()).isEqualTo(3);
	}

	@Test
	public void testCanGetSpecificVersion() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		final ImportVersion result = repository.getImportVersion("2025", "version2").get();
		assertThat(result.timetable).isEqualTo("2025");
		assertThat(result.version).isEqualTo("version2");
		assertThat(result.createdAt).isEqualTo(Instant.ofEpochSecond(2));
		assertThat(result.schemaVersion).isEqualTo(ImportVersion.CURRENT_SCHEMA_VERSION);
	}

	@Test
	public void testReturnsEmptyIfVersionDoesNotExist() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		assertThat(repository.getImportVersion("2025", "version4")).isEmpty();
	}

	@Test
	public void testCanUpdateVersion() {
		insertVersion("2025", "version", 1);
		assertThat(repository.getImportVersion("2025", "version").get().createdAt).isEqualTo(Instant.ofEpochSecond(1));
		insertVersion("2025", "version", 2);
		assertThat(repository.getImportVersion("2025", "version").get().createdAt).isEqualTo(Instant.ofEpochSecond(2));
		insertVersion("2025", "version", 3);
		assertThat(repository.getImportVersion("2025", "version").get().createdAt).isEqualTo(Instant.ofEpochSecond(3));
	}

	@Test
	public void testCanDeleteVersion() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		assertThat(repository.getImportVersion("2025", "version1")).isPresent();
		assertThat(repository.getImportVersion("2025", "version2")).isPresent();
		assertThat(repository.getImportVersion("2025", "version3")).isPresent();

		repository.deleteImportVersion(repository.getImportVersion("2025", "version2").get());

		assertThat(repository.getImportVersion("2025", "version1")).isPresent();
		assertThat(repository.getImportVersion("2025", "version2")).isNotPresent();
		assertThat(repository.getImportVersion("2025", "version3")).isPresent();
	}

	@Test
	public void testCanGetAllVersionsForTimetable_orderedByDescendingCreatedAt() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		final List<ImportVersion> result = repository.getImportVersions("2025");
		assertThat(result).hasSize(3);
		assertThat(result.get(0).version).isEqualTo("version3");
		assertThat(result.get(1).version).isEqualTo("version2");
		assertThat(result.get(2).version).isEqualTo("version1");
	}

	@Test
	public void testCanGetLastVersionForTimetable() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		final ImportVersion result = repository.getLastImportVersion("2025").get();
		assertThat(result.version).isEqualTo("version3");
	}

	@Test
	public void testReturnsEmptyIfNoLastVersionExists() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);

		assertThat(repository.getLastImportVersion("2026")).isEmpty();
	}

	@Test
	public void testHandlesVersionWithDifferentSchemaVersionCorrectly() {
		insertVersion("2025", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		insertVersion("2025", "version2", 2, ImportVersion.CURRENT_SCHEMA_VERSION);
		insertVersion("2025", "version3", 3, ImportVersion.CURRENT_SCHEMA_VERSION);
		insertVersion("2025", "version1", 4, ImportVersion.CURRENT_SCHEMA_VERSION + 1);
		insertVersion("2025", "version2", 5, ImportVersion.CURRENT_SCHEMA_VERSION + 1);
		insertVersion("2025", "version3", 6, ImportVersion.CURRENT_SCHEMA_VERSION + 1);

		// ignores different schema versions when specific version is queried
		assertThat(repository.getImportVersion("2025", "version2").get().createdAt).isEqualTo(Instant.ofEpochSecond(2));

		// ignores different schema versions when all versions for timetable are queried
		final List<ImportVersion> result = repository.getImportVersions("2025");
		assertThat(result).hasSize(3);
		assertThat(result.get(0).version).isEqualTo("version3");
		assertThat(result.get(0).createdAt).isEqualTo(Instant.ofEpochSecond(3));
		assertThat(result.get(1).version).isEqualTo("version2");
		assertThat(result.get(1).createdAt).isEqualTo(Instant.ofEpochSecond(2));
		assertThat(result.get(2).version).isEqualTo("version1");
		assertThat(result.get(2).createdAt).isEqualTo(Instant.ofEpochSecond(1));

		// ignores different schema version when last version is queried
		assertThat(repository.getLastImportVersion("2025").get().createdAt).isEqualTo(Instant.ofEpochSecond(3));

		// ignores different schema version when active version is queried
		assertThat(repository.getActiveImportVersions()).hasSize(1);
		assertThat(repository.getActiveImportVersions()).extracting(iv -> iv.createdAt).containsExactly(Instant.ofEpochSecond(3));

		// returns different schema versions when all versions are queried
		assertThat(repository.getAllImportVersions()).hasSize(6);

		// deletes version of correct schema version
		repository.deleteImportVersion(repository.getImportVersion("2025", "version2").get());

		assertThat(repository.getImportVersion("2025", "version2")).isNotPresent();
		assertThat(repository.getImportVersions("2025")).hasSize(2);
		assertThat(repository.getAllImportVersions()).hasSize(5);
	}

	@Test
	public void testActiveVersionIsTheOneWithHighestCreatedAtPerTimetable_whenAllAreCompleteAndNoneIsForced() {
		final ImportVersion version1 = createVersion("2025", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version2 = createVersion("2025", "version2", 2, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version3 = createVersion("2025", "version3", 3, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version4 = createVersion("2026", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		repository.insertOrUpdate(version1);
		repository.insertOrUpdate(version2);
		repository.insertOrUpdate(version3);
		repository.insertOrUpdate(version4);

		final Collection<ImportVersion> result = repository.getActiveImportVersions();
		assertThat(result).hasSize(2);
		assertThat(result).extracting(ImportVersion::getId).containsExactlyInAnyOrder("1_2025_version3", "1_2026_version1");
	}

	@Test
	public void testActiveVersionIsTheOneWithHighestCreatedAtPerTimetableThatIsComplete_whenNoneIsForced() {
		final ImportVersion version1 = createVersion("2025", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version2 = createVersion("2025", "version2", 2, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version3 = createVersion("2025", "version3", 3, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version4 = createVersion("2026", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		version3.complete = false;
		version4.complete = false;
		repository.insertOrUpdate(version1);
		repository.insertOrUpdate(version2);
		repository.insertOrUpdate(version3);
		repository.insertOrUpdate(version4);

		final Collection<ImportVersion> result = repository.getActiveImportVersions();
		assertThat(result).hasSize(1);
		assertThat(result).extracting(ImportVersion::getId).containsExactlyInAnyOrder("1_2025_version2");
	}

	@Test
	public void testActiveVersionIsTheForcedOne() {
		final ImportVersion version1 = createVersion("2025", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version2 = createVersion("2025", "version2", 2, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version3 = createVersion("2025", "version3", 3, ImportVersion.CURRENT_SCHEMA_VERSION);
		final ImportVersion version4 = createVersion("2026", "version1", 1, ImportVersion.CURRENT_SCHEMA_VERSION);
		version2.force = true;
		repository.insertOrUpdate(version1);
		repository.insertOrUpdate(version2);
		repository.insertOrUpdate(version3);
		repository.insertOrUpdate(version4);

		final Collection<ImportVersion> result = repository.getActiveImportVersions();
		assertThat(result).hasSize(2);
		assertThat(result).extracting(ImportVersion::getId).containsExactlyInAnyOrder("1_2025_version2", "1_2026_version1");
	}

	private void insertVersion(String timetable, String version, int createdAt) {
		insertVersion(timetable, version, createdAt, ImportVersion.CURRENT_SCHEMA_VERSION);
	}

	private void insertVersion(String timetable, String version, int createdAt, long schemaVersion) {
		repository.insertOrUpdate(createVersion(timetable, version, createdAt, schemaVersion));
	}

	private static ImportVersion createVersion(String timetable, String version, int createdAt, long schemaVersion) {
		final ImportVersion importVersion = new ImportVersion();
		importVersion.timetable = timetable;
		importVersion.version = version;
		importVersion.createdAt = Instant.ofEpochSecond(createdAt);
		importVersion.schemaVersion = schemaVersion;
		importVersion.complete = true;
		return importVersion;
	}
}
