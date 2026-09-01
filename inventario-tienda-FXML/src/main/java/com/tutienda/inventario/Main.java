package com.tutienda.inventario;

import com.tutienda.inventario.dao.ConexionDB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        ConexionDB.getConnection();

        // FIX QODANA 1: FXML con null-check Senior
        var fxmlUrl = getClass().getResource("/com/tutienda/inventario/inventario.fxml");
        Objects.requireNonNull(fxmlUrl, "No se encontró inventario.fxml en resources");
        FXMLLoader loader = new FXMLLoader(fxmlUrl);

        Scene scene = new Scene(loader.load(), 1200, 700);

        // FIX QODANA 2: CSS con null-check Senior - este es el de la línea 18
        var cssUrl = getClass().getResource("/css/dark-theme.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        } else {
            System.err.println("Advertencia: /css/dark-theme.css no encontrado, usando estilos por defecto");
        }

        stage.setTitle("Inventario Tienda - Como cuaderno pero digital");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args){ launch(); }
}