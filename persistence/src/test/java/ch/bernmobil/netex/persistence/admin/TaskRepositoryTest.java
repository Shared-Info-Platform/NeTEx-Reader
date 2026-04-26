package ch.bernmobil.netex.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

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
import ch.bernmobil.netex.persistence.model.task.HaltelogTask;
import ch.bernmobil.netex.persistence.model.task.HistoryTask;
import ch.bernmobil.netex.persistence.model.task.Task;

@SpringBootTest(classes = PersistenceConfig.class)
@ActiveProfiles("test")
public class TaskRepositoryTest {

	@Autowired
	private TaskRepository repository;

	@Autowired
	private MongoClient mongoClient;

	@Autowired
	private PersistenceProperties properties;

	private MongoCollection<Task> collection;

	@BeforeEach
	private void setup() {
		collection = mongoClient.getDatabase(properties.getAdminDatabaseName()).getCollection("Tasks", Task.class);
		collection.deleteMany(Filters.empty());
	}

	@Test
	public void testReturnsNullIfHistoryTaskDoesNotExist() {
		assertThat(repository.getHistoryTask()).isNull();
	}

	@Test
	public void testCanInsertHistoryTask() {
		assertThat(collection.countDocuments()).isEqualTo(0);
		repository.updateHistoryTask(new HistoryTask());
		assertThat(collection.countDocuments()).isEqualTo(1);
	}

	@Test
	public void testReturnsHistoryTaskIfItExists() {
		repository.updateHistoryTask(new HistoryTask());
		assertThat(repository.getHistoryTask()).isNotNull();
	}

	@Test
	public void testCanUpdateHistoryTask() {
		{
			final HistoryTask task = new HistoryTask();
			task.setHistoryExportedUntil(LocalDate.of(2026, 4, 27));
			repository.updateHistoryTask(task);
		}

		assertThat(repository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(LocalDate.of(2026, 4, 27));

		{
			final HistoryTask task = new HistoryTask();
			task.setHistoryExportedUntil(LocalDate.of(2026, 4, 28));
			repository.updateHistoryTask(task);
		}

		assertThat(repository.getHistoryTask().getHistoryExportedUntil()).isEqualTo(LocalDate.of(2026, 4, 28));
	}

	@Test
	public void testReturnsNullIfHaltelogTaskDoesNotExist() {
		assertThat(repository.getHaltelogTask()).isNull();
	}

	@Test
	public void testCanInsertHaltelogTask() {
		assertThat(collection.countDocuments()).isEqualTo(0);
		repository.updateHaltelogTask(new HaltelogTask());
		assertThat(collection.countDocuments()).isEqualTo(1);
	}

	@Test
	public void testReturnsHaltelogTaskIfItExists() {
		repository.updateHaltelogTask(new HaltelogTask());
		assertThat(repository.getHaltelogTask()).isNotNull();
	}

	@Test
	public void testCanUpdateHaltelogTask() {
		{
			final HaltelogTask task = new HaltelogTask();
			task.setHaltelogExportedUntil(LocalDate.of(2026, 4, 27));
			task.setLastExportedVersions(Set.of("version1", "version2"));
			repository.updateHaltelogTask(task);
		}

		assertThat(repository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(LocalDate.of(2026, 4, 27));
		assertThat(repository.getHaltelogTask().getLastExportedVersions()).isEqualTo(Set.of("version1", "version2"));

		{
			final HaltelogTask task = new HaltelogTask();
			task.setHaltelogExportedUntil(LocalDate.of(2026, 4, 28));
			task.setLastExportedVersions(Set.of("version2", "version3"));
			repository.updateHaltelogTask(task);
		}

		assertThat(repository.getHaltelogTask().getHaltelogExportedUntil()).isEqualTo(LocalDate.of(2026, 4, 28));
		assertThat(repository.getHaltelogTask().getLastExportedVersions()).isEqualTo(Set.of("version2", "version3"));
	}
}
