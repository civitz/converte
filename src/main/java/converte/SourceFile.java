package converte;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

import java.nio.file.Path;

public class SourceFile {
	private SimpleStringProperty name = new SimpleStringProperty("","name");
	private SimpleStringProperty filename = new SimpleStringProperty("","filename");
	private SimpleDoubleProperty progress = new SimpleDoubleProperty(0L, "progress");
	private SimpleStringProperty progressDetails = new SimpleStringProperty("","progressDetails");
	private SimpleStringProperty basePath = new SimpleStringProperty("", "basePath");

	public SourceFile() {
	}

	public SourceFile(String name, String filename, double progress, String progressDetails) {
		super();
		this.name.setValue(name);
		this.filename.setValue(filename);
		this.progress.setValue(progress);
		this.progressDetails.setValue(progressDetails);
	}

	public SimpleStringProperty nameProperty() {
		return name;
	}

	public SimpleStringProperty filenameProperty() {
		return filename;
	}

	public SimpleDoubleProperty progressProperty() {
		return progress;
	}

	public SimpleStringProperty progressDetailsProperty() {
		return progressDetails;
	}
	
	public SimpleStringProperty basePathProperty() {
		return basePath;
	}

	public SourceFile withBasePath(String basePath) {
		this.basePath.setValue(basePath);
		return this;
	}

	public static SourceFile fromPath(Path p) {
		String name = p.getFileName().toString();
		String filename = p.toAbsolutePath().toString();
		return new SourceFile(name, filename, 0, "");
	}

	@Override
	public String toString() {
		return "SourceFile [name=" + name + ", filename=" + filename + ", progress=" + progress + ", progressDetails="
				+ progressDetails + ", basePath=" + basePath + "]";
	}
}
