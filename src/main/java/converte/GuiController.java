package converte;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import converte.files.SimpleFileRecursiveFinder;
import converte.utils.OsUtils;
import javafx.application.Application.Parameters;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.ProgressBarTableCell;
import javafx.scene.control.cell.PropertyValueFactory;

public class GuiController implements Initializable {
	
	private final Logger logger = LoggerFactory.getLogger(GuiController.class);

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
		logger.debug("initialize");
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
				ConversionParameters.of("Low", 11025, 56*1024),
				ConversionParameters.of("Medium", 44100, 128*1024),
				ConversionParameters.of("High", 44100, 192*1024),
				ConversionParameters.of("CD", 44100, 320*1024)
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
		tableView.setOnKeyPressed(keyEvent -> {
			if(keyEvent.getCode().equals(KeyCode.DELETE)) {
				ObservableList<SourceFile> selectedItems = tableView.getSelectionModel().getSelectedItems();
				logger.debug("Deleting selected items {}", selectedItems);
				tableView.getItems().removeAll(selectedItems);
			}
		});
	}
	
	private void onFrequencyChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		logger.debug("frequencyList: Observable {}, before {},after {}", observable, before, after);
		// if after is not null, deselect presets
		if(after!=null) {
			logger.debug("deselecting presets");
			presetList.getSelectionModel().clearSelection();
			SingleSelectionModel<Integer> selectedBitrate = bitrateList.getSelectionModel();
			if(selectedBitrate.isEmpty()) {
				logger.debug("pre-selecting bitrate");
				selectedBitrate.select(0);
			}
		}
	}

	private void onBitrateChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		// if after is not null, deselect presets
		if(after!=null) {
			presetList.getSelectionModel().clearSelection();
			SingleSelectionModel<Integer> selectedFrequency = frequencyList.getSelectionModel();
			if(selectedFrequency.isEmpty()) {
				selectedFrequency.select(0);
			}
		}
	}

	private void onPresetChanged(ObservableValue<? extends ConversionParameters> observable, ConversionParameters before,
			ConversionParameters after) {
		if(after != null) {
			//deselect from bitrate and sample rate
			bitrateList.getSelectionModel().clearSelection();
			frequencyList.getSelectionModel().clearSelection();
		}
	}
	
	public void setParameters(Parameters parameters) {
		for (Entry<String, String> entry : parameters.getNamed().entrySet()) {
			switch (entry.getKey()) {
			case "ffmpegPath":
				ffmpegParams = ffmpegParams.withFfmpegPath(entry.getValue());
				break;
			case "ffprobePath":
				ffmpegParams= ffmpegParams.withFfprobePath(entry.getValue());
				break;
			default: {
				logger.debug("Ignoring parameter " + entry.getKey());
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
		Dragboard board = de.getDragboard();
		List<File> phil = board.getFiles();
		Stream<SourceFile> sourceFiles = SimpleFileRecursiveFinder.findRecursively(phil);
		sourceFiles
			.peek(el -> logger.info("Adding file {}", el))
			.forEach(tableView.getItems()::add);

		pathCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/25));
		nameCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/30));
		progressCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/15));
		detailsCol.prefWidthProperty().bind(tableView.widthProperty().divide(100/20));
	}

	@FXML
	protected void convertAction(ActionEvent event) {
		// TODO: validate presence of all parameters
		
		
		convertButton.setDisable(true);
		Optional<ConversionParameters> maybeParams = Optional.ofNullable(presetList.getValue());
		logger.info("Conversion parameters: {}", maybeParams);
		int parallelism = Runtime.getRuntime().availableProcessors();
		logger.info("parallelism is {}", parallelism);
		Path destinationBase = Paths.get(targetPath.getText());
		Service<Void> conversionService = new FfmpegConvertService(tableView.getItems(), ffmpegParams, maybeParams.get(),
				parallelism, destinationBase);
		progress.progressProperty().bind(conversionService.progressProperty());
		conversionService.setOnRunning(running -> {
			progress.setDisable(false);
		});
		conversionService.setOnSucceeded(success -> {
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		conversionService.setOnFailed(fail -> {
			convertButton.setDisable(false);
			progress.setDisable(true);
		});
		conversionService.start();
	}

	@FXML public void openTargetChooser() {
		DirectoryChooser chooser = new DirectoryChooser();
		File destination = chooser.showDialog(targetPath.getScene().getWindow());
		targetPath.textProperty().set(destination.getAbsolutePath());
	}
}
