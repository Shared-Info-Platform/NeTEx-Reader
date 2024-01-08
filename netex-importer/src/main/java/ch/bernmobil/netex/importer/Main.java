package ch.bernmobil.netex.importer;

import java.io.File;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.importer.mongodb.export.MongoDbWriter;

public class Main {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws XMLStreamException, InterruptedException {
		try {
			final File directory = new File("C:\\projects\\bernmobil\\files\\netex\\prod_netex_tt_1.10_che_ski_2024_oev-schweiz__1_1_202312200612");
			final MongoDbWriter mongoDbWriter = new MongoDbWriter("mongodb://localhost:27017/", "netex");
			final Importer importer = new Importer(mongoDbWriter);
			importer.importDirectory(directory);
		} catch (Throwable t) {
			LOGGER.error("import failed", t);
			System.exit(1);
		}
	}
}
