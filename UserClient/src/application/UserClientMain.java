package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class UserClientMain extends Application {
	@Override
	public void start(Stage primaryStage) {
		Parent root;
		UserClientController controller;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("UserClient.fxml"));
			root = loader.load();
			controller = loader.getController();
			
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("User Client");
			primaryStage.setResizable(false);
			controller.setup();
			primaryStage.show();
		} catch(Exception e) {
			System.out.println("Unable to load UserClient.fxml file.");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
