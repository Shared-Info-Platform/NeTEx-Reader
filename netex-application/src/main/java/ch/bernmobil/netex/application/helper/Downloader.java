package ch.bernmobil.netex.application.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lingala.zip4j.ZipFile;

public class Downloader {

	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private final File temporaryFilesDirectory;

	public Downloader(File temporaryFilesDirectory) {
		this.temporaryFilesDirectory = temporaryFilesDirectory;
	}

	/**
	 * Downloads a file from an URL, stores it in a temporary directory and returns the file.
	 */
	public File downloadFileFromUrlToTemporaryDirectory(URL url) throws IOException {
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
	public File extractZipFileToTemporarySubfolder(File zipFile) throws IOException {
		final Path tempSubfolder = createTemporarySubfolder();
		logger.info("extracting {} to {}", zipFile, tempSubfolder);
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
}
