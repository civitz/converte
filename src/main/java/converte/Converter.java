package converte;

import converte.AbstractConversionParameters.Channels;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Converter {

	private static final Logger logger = LoggerFactory.getLogger(Converter.class);

	public static Runnable convert(Consumer<Double> onProgressPercent, FfmpegParameters ffmpegParams,
			ConversionParameters params, SourceFile sourceFile, Path outputFile) throws IOException {
		final String sourcePath = "\""+sourceFile.filenameProperty().get()+"\"";
		final String outputPath = outputFile.toString();
		final int channels = params.getChannels() == Channels.MONO ? 1 : 2;
		final int sampleRate = params.getSampleRate();
		final int bitRate = params.getBitRate();
		logger.info("Converting {} to {}bps @ {}Hz {} to file {}", sourcePath, bitRate, sampleRate,
				params.getChannels(), outputPath);
		FFmpeg ffmpeg = new FFmpeg(ffmpegParams.ffmpegPath());
		FFprobe ffprobe = new FFprobe(ffmpegParams.ffprobePath());
		final FFmpegProbeResult in = ffprobe.probe(sourcePath);
		// Using the FFmpegProbeResult determine the duration of the input
		final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
		FFmpegBuilder builder = new FFmpegBuilder().setInput(sourcePath)					.overrideOutputFiles(true)
				.addOutput(outputPath)
				.setFormat("mp3") // Format is inferred from filename, or can be
									// set
				.setAudioChannels(channels)
				.setAudioCodec("mp3")
				.setAudioSampleRate(sampleRate)
				.setAudioBitRate(bitRate)
				.done();

		FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
		// Run a one-pass encode
		return executor.createJob(builder, progress -> {
			double percent = (double) progress.out_time_ns / duration_ns;
			onProgressPercent.accept(percent);
		});
	}

}
