import javafx.application.Application;
import javafx.stage.Stage;
import frontend.auth_ui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends Application {

    // ── Shared DB connection — accessible anywhere via Main.conn ──
    public static Connection conn;

    @Override
    public void start(Stage stage) {
        initConnection();
        new auth_ui(conn).start(stage);
    }

    private void initConnection() {
        try {
            String url = "jdbc:sqlserver://localhost:1433;"
                + "databaseName=CAFE_SABURO;"
                + "user=cafe_user;"
                + "password=YourPassword123!;"
                + "encrypt=false;"               // avoids SSL handshake issues on local dev
                + "trustServerCertificate=true;";

            conn = DriverManager.getConnection(url);

            if (conn != null && !conn.isClosed()) {
                System.out.println("[Main] CAFE_SABURO database connected successfully.");
            }

        } catch (SQLException e) {
            System.err.println("[Main] Connection failed: " + e.getMessage());
            e.printStackTrace(System.err);
            conn = null;   // make sure it stays null so callers can detect failure
        }
    }

    /** Called automatically by JavaFX when the app closes. */
    @Override
    public void stop() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("[Main] DB connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("[Main] Error closing connection: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}