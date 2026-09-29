package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.exception.ValidationException;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.service.InventarioService;
import com.tutienda.inventario.util.Alerts;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class FormHandler {

    private final InventarioService inventarioService;

    public FormHandler(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    public boolean guardarProducto(TextField txtRef, TextField txtNombre, ComboBox<Marca> cbMarcas) {
        try {
            Marca m = cbMarcas.getValue();
            if (m == null) {
                throw new ValidationException("Por favor seleccione una marca.");
            }

            String ref = txtRef.getText();
            String nom = txtNombre.getText();

            inventarioService.crearProducto(ref, nom, m.getId());

            txtRef.clear();
            txtNombre.clear();
            Alerts.info("Éxito", "Producto registrado correctamente.");
            return true;
        } catch (ValidationException ve) {
            Alerts.info("Validación", ve.getMessage());
        } catch (Exception ex) {
            Alerts.error("Error", "No se pudo crear el producto.", ex);
        }
        return false;
    }

    public void guardarMarca(TextField txtNuevaMarca, Runnable onMarcaGuardadaCallback) {
        try {
            String nueva = txtNuevaMarca.getText();
            inventarioService.crearMarca(nueva);
            txtNuevaMarca.clear();

            Alerts.info("Éxito", "Marca guardada con éxito.");

            if (onMarcaGuardadaCallback != null) {
                onMarcaGuardadaCallback.run();
            }
        } catch (ValidationException ve) {
            Alerts.info("Validación", ve.getMessage());
        } catch (Exception ex) {
            Alerts.error("Error", "Error al guardar marca.", ex);
        }
    }

}