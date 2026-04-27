package ch.bernmobil.netex.persistence.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
}
