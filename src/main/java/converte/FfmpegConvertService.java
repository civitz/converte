package converte;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

final class FfmpegConvertService extends Service<Void> {

	private final ObservableList<SourceFile> items;
	private final ConversionParameters params;
	private int parallelism;
	private Path destinationBase;
	private FfmpegParameters ffmpegParams;

	public FfmpegConvertService(ObservableList<SourceFile> items,FfmpegParameters ffmpegParams, ConversionParameters params, int parallelism, Path destinationBase) {
		this.items = items;
		this.params = params;
		this.parallelism = parallelism;
		this.destinationBase = destinationBase;
		this.ffmpegParams = ffmpegParams;
	}

	@Override
	protected Task<Void> createTask() {
		return new ParallelConverterTask();
	}
	
	private final class ParallelConverterTask extends Task<Void> {
		@Override
		protected Void call() throws Exception {
			updateProgress(0d, 100d);
			AtomicInteger count = new AtomicInteger(1);
			ExecutorService pool = Executors.newFixedThreadPool(parallelism);
			for (SourceFile sf : items) {
				Path outputPath = determineOutputPathAndCreateDirs(sf);
				CompletableFuture.supplyAsync(() -> determineOutputPathAndCreateDirs(sf))
					.thenRunAsync(convertSingleFile(sf, outputPath), pool)
					.thenRun(() -> {
						sf.progressDetailsProperty().setValue("Converted to " + outputPath.toString());
						sf.progressProperty().setValue(1d);
						updateProgress(count.incrementAndGet(), items.size());
					})
					.exceptionally(exception -> {
						sf.progressDetailsProperty().set("ERROR: " + exception.getClass().getSimpleName() + " : " +exception.getMessage());
						return (Void ) null;
					});
			}
			return null;
		}
	}
	
	Path determineOutputPathAndCreateDirs(SourceFile sf) {
		try {
			Path outputFilePath = outputFilePath(sf, destinationBase);
			Files.createDirectories(outputFilePath.getParent());
			return outputFilePath;
		} catch (Exception ex) {
			System.err.println("Error creating parent directories for " + sf);
			ex.printStackTrace();
			throw new RuntimeException("Error creating parent directories for " + sf, ex);
		}
	}
	
	Runnable convertSingleFile(SourceFile sf, Path outputFile) {
		try {
			sf.progressDetailsProperty().setValue("Converting...");
			return Converter.convert(percent -> sf.progressProperty().set(percent),ffmpegParams, params, sf, outputFile);
		} catch (Exception e) {
			System.err.println("Error processing file " + sf);
			e.printStackTrace();
			throw new RuntimeException("Error processing file " + sf, e);
		}
	}
	
	static Path outputFilePath(SourceFile sourceFile, Path destinationBase) {
		String sourcePath = sourceFile.filenameProperty().get();
		Path baseSourcePath = Paths.get(sourceFile.basePathProperty().get());
		Path relativeSourcePath = baseSourcePath.relativize(Paths.get(sourcePath));
		return destinationBase.resolve(relativeSourcePath).toAbsolutePath();
	}
}