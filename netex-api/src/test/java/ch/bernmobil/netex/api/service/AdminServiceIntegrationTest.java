package ch.bernmobil.netex.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import ch.bernmobil.netex.api.NetexApiConfig;
import ch.bernmobil.netex.persistence.PersistenceConfig;
import ch.bernmobil.netex.persistence.admin.ImportVersionRepository;
import ch.bernmobil.netex.persistence.model.ImportVersion;

@SpringBootTest(classes = {NetexApiConfig.class, PersistenceConfig.class})
@ActiveProfiles("test")
public class AdminServiceIntegrationTest {

	@Autowired
	private AdminService service;

	@Autowired
	private ImportVersionRepository repository;

	@BeforeEach
	public void setup() {
		repository.getAllImportVersions().forEach(repository::deleteImportVersion);
	}

	@Test
	public void testReturnsAllTimetables() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 1);

		assertThat(service.getTimetables()).containsExactlyInAnyOrder("2025", "2026");
	}

	@Test
	public void testReturnsAllVersionsForTimetable() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 1);

		assertThat(service.getVersions("2025")).containsExactly("version3", "version2", "version1");
		assertThat(service.getVersions("2026")).containsExactly("version1");
		assertThat(service.getVersions("2027")).isEmpty();
	}

	@Test
	public void testReturnsSpecificVersion() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 4);

		assertThat(service.getVersion("2025", "version1").get().createdAt).isEqualTo(Instant.ofEpochSecond(1));
		assertThat(service.getVersion("2025", "version2").get().createdAt).isEqualTo(Instant.ofEpochSecond(2));
		assertThat(service.getVersion("2025", "version3").get().createdAt).isEqualTo(Instant.ofEpochSecond(3));
		assertThat(service.getVersion("2026", "version1").get().createdAt).isEqualTo(Instant.ofEpochSecond(4));
		assertThat(service.getVersion("2026", "version2")).isEmpty();
	}

	@Test
	public void testReturnsActiveVersions() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 4);

		assertThat(service.getActiveVersions()).extracting(iv -> iv.createdAt).containsExactlyInAnyOrder(Instant.ofEpochSecond(3),
				Instant.ofEpochSecond(4));
	}

	@Test
	public void testUpdatesForceFlag() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 4);
		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.force).containsOnly(false);

		service.forceVersion("2025", "version2", false);
		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.force).containsOnly(false);

		service.forceVersion("2025", "version2", true);
		assertThat(service.getVersion("2025", "version1").get().force).isFalse();
		assertThat(service.getVersion("2025", "version2").get().force).isTrue();
		assertThat(service.getVersion("2025", "version3").get().force).isFalse();
		assertThat(service.getVersion("2026", "version1").get().force).isFalse();

		service.forceVersion("2025", "version3", true);
		assertThat(service.getVersion("2025", "version1").get().force).isFalse();
		assertThat(service.getVersion("2025", "version2").get().force).isFalse(); // becomes false
		assertThat(service.getVersion("2025", "version3").get().force).isTrue();
		assertThat(service.getVersion("2026", "version1").get().force).isFalse();

		service.forceVersion("2026", "version1", true);
		assertThat(service.getVersion("2025", "version1").get().force).isFalse();
		assertThat(service.getVersion("2025", "version2").get().force).isFalse();
		assertThat(service.getVersion("2025", "version3").get().force).isTrue();
		assertThat(service.getVersion("2026", "version1").get().force).isTrue();
	}

	@Test
	public void testUpdatesKeepFlag() {
		insertVersion("2025", "version1", 1);
		insertVersion("2025", "version2", 2);
		insertVersion("2025", "version3", 3);
		insertVersion("2026", "version1", 4);
		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.keep).containsOnly(false);

		service.keepVersion("2025", "version2", false);
		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.keep).containsOnly(false);

		service.keepVersion("2025", "version3", true);
		assertThat(service.getVersion("2025", "version1").get().keep).isFalse();
		assertThat(service.getVersion("2025", "version2").get().keep).isFalse();
		assertThat(service.getVersion("2025", "version3").get().keep).isTrue();
		assertThat(service.getVersion("2026", "version1").get().keep).isFalse();

		service.keepVersion("2026", "version1", true);
		assertThat(service.getVersion("2025", "version1").get().keep).isFalse();
		assertThat(service.getVersion("2025", "version2").get().keep).isFalse();
		assertThat(service.getVersion("2025", "version3").get().keep).isTrue();
		assertThat(service.getVersion("2026", "version1").get().keep).isTrue();

		service.keepVersion("2025", "version1", true);
		assertThat(service.getVersion("2025", "version1").get().keep).isTrue();
		assertThat(service.getVersion("2025", "version2").get().keep).isFalse();
		assertThat(service.getVersion("2025", "version3").get().keep).isTrue(); // remains true
		assertThat(service.getVersion("2026", "version1").get().keep).isTrue();
	}

	@Test
	public void testUpdatesValidFlag() {
		final ImportVersion versionA = createVersion("2025", "version1", 1);
		final ImportVersion versionB = createVersion("2025", "version2", 2);
		final ImportVersion versionC = createVersion("2025", "version3", 3);
		final ImportVersion versionD = createVersion("2026", "version1", 4);
		versionA.valid = false;
		versionB.valid = false;
		versionC.valid = false;
		versionD.valid = false;
		repository.insertOrUpdate(versionA);
		repository.insertOrUpdate(versionB);
		repository.insertOrUpdate(versionC);
		repository.insertOrUpdate(versionD);

		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.valid).containsOnly(false);

		service.validateVersion("2025", "version2", false);
		assertThat(repository.getAllImportVersions()).extracting(iv -> iv.valid).containsOnly(false);

		service.validateVersion("2025", "version3", true);
		assertThat(service.getVersion("2025", "version1").get().valid).isFalse();
		assertThat(service.getVersion("2025", "version2").get().valid).isFalse();
		assertThat(service.getVersion("2025", "version3").get().valid).isTrue();
		assertThat(service.getVersion("2026", "version1").get().valid).isFalse();

		service.validateVersion("2026", "version1", true);
		assertThat(service.getVersion("2025", "version1").get().valid).isFalse();
		assertThat(service.getVersion("2025", "version2").get().valid).isFalse();
		assertThat(service.getVersion("2025", "version3").get().valid).isTrue();
		assertThat(service.getVersion("2026", "version1").get().valid).isTrue();

		service.validateVersion("2025", "version1", true);
		assertThat(service.getVersion("2025", "version1").get().valid).isTrue();
		assertThat(service.getVersion("2025", "version2").get().valid).isFalse();
		assertThat(service.getVersion("2025", "version3").get().valid).isTrue(); // remains true
		assertThat(service.getVersion("2026", "version1").get().valid).isTrue();
	}

	@Test
	public void testThrowsWhenFlagCannotBeSetBecauseVersionDoesNotExist() {
		assertThatIllegalArgumentException().isThrownBy(() -> service.forceVersion("2025", "version1", true));
		assertThatIllegalArgumentException().isThrownBy(() -> service.keepVersion("2025", "version1", true));
		assertThatIllegalArgumentException().isThrownBy(() -> service.validateVersion("2025", "version1", true));
	}

	private void insertVersion(String timetable, String version, int createdAt) {
		repository.insertOrUpdate(createVersion(timetable, version, createdAt));
	}

	private static ImportVersion createVersion(String timetable, String version, int createdAt) {
		final ImportVersion importVersion = new ImportVersion();
		importVersion.timetable = timetable;
		importVersion.version = version;
		importVersion.createdAt = Instant.ofEpochSecond(createdAt);
		importVersion.complete = true;
		importVersion.valid = true;
		return importVersion;
	}
}
