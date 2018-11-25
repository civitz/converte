package converte;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

public class Converter {

	public static File doTheThing(Consumer<Double> onProgressPercent,FfmpegParameters ffmpegParams, ConversionParameters params, SourceFile sourceFile, Path destinationBase) throws IOException {
			final String sourcePath = sourceFile.filenameProperty().get();
			Path baseSourcePath = Paths.get(sourceFile.basePathProperty().get());
			Path relativeSourcePath = baseSourcePath.relativize(Paths.get(sourcePath));
			System.out.println(relativeSourcePath);
			System.out.println(destinationBase.resolve(relativeSourcePath));
			Path outputAbsolute = destinationBase.resolve(relativeSourcePath).toAbsolutePath();
			Files.createDirectories(outputAbsolute.getParent());
			final String outputPath = outputAbsolute.toString();
			final int channels = params.getChannels();
			final int sampleRate = params.getSampleRate();
			final int bitRate = params.getBitRate();
			System.out.println(new File(".").getAbsolutePath());
	
			FFmpeg ffmpeg = new FFmpeg(ffmpegParams.ffmpegPath());
			FFprobe ffprobe = new FFprobe(ffmpegParams.ffprobePath());
			final FFmpegProbeResult in = ffprobe.probe(sourcePath);
			// Using the FFmpegProbeResult determine the duration of the input
			final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
			System.out.println(sourcePath+" duration: " + duration_ns);
			FFmpegBuilder builder = new FFmpegBuilder().setInput(sourcePath) // Filename,
																			// or a
																			// FFmpegProbeResult
					.overrideOutputFiles(true) // Override the output if it exists
					.addOutput(outputPath) // Filename for the
											// destination
					.setFormat("mp3") // Format is inferred from filename, or can be
										// set
					.setAudioChannels(channels) // Mono audio
					.setAudioCodec("mp3") // using the aac codec
					.setAudioSampleRate(sampleRate) // at 48KHz
					.setAudioBitRate(bitRate) // at 32 kbit/s
					.done();
	
			FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
			// Run a one-pass encode
			executor.createJob(builder, progress -> {
				double percent = (double)progress.out_time_ns / duration_ns;
				System.out.println(percent);
				onProgressPercent.accept(percent);
			}).run();
			return new File(outputPath);
		}

}
