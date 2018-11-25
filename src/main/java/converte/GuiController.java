package converte;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.google.common.util.concurrent.Uninterruptibles;

import converte.files.SimpleFileRecursiveFinder;
import converte.utils.OsUtils;
import javafx.application.Application.Parameters;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

public class GuiController implements Initializable {

	@FXML
	private Button convertButton;

	@FXML
	private ProgressBar progress;

	@FXML
	private TableView<SourceFile> tableView;
	
	@FXML
	private TableColumn<SourceFile, String> nameCol;
	@FXML
	private TableColumn<SourceFile, String> pathCol;
	@FXML
	private TableColumn<SourceFile, Double> progressCol;
	@FXML
	private TableColumn<SourceFile, String> detailsCol;
	
	@FXML
	private ComboBox<ConversionParameters> presetList;
	@FXML
	private ComboBox<Integer> bitrateList;
	@FXML
	private ComboBox<Integer> frequencyList;
	@FXML
	private TextField targetPath;
	
	private FfmpegParameters ffmpegParams;

	@Override
	public void initialize(URL var1, ResourceBundle var2) {
		System.out.println("initialize");
		if (OsUtils.isUnix()) {
			this.ffmpegParams = FfmpegParameters.builder()
					.ffmpegPath(Paths.get("ffmpeg-3.4.2-64bit-static/ffmpeg").toAbsolutePath().toString())
					.ffprobePath(Paths.get("ffmpeg-3.4.2-64bit-static/ffprobe").toAbsolutePath().toString())
					.build();
		} else if (OsUtils.isWindows()) {
			this.ffmpegParams = FfmpegParameters.builder()
					.ffmpegPath(Paths.get("ffmpeg-3.4.2-win64-static/bin/ffmpeg.exe").toAbsolutePath().toString())
					.ffprobePath(Paths.get("ffmpeg-3.4.2-win64-static/bin/ffprobe.exe").toAbsolutePath().toString())
					.build();
		}
		progressCol.setCellValueFactory(new PropertyValueFactory<SourceFile, Double>("progress"));
		progressCol.setCellFactory(ProgressBarTableCell.<SourceFile>forTableColumn());
		presetList.setConverter(new ConversionParametersStringConverter());
		presetList.getItems().addAll(
				new ConversionParameters("Low", 11025, 56*1024),
				new ConversionParameters("Medium", 44100, 128*1024),
				new ConversionParameters("High", 44100, 192*1024),
				new ConversionParameters("CD", 44100, 320*1024)
				);
		presetList.valueProperty().addListener(this::onPresetChanged);
		bitrateList.setConverter(new IntegerToStringConverter("k", 1024));
		bitrateList.getItems().addAll(
				56 * 1024,
				128 * 1024,
				192 * 1024,
				320 * 1024);
		bitrateList.valueProperty().addListener(this::onBitrateChanged);
		frequencyList.setConverter(new IntegerToStringConverter("KHz", 1000));
		frequencyList.getItems().addAll(
				11025,
				22050,
				44100);
		frequencyList.valueProperty().addListener(this::onFrequencyChanged);
		// set default
		presetList.getSelectionModel().select(0);
	}
	
	private void onFrequencyChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		System.out.println(String.format("frequencyList: Observable %s, before %s,after %s", observable, before,after));
		// if after is not null, deselect presets
	}

	private void onBitrateChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		System.out.println(String.format("bitrateList: Observable %s, before %s,after %s", observable, before,after));
		// if after is not null, deselect presets
	}

	private void onPresetChanged(ObservableValue<? extends ConversionParameters> observable, ConversionParameters before,
			ConversionParameters after) {
		System.out.println(String.format("presetList: Observable %s, before %s,after %s", observable, before,after));
		if(after != null) {
			//deselect from bitrate and sample rate
		}
	}
	
	public void setParameters(Parameters parameters) {
		System.out.println("set parameters");
		for (Entry<String, String> entry : parameters.getNamed().entrySet()) {
			switch (entry.getKey()) {
			case "ffmpegPath":
				ffmpegParams = ffmpegParams.withFfmpegPath(entry.getValue());
				break;
			case "ffprobePath":
				ffmpegParams= ffmpegParams.withFfprobePath(entry.getValue());
				break;
			default: {
				System.out.println("Ignoring parameter " + entry.getKey());
			}
				break;
			}
		}
	}
	
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

		pathCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/25));
		nameCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/30));
		progressCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/15));
		detailsCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/20));
	}

	@FXML
	protected void convertAction(ActionEvent event) {
		
		convertButton.setDisable(true);
		ConversionParameters params = presetList.getValue();
		int parallelism = Runtime.getRuntime().availableProcessors();
		System.out.println("parallelism is " + parallelism);
		Path destinationBase = Paths.get(targetPath.getText());
		Service<Void> conversionSErvice = new FfmpegConvertService(tableView.getItems(), ffmpegParams, params,
				parallelism, destinationBase);
		progress.progressProperty().bind(conversionSErvice.progressProperty());
		conversionSErvice.setOnRunning(running -> {
			progress.setDisable(false);
		});
		conversionSErvice.setOnSucceeded(success -> {
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		conversionSErvice.setOnFailed(fail -> {
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		conversionSErvice.start();
	}

	private static final class FfmpegConvertService extends Service<Void> {
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
			return new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					updateProgress(0d, 100d);
					AtomicInteger count = new AtomicInteger(1);
					ExecutorService pool = Executors.newFixedThreadPool(parallelism);
					for(SourceFile sf: items) {
						CompletableFuture.runAsync(() -> convertSingleFile(sf,ffmpegParams, params, destinationBase), pool)
							.thenRun(() -> {
								updateProgress(count.incrementAndGet(), items.size());
								System.out.println("updated total progress");
							});
					}
					return null;
				}
			};
		}
	}
	static void convertSingleFile(SourceFile sf, FfmpegParameters ffmpegParams, ConversionParameters params, Path destinationBase) {
		try {
			System.out.println("item: " + sf);
			sf.progressDetailsProperty().setValue("Converting...");
			File finalPath = Converter.doTheThing(percent -> sf.progressProperty().set(percent),ffmpegParams, params, sf, destinationBase);
			sf.progressDetailsProperty().setValue("Converted to " + finalPath.getAbsolutePath());
			sf.progressProperty().setValue(1d);
			System.out.println("item: " + sf + " 100%");
		} catch (Exception e) {
			System.err.println("Error processing file " + sf);
			e.printStackTrace();
			sf.progressDetailsProperty().set("ERROR: " + e.getClass().getSimpleName() + " : " +e.getMessage());
		}
	}

	@FXML public void openTargetChooser() {
		DirectoryChooser chooser = new DirectoryChooser();
		File destination = chooser.showDialog(targetPath.getScene().getWindow());
		targetPath.textProperty().set(destination.getAbsolutePath());
	}
}
