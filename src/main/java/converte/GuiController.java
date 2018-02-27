package converte;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.base.Throwables;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;

public class GuiController {

	@FXML
	private Text actiontarget;

	@FXML
	private Button convertButton;

	@FXML
	private ProgressBar progress;

	@FXML
	protected void convertAction(ActionEvent event) {
		convertButton.setDisable(true);
		Service<Void> s = new Service<Void>() {
			@Override
			protected Task<Void> createTask() {
				final Task<Void> task = new Task<Void>() {
					@Override
					protected Void call() throws Exception {
						doTheThing(percent -> updateProgress(percent, 100d));
						return null;
					}
				};
				task.progressProperty().addListener(new ChangeListener<Number>() {
					@Override
					public void changed(ObservableValue<? extends Number> observable, Number oldValue,
							Number newValue) {
						progress.setProgress(newValue.doubleValue());
					}
				});
				//task.onSucceededProperty()
				return task;
			}
		};
		s.setOnRunning(running -> {
			actiontarget.setText("Converting...");
		});
		s.setOnSucceeded(success -> {
			actiontarget.setText("Converted");
			convertButton.setDisable(false);
		});
		s.setOnFailed(fail -> {
			actiontarget.setText("Failed: " + fail.getSource().getException());
			convertButton.setDisable(false);
		});
		s.start();
	}

	public static void doTheThing(Consumer<Double> onProgressPercent) throws IOException {
		final String source = "/tmp/converte/Have It All - Side By Side.mp3";
		final String outputPath = "/tmp/converted/output.mp3";
		final int channels = 2;
		final int sampleRate = 22_050;
		final int bitRate = 56 * 1024;

		FFmpeg ffmpeg = new FFmpeg("./ffmpeg-3.4.2-64bit-static/ffmpeg");
		FFprobe ffprobe = new FFprobe("./ffmpeg-3.4.2-64bit-static/ffprobe");
		final FFmpegProbeResult in = ffprobe.probe(source);
		// Using the FFmpegProbeResult determine the duration of the input
		final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);
		FFmpegBuilder builder = new FFmpegBuilder().setInput(source) // Filename,
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
			double percent = progress.out_time_ns / duration_ns * 100d;
			// System.out.println("" + new Date() + " progress " + percent + " :
			// " + progress);
			onProgressPercent.accept(percent);
		}).run();
		System.out.println("done");
	}
}
