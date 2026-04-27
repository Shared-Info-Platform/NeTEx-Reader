package ch.bernmobil.netex.persistence.admin;

import org.bson.conversions.Bson;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import ch.bernmobil.netex.persistence.model.task.HistoryTask;
import ch.bernmobil.netex.persistence.model.task.Task;

public class TaskRepository {

	private final MongoCollection<Task> taskCollection;

	public TaskRepository(MongoClient mongoClient, String databaseName) {
		final MongoDatabase database = mongoClient.getDatabase(databaseName);

		taskCollection = database.getCollection("Tasks", Task.class);
	}

	public HistoryTask getHistoryTask() {
		final Bson filter = Filters.eq(HistoryTask.TASK_ID);
		return taskCollection.find(filter, HistoryTask.class).first();
	}

	public void updateHistoryTask(HistoryTask task) {
		final Bson filter = Filters.eq(HistoryTask.TASK_ID);
		taskCollection.replaceOne(filter, task, new ReplaceOptions().upsert(true));
	}
}
