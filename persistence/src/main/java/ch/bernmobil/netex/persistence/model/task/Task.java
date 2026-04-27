package ch.bernmobil.netex.persistence.model.task;

import org.bson.codecs.pojo.annotations.BsonId;

public class Task {

	@BsonId
	private final String id;

	public Task(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}
}
