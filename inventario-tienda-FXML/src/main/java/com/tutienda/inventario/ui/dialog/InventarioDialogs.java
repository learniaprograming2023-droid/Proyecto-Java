package com.tutienda.inventario.ui.dialog;

import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.InventarioService;
import com.tutienda.inventario.util.Alerts;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class InventarioDialogs {

    public static void mostrarDialogoMovimiento(Producto p, int talla, InventarioService service, Runnable onSuccess) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Gestión de Stock - Talla " + talla);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1e1e1e;");

        Label lblTitle = new Label(p.getMarcaNombre() + " - " + p.getNombre());
        lblTitle.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");

        Label lblSub = new Label("REF: " + p.getReferencia() + " | Stock actual T" + talla + ": " + p.getTallas().getOrDefault(talla, 0));
        lblSub.setStyle("-fx-text-fill: #aaa; -fx-font-size: 12px;");

        ComboBox<String> cbTipo = new ComboBox<>();
        cbTipo.getItems().addAll("ENTRADA", "SALIDA");
        cbTipo.setValue("ENTRADA");

        Spinner<Integer> spCantidad = new Spinner<>(1, 500, 1);
        spCantidad.setEditable(true);

        TextField txtObs = new TextField();
        txtObs.setPromptText("Observación / Motivo");

        Button btnGuardar = new Button("Confirmar Movimiento");
        btnGuardar.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        btnGuardar.setOnAction(e -> {
            try {
                service.registrarMovimiento(p.getId(), talla, cbTipo.getValue(), spCantidad.getValue(), txtObs.getText());
                stage.close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Alerts.error("Error al registrar", ex.getMessage(), ex);
            }
        });

        root.getChildren().addAll(lblTitle, lblSub, new Label("Tipo:"), cbTipo, new Label("Cantidad:"), spCantidad, new Label("Observación:"), txtObs, btnGuardar);

        Scene scene = new Scene(root, 360, 320);
        stage.setScene(scene);
        stage.showAndWait();
    }

    public static void mostrarFotoGrande(File archivo, Producto p) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(p.getMarcaNombre() + " - " + p.getReferencia());

        ImageView iv = new ImageView(new Image(archivo.toURI().toString()));
        iv.setFitWidth(600);
        iv.setFitHeight(450);
        iv.setPreserveRatio(true);

        VBox layout = new VBox(iv);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-background-color: #111;");

        Scene scene = new Scene(layout, 620, 470);
        stage.setScene(scene);
        stage.show();
    }
}