package ch.bernmobil.netex.application.cli;

import java.io.File;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.application.helper.Downloader;
import ch.bernmobil.netex.importer.Importer;
import ch.bernmobil.netex.persistence.export.MongoDbWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Imports journeys from a set of NeTEx files. Exactly one input must be defined: file, directory, zip-file, or url.",
		 mixinStandardHelpOptions = true,
		 sortOptions = false,
		 version = "1.0")
public class Cli implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(Cli.class);

	@Option(names = {"-f", "--file"},
			paramLabel = "<FILE>",
			defaultValue = "${NETEX_FILE}",
			description = "An *.xml file that contains netex data. "
					+ "Can also be defined with environment variable NETEX_FILE.")
	private File file;

	@Option(names = {"-d", "--directory"},
			paramLabel = "<DIRECTORY>",
			defaultValue = "${NETEX_DIRECTORY}",
			description = "A directory that contains *.xml files with netex data. "
					+ "Can also be defined with environment variable NETEX_DIRECTORY.")
	private File directory;

	@Option(names = {"-z", "--zip-file"},
			paramLabel = "<FILE>",
			defaultValue = "${NETEX_ZIP_FILE}",
			description = "A *.zip file that contains *.xml files with netex data. "
					+ "Can also be defined with environment variable NETEX_ZIP_FILE.")
	private File zipFile;

	@Option(names = {"-u", "--url"},
			paramLabel = "<URL>",
			defaultValue = "${NETEX_URL}",
			description = "URL to a *.zip file that contains *.xml files with netex data. "
					+ "Can also be defined with environment variable NETEX_URL.")
	private URL url;

	@Option(names = {"-t", "--temporary-directory"},
			paramLabel = "<DIRECTORY>",
			defaultValue = "${NETEX_TEMPORARY_DIRECTORY}",
			description = "Optional directory where temporary files can be stored (downloaded or unpacked zip files). "
					+ "If not defined, the system default is used. "
					+ "Can also be defined with environment variable NETEX_TEMPORARY_DIRECTORY.")
	private File temporaryFilesDirectory;

	@Option(names = {"-c", "--mongo-connection-string"},
			paramLabel = "<STRING>",
			defaultValue = "${NETEX_MONGO_CONNECTION_STRING}",
			description = "Connection string for MongoDB. "
					+ "Can also be defined with environment variable NETEX_MONGO_CONNECTION_STRING. "
					+ "Default: mongodb://localhost:27017/")
	private String connectionString = "mongodb://localhost:27017/";

	@Option(names = {"-n", "--mongo-database-name"},
			paramLabel = "<NAME>",
			defaultValue = "${NETEX_MONGO_DATABASE_NAME}",
			description = "Name of the database in MongoDB. "
					+ "Can also be defined with environment variable NETEX_MONGO_DATABASE_NAME. "
					+ "Default: netex")
	private String databaseName = "netex";

	@Override
	public void run() {
		try {
			if (!checkOptions()) {
				return;
			}

			final Downloader downloader = new Downloader(temporaryFilesDirectory);
			final MongoDbWriter mongoDbWriter = new MongoDbWriter(connectionString, databaseName);
			final Importer importer = new Importer(mongoDbWriter);

			if (url != null) {
				zipFile = downloader.downloadFileFromUrlToTemporaryDirectory(url);
			}

			if (zipFile != null) {
				directory = downloader.extractZipFileToTemporarySubfolder(zipFile);
			}

			if (file != null) {
				importer.importFile(file);
			} else if (directory != null) {
				importer.importDirectory(directory);
			}
		} catch (Throwable t) {
			logger.error("import failed", t);
			System.exit(1);
		}
	}

	private boolean checkOptions() {
		final Map<String, Object> fileOptions = new LinkedHashMap<>();
		fileOptions.put("file", file);
		fileOptions.put("directory", directory);
		fileOptions.put("zip-file", zipFile);
		fileOptions.put("url", url);

		final Map<String, Object> nonNullOptions = fileOptions.entrySet().stream()
				.filter(entry -> entry.getValue() != null)
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue));

		if (nonNullOptions.isEmpty()) {
			logger.error("no source for netex files defined");
			return false;
		} else if (nonNullOptions.size() > 1) {
			logger.error("multiple sources for netex files defined {}", nonNullOptions.keySet());
			return false;
		} else {
			return true;
		}
	}
}
