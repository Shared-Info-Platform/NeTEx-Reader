package ch.bernmobil.netex.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.bernmobil.netex.persistence.export.MongoDbWriter;
import net.lingala.zip4j.ZipFile;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Imports journeys from a set of NeTEx files. Exactly one input must be defined: file, directory, zip-file, or url.",
		 mixinStandardHelpOptions = true,
		 sortOptions = false,
		 version = "1.0")
public class Main implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

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

			final MongoDbWriter mongoDbWriter = new MongoDbWriter(connectionString, databaseName);
			final Importer importer = new Importer(mongoDbWriter);

			if (url != null) {
				zipFile = downloadFileFromUrlToTemporaryDirectory(url);
			}

			if (zipFile != null) {
				directory = extractZipFileToTemporarySubfolder(zipFile);
			}

			if (file != null) {
				importer.importFile(file);
			} else if (directory != null) {
				importer.importDirectory(directory);
			}
		} catch (Throwable t) {
			LOGGER.error("import failed", t);
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
			LOGGER.error("no source for netex files defined");
			return false;
		} else if (nonNullOptions.size() > 1) {
			LOGGER.error("multiple sources for netex files defined {}", nonNullOptions.keySet());
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Downloads a file from an URL, stores it in a temporary directory and returns the file.
	 */
	private File downloadFileFromUrlToTemporaryDirectory(URL url) throws IOException {
		final String fileName = "netex-" + System.currentTimeMillis() + ".tmp";
		final File tempDirectory = getTemporaryDirectory();
		final File tempFile = new File(tempDirectory, fileName);
		try (final InputStream inputStream = url.openStream()) {
			Files.copy(inputStream, tempFile.toPath());
		}
		return tempFile;
	}

	/**
	 * Extracts a zip file to a temporary directory and returns the directory.
	 */
	private File extractZipFileToTemporarySubfolder(File zipFile) throws IOException {
		final Path tempSubfolder = createTemporarySubfolder();
		LOGGER.info("extracting {} to {}", zipFile, tempSubfolder);
		try (final ZipFile zip = new ZipFile(zipFile)) {
			zip.extractAll(tempSubfolder.toString());
		}
		return tempSubfolder.toFile();
	}

	/**
	 * Creates a subfolder in the temp directory.
	 */
	private Path createTemporarySubfolder() throws IOException {
		final String subfolderName = "netex-" + System.currentTimeMillis();
		final File tempDirectory = getTemporaryDirectory();
		final File tempSubfolder = new File(tempDirectory, subfolderName);
		return Files.createDirectory(tempSubfolder.toPath());
	}

	/**
	 * Returns the user-defined temporary directory (if defined) or the system default.
	 */
	private File getTemporaryDirectory() {
		if (temporaryFilesDirectory != null) {
			return temporaryFilesDirectory;
		} else {
			return new File(System.getProperty("java.io.tmpdir"));
		}
	}

	public static void main(String[] args) throws XMLStreamException, InterruptedException {
		new CommandLine(new Main()).execute(args);
	}
}
