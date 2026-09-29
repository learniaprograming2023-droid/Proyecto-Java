package com.tutienda.inventario.ui.dialog;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.Alerts;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Diálogo para registrar un cambio de talla o modelo. */
final class CambioDialog {
    private final ProductoDAO productoDAO;

    CambioDialog(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    boolean mostrar(Producto origen) {
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Cambio");
        DialogPane panel = dialogo.getDialogPane();
        DialogStyles.applyDarkTheme(panel, CambioDialog.class);
        panel.setStyle("-fx-background-color: #1e1e1e;");

        ComboBox<Producto> destino = crearSelectorProducto();
        ComboBox<Integer> tallaOrigen = new ComboBox<>(FXCollections.observableArrayList(
                origen.getTallas().keySet().stream().sorted().toList()));
        ComboBox<Integer> tallaDestino = new ComboBox<>();
        Label stockDestino = new Label();
        destino.valueProperty().addListener((obs, anterior, producto) -> {
            if (producto == null) return;
            tallaDestino.setItems(FXCollections.observableArrayList(producto.getTallas().keySet().stream().sorted().toList()));
            tallaDestino.valueProperty().addListener((o, a, talla) -> {
                if (talla != null) stockDestino.setText("Stock destino T" + talla + ": " + producto.getTallas().getOrDefault(talla, 0));
            });
        });
        TextField motivo = new TextField();
        motivo.setPromptText("No le quedó");
        VBox contenido = new VBox(10, new Label("DEVUELVE:"),
                new Label(origen.getMarcaNombre() + " " + origen.getReferencia()),
                new Label("Talla que devuelve:"), tallaOrigen, new Separator(),
                new Label("LLEVA:"), destino, new Label("Talla que lleva:"), tallaDestino,
                stockDestino, new Label("Motivo:"), motivo);
        contenido.setStyle("-fx-background-color: #1e1e1e;");
        panel.setContent(contenido);
        panel.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return false;
        if (destino.getValue() == null || tallaOrigen.getValue() == null || tallaDestino.getValue() == null) {
            Alerts.error("Error cambio", "Completa todos los campos", null);
            return false;
        }
        try {
            productoDAO.realizarCambio(origen.getId(), tallaOrigen.getValue(), destino.getValue().getId(),
                    tallaDestino.getValue(), 1, motivo.getText());
            Alerts.info("Cambio realizado", "Cambio OK");
            return true;
        } catch (Exception error) {
            Alerts.error("Error cambio", error.getMessage(), error);
            return false;
        }
    }

    private ComboBox<Producto> crearSelectorProducto() {
        ComboBox<Producto> selector = new ComboBox<>(FXCollections.observableArrayList(productoDAO.listar("", 0, 200)));
        selector.setCellFactory(lista -> new ListCell<>() {
            @Override protected void updateItem(Producto producto, boolean empty) {
                super.updateItem(producto, empty);
                setText(empty || producto == null ? null : producto.getMarcaNombre() + " - " + producto.getReferencia() + " " + producto.getNombre());
            }
        });
        selector.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Producto producto, boolean empty) {
                super.updateItem(producto, empty);
                setText(empty || producto == null ? "Selecciona producto que lleva" : producto.getMarcaNombre() + " " + producto.getReferencia());
            }
        });
        return selector;
    }
}
