package converte.files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import converte.SourceFile;

public class SimpleFileRecursiveFinder {
	public static Stream<SourceFile> findRecursively(List<File> roots) {
		return roots.stream()
				.flatMap(SimpleFileRecursiveFinder::findRecursively);
	}

	static Stream<SourceFile> findRecursively(File root) {
		String basePath = root.getAbsolutePath();
		return walk(root)
			.filter(SimpleFileRecursiveFinder::isAudio)
			.map(SourceFile::fromPath)
			.map(sourceFile -> sourceFile.withBasePath(basePath));
	}

	static boolean isAudio(Path p) {
		final File file = p.toFile();
		return file.isFile() && file.getName().toLowerCase().endsWith(".mp3");
	}

	static Stream<Path> walk(File f) {
		try {
			return Files.walk(f.toPath());
		} catch (IOException e) {
			return Stream.empty();
		}
	}
}
