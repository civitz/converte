package converte;

import java.awt.image.SampleModel;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
				tableView.getItems().removeAll(selectedItems);
			}
		});
		
	}
	
	private void onFrequencyChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		System.out.println(String.format("frequencyList: Observable %s, before %s,after %s", observable, before,after));
		// if after is not null, deselect presets
		if(after!=null) {
			presetList.getSelectionModel().clearSelection();
			SingleSelectionModel<Integer> selectedBitrate = bitrateList.getSelectionModel();
			if(selectedBitrate.isEmpty()) {
				selectedBitrate.select(0);
			}
		}
	}

	private void onBitrateChanged(ObservableValue<? extends Integer> observable, Integer before, Integer after) {
		System.out.println(String.format("bitrateList: Observable %s, before %s,after %s", observable, before,after));
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
		System.out.println(String.format("presetList: Observable %s, before %s,after %s", observable, before,after));
		if(after != null) {
			//deselect from bitrate and sample rate
			bitrateList.getSelectionModel().clearSelection();
			frequencyList.getSelectionModel().clearSelection();
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
		// TODO: validate presence of all parameters
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

	@FXML public void openTargetChooser() {
		DirectoryChooser chooser = new DirectoryChooser();
		File destination = chooser.showDialog(targetPath.getScene().getWindow());
		targetPath.textProperty().set(destination.getAbsolutePath());
	}
}
