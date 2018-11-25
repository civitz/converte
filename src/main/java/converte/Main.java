package converte;

import java.io.IOException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {

		FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/gui.fxml"));
		Parent root = fxmlLoader.load();
		GuiController controller = fxmlLoader.getController();
		controller.setParameters(getParameters());
		Scene scene = new Scene(root, 800, 600);
		primaryStage.setTitle("Converte");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(windowEvent -> {
			Platform.exit();
			System.exit(0);
		});
		primaryStage.show();
		

	}

	public static void main(String[] args) throws IOException {
		launch(args);
	}

}
