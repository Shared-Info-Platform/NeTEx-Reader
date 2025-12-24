package ch.bernmobil.netex.application.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.lingala.zip4j.ZipFile;

public class Downloader {

	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private final File temporaryFilesDirectory;
	private final FilesystemWrapper filesystem;

	public Downloader(File temporaryFilesDirectory, FilesystemWrapper filesystem) {
		this.temporaryFilesDirectory = temporaryFilesDirectory;
		this.filesystem = filesystem;
	}

	/**
	 * Downloads a file from an URL, stores it in a temporary directory and returns the file.
	 */
	public NetexFile downloadFileFromUrlToTemporaryDirectory(URI uri) throws IOException, InterruptedException {
		return downloadFileFromUrlToTemporaryDirectoryIfNecessary(uri, null);
	}

	/**
	 * Downloads a file from an URI to a temporary directory if the Etag is different. Otherwise it does nothing and returns null.
	 */
	public NetexFile downloadFileFromUrlToTemporaryDirectoryIfNecessary(URI uri, String previousEtag)
			throws IOException, InterruptedException {
		// Prepare request
		final HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(uri).GET();
		if (previousEtag != null) {
			// Add If-None-Match if we have an ETag
			requestBuilder.header("If-None-Match", previousEtag);
		}
		final HttpRequest request = requestBuilder.build();

		// Send request
		final HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

		// Check response code
		if (response.statusCode() == 304) {
			return null;
		} else if (response.statusCode() != 200) {
			throw new IOException("Unexpected HTTP status " + response.statusCode());
		}

		// Read metadata
		final String filename = Path.of(response.uri().getPath()).getFileName().toString();
		final String newEtag = response.headers().firstValue("ETag").orElse(null);
		if (newEtag == null) {
			logger.warn("no ETag defined in response");
		}

		// Store content as file
		final File tempDirectory = getTemporaryDirectory();
		filesystem.createDirectoriesIfNecessary(tempDirectory);

		final File tempFile = new File(tempDirectory, filename);
		if (filesystem.exists(tempFile)) {
			logger.warn("file {} already exists, trying to delete it", tempFile);
			filesystem.deleteFile(tempFile);
		}

		try (final InputStream inputStream = response.body()) {
			Files.copy(inputStream, tempFile.toPath());
		}

		return new NetexFile(tempFile, newEtag);
	}

	/**
	 * Extracts a zip file to a temporary directory and returns the directory.
	 */
	public File extractZipFileToTemporarySubfolder(File zipFile) throws IOException {
		final Path tempSubfolder = createTemporarySubfolder(zipFile.getName().replaceAll("\\.zip", ""));
		logger.info("extracting {} to {}", zipFile, tempSubfolder);
		try (final ZipFile zip = new ZipFile(zipFile)) {
			zip.extractAll(tempSubfolder.toString());
		}
		return tempSubfolder.toFile();
	}

	/**
	 * Creates a subfolder in the temp directory.
	 */
	private Path createTemporarySubfolder(String subfolderName) throws IOException {
		final File tempDirectory = getTemporaryDirectory();
		final File tempSubfolder = new File(tempDirectory, subfolderName);
		if (filesystem.exists(tempSubfolder)) {
			logger.warn("directory {} already exists, trying to delete it", tempSubfolder);
			filesystem.deleteDirectory(tempSubfolder);
		}
		return filesystem.createDirectoriesIfNecessary(tempSubfolder);
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

	public record NetexFile(File file, String etag) {
	}
}
