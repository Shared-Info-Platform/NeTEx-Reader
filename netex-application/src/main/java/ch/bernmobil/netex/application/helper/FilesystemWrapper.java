package ch.bernmobil.netex.application.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.FileSystemUtils;

public class FilesystemWrapper {

	public boolean exists(File file) {
		return file.exists();
	}

	public boolean isFile(File file) {
		return file.isFile();
	}

	public boolean isDirectory(File file) {
		return file.isDirectory();
	}

	public void deleteFile(File file) throws IOException {
		if (!file.delete()) {
			throw new IOException("could not delete file " + file);
		}
	}

	public void deleteDirectory(File directory) throws IOException {
		if (!FileSystemUtils.deleteRecursively(directory)) {
			throw new IOException("could not delete directory " + directory);
		}
	}

	public Path createDirectoriesIfNecessary(File directory) throws IOException {
		return Files.createDirectories(directory.toPath());
	}

	public List<File> listFiles(File directory) {
		final File[] files = directory.listFiles();
		if (files != null) {
			return Arrays.asList(files);
		} else {
			return List.of();
		}
	}
}
