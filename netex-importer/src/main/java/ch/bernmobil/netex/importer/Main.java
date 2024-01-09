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

import ch.bernmobil.netex.importer.mongodb.export.MongoDbWriter;
import net.lingala.zip4j.ZipFile;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Imports journeys from a set of NeTEx files",  mixinStandardHelpOptions = true, version = "1.0")
public class Main implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

	@Option(names = {"-f", "--file"},
			description = "An *.xml file that contains netex data")
	private File file;

	@Option(names = {"-d", "--directory"},
			description = "A directory that contains *.xml files with netex data")
	private File directory;

	@Option(names = {"-z", "--zip-file"},
			description = "A *.zip file that contains *.xml files with netex data")
	private File zipFile;

	@Option(names = {"-u", "--url"},
			description = "URL to a *.zip file that contains *.xml files with netex data")
	private URL url;

	@Option(names = {"-t", "--temporary-directory"},
			description = "A directory where temporary files can be stored (downloaded or unpacked zip files)")
	private File temporaryFilesDirectory;

	@Option(names = {"-c", "--mongo-connection-string"},
			description = "Connection string for MongoDB",
			defaultValue = "mongodb://localhost:27017/")
	private String connectionString;

	@Option(names = {"-n", "--mongo-database-name"},
			description = "Name of the database in MongoDB",
			defaultValue = "netex")
	private String databaseName;

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
		fileOptions.put("zipFile", zipFile);
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
