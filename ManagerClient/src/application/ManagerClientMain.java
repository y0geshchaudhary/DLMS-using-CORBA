package application;
	
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class ManagerClientMain extends Application {
	@Override
	public void start(Stage primaryStage) {
		Parent root;
		ManagerClientController controller;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("ManagerClient.fxml"));
			root = loader.load();
			controller = loader.getController();
			
			Scene scene = new Scene(root);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("Manager Client");
			primaryStage.setResizable(false);
			controller.setup();
			primaryStage.show();
		} catch(Exception e) {
			System.out.println("Unable to load ManagerClient.fxml file.");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
