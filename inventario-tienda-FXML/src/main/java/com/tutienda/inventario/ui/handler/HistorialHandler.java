package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.Alerts;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class HistorialHandler {

    public void mostrarGeneral() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tutienda/inventario/ui/historial_general.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Historial General - Inventario");
            stage.initModality(Modality.APPLICATION_MODAL);

            // Le asignamos ancho 1200 y alto 720
            stage.setScene(new Scene(root, 1200, 720));
            stage.setMinWidth(1100);
            stage.setMinHeight(650);

            stage.showAndWait();

        } catch (IOException e) {
            Alerts.error("Error al abrir historial", "No se pudo cargar la vista del historial general.", e);
        }
    }
    public void mostrarSeleccionado(TableView<Producto> tabla) {
        Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alerts.info("Selección requerida", "Seleccione un producto para consultar su historial de movimientos.");
            return;
        }

        // TODO: Filtro por producto individual en el mismo modal si se requiere
        Alerts.info("Historial de Producto", "Historial de " + seleccionado.getNombre() + " (Ref: " + seleccionado.getReferencia() + ")");
    }
}