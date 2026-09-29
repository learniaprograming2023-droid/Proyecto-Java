package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.Alerts;
import javafx.scene.control.TableView;

public class CambioHandler {

    private final ProductoDAO productoDAO;

    public CambioHandler(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public void mostrarDialogo(TableView<Producto> tabla) {
        Producto seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alerts.info("Selección requerida", "Seleccione el producto devuelto por el cliente para procesar el cambio.");
            return;
        }

        // AQUÍ: Invocar la ventana emergente/Dialog con los datos de devolución y entrega.
        // Al confirmar, invoca: productoDAO.realizarCambio(origenId, tallaOrigen, destinoId, tallaDestino, cantidad, motivo);
        Alerts.info("Procesar Cambio", "Iniciando flujo de cambio para: " + seleccionado.getReferencia());
    }
}