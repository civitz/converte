package converte;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import converte.files.SimpleFileRecursiveFinder;
import javafx.collections.FXCollections;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
	private TableView<SourceFile> tableView;

	@FXML
	void listDragOver(DragEvent de) {
		Dragboard board = de.getDragboard();
		if (board.hasFiles()) {
			de.acceptTransferModes(TransferMode.ANY);
		}
	}

	@FXML
	void listDropped(DragEvent de) {
		System.out.println("called list dropped ");
		Dragboard board = de.getDragboard();
		List<File> phil = board.getFiles();
		Stream<SourceFile> sourceFiles = SimpleFileRecursiveFinder.findRecursively(phil);
		sourceFiles
			.peek(el -> System.out.println(el))
			.forEach(tableView.getItems()::add);
	}

	@FXML
	protected void convertAction(ActionEvent event) {
		convertButton.setDisable(true);
		Service<Void> s = new FfmpegConvertService();
		progress.progressProperty().bind(s.progressProperty());
		s.setOnRunning(running -> {
			progress.setDisable(false);
			actiontarget.setText("Converting...");
		});
		s.setOnSucceeded(success -> {
			actiontarget.setText("Converted");
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		s.setOnFailed(fail -> {
			actiontarget.setText("Failed: " + fail.getSource().getException());
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		s.start();
	}

	public static void doTheThing(Consumer<Double> onProgressPercent) throws IOException {
		final String source = "/tmp/converte/Have It All - Side By Side.mp3";
		final String outputPath = "/tmp/converted/output.mp3";
		final int channels = 2;
		final int sampleRate = 22_050;
		final int bitRate = 56 * 1024;

//		FFmpeg ffmpeg = new FFmpeg("./ffmpeg-3.4.2-64bit-static/ffmpeg");
//		FFprobe ffprobe = new FFprobe("./ffmpeg-3.4.2-64bit-static/ffprobe");
		System.out.println(new File(".").getAbsolutePath());
		FFmpeg ffmpeg = new FFmpeg("bin/ffmpeg");
		FFprobe ffprobe = new FFprobe("bin/ffprobe");
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
	}

	private static final class FfmpegConvertService extends Service<Void> {
		@Override
		protected Task<Void> createTask() {
			return new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					doTheThing(percent -> updateProgress(percent, 100d));
					return null;
				}
			};
		}
	}
}
