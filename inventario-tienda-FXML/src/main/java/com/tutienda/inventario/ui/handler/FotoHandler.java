package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.Alerts;
import com.tutienda.inventario.util.FotoActions;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

public class FotoHandler {

    private final ProductoDAO productoDAO;

    public FotoHandler(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public void adjuntarFoto(TableView<Producto> tabla, Stage ownerStage) {
        Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alerts.info("Selección requerida", "Por favor, seleccione un producto de la tabla para agregar una foto.");
            return;
        }

        boolean fotoGuardada = FotoActions.seleccionarYGuardar(seleccionado, productoDAO, ownerStage);
        if (fotoGuardada) {
            tabla.refresh();
        }
    }
}