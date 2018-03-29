package converte;

import java.nio.file.Path;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

public class SourceFile {
	private SimpleStringProperty name = new SimpleStringProperty("");
	private SimpleStringProperty filename = new SimpleStringProperty("");
	private SimpleDoubleProperty progress = new SimpleDoubleProperty(0L);
	private SimpleStringProperty progressDetails = new SimpleStringProperty("");

	public SourceFile() {
	}

	public SourceFile(String name, String filename, double progress, String progressDetails) {
		super();
		this.name.setValue(name);
		this.filename.setValue(filename);
		this.progress.setValue(progress);
		this.progressDetails.setValue(progressDetails);
	}

	public String getName() {
		return name.getValue();
	}

	public String getFilename() {
		return filename.getValue();
	}

	public Double getProgress() {
		return progress.getValue();
	}

	public String getProgressDetails() {
		return progressDetails.getValue();
	}

	public SimpleStringProperty getNameProperty() {
		return name;
	}

	public SimpleStringProperty getFilenameProperty() {
		return filename;
	}

	public SimpleDoubleProperty getProgressProperty() {
		return progress;
	}

	public SimpleStringProperty getProgressDetailsProperty() {
		return progressDetails;
	}

	public static SourceFile fromPath(Path p) {
		String name = p.getFileName().toString();
		String filename = p.toAbsolutePath().toString();
		return new SourceFile(name, filename, 0, "");
	}

	@Override
	public String toString() {
		return "SourceFile [name=" + name + ", filename=" + filename + ", progress=" + progress + ", progressDetails="
				+ progressDetails + "]";
	}
}
