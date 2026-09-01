package com.tutienda.inventario;

import com.tutienda.inventario.dao.ConexionDB;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {

        ConexionDB.getConnection();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tutienda/inventario/inventario.fxml"));
        Scene scene = new Scene(loader.load(), 1200, 700);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        stage.setTitle("Inventario Tienda - Como cuaderno pero digital");
        stage.setScene(scene);
        stage.show();
    }
    public static void main(String[] args){ launch(); }
}
