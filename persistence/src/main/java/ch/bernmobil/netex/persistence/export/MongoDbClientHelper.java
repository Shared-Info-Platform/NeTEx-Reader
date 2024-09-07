package ch.bernmobil.netex.persistence.export;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import ch.bernmobil.netex.persistence.Constants;

public class MongoDbClientHelper {

	public static MongoClient createClient(String connectionString) {
		final ConnectionString connection = new ConnectionString(connectionString);
		final CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
				MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromCodecs(new ZonedDateTimeCodec()),
				CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
		final MongoClientSettings clientSettings = MongoClientSettings.builder()
				.applyConnectionString(connection)
				.codecRegistry(codecRegistry)
				.build();
		return MongoClients.create(clientSettings);
	}

	public static class ZonedDateTimeCodec implements Codec<ZonedDateTime> {
		@Override
		public Class<ZonedDateTime> getEncoderClass() {
			return ZonedDateTime.class;
		}

		@Override
		public void encode(BsonWriter writer, ZonedDateTime object, EncoderContext encoderContext) {
			writer.writeDateTime(object.toInstant().toEpochMilli());
		}

		@Override
		public ZonedDateTime decode(BsonReader reader, DecoderContext decoderContext) {
			return Instant.ofEpochMilli(reader.readDateTime()).atZone(Constants.ZONE_ID);
		}
	}
}
