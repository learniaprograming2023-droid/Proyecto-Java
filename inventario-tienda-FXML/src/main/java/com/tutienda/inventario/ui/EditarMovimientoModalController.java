package com.tutienda.inventario.ui;

import com.tutienda.inventario.dao.MovimientoDAO;
import com.tutienda.inventario.model.Movimiento;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class EditarMovimientoModalController {

    @FXML private Label lblProductoNombre;
    @FXML private Label lblProductoRef;
    @FXML private Button btnCambiarProducto;
    @FXML private ComboBox<Integer> cmbTalla;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private TextField txtPrecio;
    @FXML private Label lblFechaRegistro;
    @FXML private Label lblError;

    private Movimiento movimientoActual;
    private Runnable onGuardarCallback;
    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final DateTimeFormatter formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a");

    @FXML
    public void initialize() {
        cmbMetodoPago.setItems(FXCollections.observableArrayList("Efectivo", "Nequi", "Tarjeta", "Bancolombia", "Sistecredito", "Addi", "Separado"));

        // Aplicar celda personalizada para evitar el fondo blanco en las listas desplegables
        cmbTalla.setCellFactory(lv -> new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-padding: 6 10;");
                }
            }
        });

        cmbTalla.setButtonCell(new ListCell<Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-text-fill: white;");
                }
            }
        });
    }

    public void setMovimiento(Movimiento movimiento, Runnable onGuardarCallback) {
        this.movimientoActual = movimiento;
        this.onGuardarCallback = onGuardarCallback;

        // Datos del producto
        lblProductoNombre.setText(movimiento.getProductoNombre() != null ? movimiento.getProductoNombre() : "Producto Sin Nombre");
        lblProductoRef.setText("Referencia: " + (movimiento.getProductoRef() != null ? movimiento.getProductoRef() : "N/A"));

        // 1. Obtener directamente las tallas con stock desde la base de datos
        List<Integer> tallasDisponibles = movimientoDAO.obtenerTallasDisponibles(
                movimiento.getProductoId(),
                movimiento.getTalla()
        );

        // Asignar los items recuperados
        cmbTalla.getItems().clear();
        cmbTalla.setItems(FXCollections.observableArrayList(tallasDisponibles));

        if (tallasDisponibles.contains(movimiento.getTalla())) {
            cmbTalla.setValue(movimiento.getTalla());
        } else if (!tallasDisponibles.isEmpty()) {
            cmbTalla.setValue(tallasDisponibles.get(0));
        }

        // 2. Cargar campos del movimiento
        txtCantidad.setText(String.valueOf(movimiento.getCantidad()));
        cmbMetodoPago.setValue(movimiento.getMetodoPago() != null ? movimiento.getMetodoPago() : "Efectivo");
        txtPrecio.setText(String.valueOf(movimiento.getPrecio()));

        // 3. Mostrar Fecha y Hora
        if (movimiento.getFecha() != null) {
            lblFechaRegistro.setText(movimiento.getFecha().format(formatoFechaHora));
        } else {
            lblFechaRegistro.setText("N/A");
        }
    }

    @FXML
    private void guardarCambios() {
        lblError.setText("");

        try {
            if (cmbTalla.getValue() == null) {
                lblError.setText("Por favor selecciona una talla.");
                return;
            }

            if (cmbMetodoPago.getValue() == null) {
                lblError.setText("Por favor selecciona un método de pago.");
                return;
            }

            int nuevaTalla = cmbTalla.getValue();

            int nuevaCantidad;
            try {
                nuevaCantidad = Integer.parseInt(txtCantidad.getText().trim());
            } catch (NumberFormatException e) {
                lblError.setText("La cantidad debe ser un número entero.");
                return;
            }

            if (nuevaCantidad <= 0) {
                lblError.setText("La cantidad debe ser mayor a 0.");
                return;
            }

            String textoPrecio = txtPrecio.getText().replaceAll("[^0-9.]", "").trim();
            double nuevoPrecio;
            try {
                nuevoPrecio = Double.parseDouble(textoPrecio);
            } catch (NumberFormatException e) {
                lblError.setText("El precio debe ser un número decimal válido.");
                return;
            }

            String nuevoMetodoPago = cmbMetodoPago.getValue();

            // Ejecutar la actualización con transacción atómica en BD
            boolean exito = movimientoDAO.actualizarMovimiento(
                    movimientoActual.getId(),
                    movimientoActual.getProductoId(),
                    String.valueOf(movimientoActual.getTalla()),
                    nuevaTalla,
                    movimientoActual.getCantidad(),
                    nuevaCantidad,
                    nuevoMetodoPago,
                    nuevoPrecio,
                    movimientoActual.getTipo()
            );

            if (exito) {
                if (onGuardarCallback != null) {
                    onGuardarCallback.run();
                }
                cerrarVentana();
            } else {
                lblError.setText("No se pudo actualizar el registro en la base de datos.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            lblError.setText("Error: " + e.getMessage());
        }
    }

    @FXML
    private void cerrarVentana() {
        Stage stage = (Stage) txtCantidad.getScene().getWindow();
        stage.close();
    }
}