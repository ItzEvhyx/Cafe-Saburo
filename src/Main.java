import javafx.application.Application;
import javafx.stage.Stage;
import frontend.auth_ui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends Application {

    public static Connection conn;

    @Override
    public void start(Stage stage) {
        initConnection();
        new auth_ui().start(stage);
    }

    private void initConnection() {
        try {
            String url = "jdbc:sqlserver://localhost:1433;"
                + "databaseName=CAFE_SABURO;"
                + "user=cafe_user;"
                + "password=YourPassword123!;"
                + "trustServerCertificate=true;";
            conn = DriverManager.getConnection(url);
            if (conn != null && !conn.isClosed()) {
                System.out.println("CAFE_SABURO Database Connected!");
            }
        } catch (SQLException e) {
            System.err.println("Connection Failed!\n" + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}