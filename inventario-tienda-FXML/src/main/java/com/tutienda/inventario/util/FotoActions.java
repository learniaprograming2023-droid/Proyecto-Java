package com.tutienda.inventario.util;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.FotoService;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/** Operaciones de selección y reemplazo de fotos de productos. */
public class FotoActions {
    private FotoActions() {
    }

    public static boolean seleccionarYGuardar(Producto producto, ProductoDAO productoDAO, Window owner) {
        FileChooser selector = new FileChooser();
        selector.setTitle("Foto - " + producto.getReferencia());
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.webp"));
        File archivo = selector.showOpenDialog(owner);
        if (archivo == null) {
            return false;
        }

        try {
            String rutaAnterior = producto.getFotoRuta();
            String rutaNueva = FotoService.guardarFoto(producto.getId(), archivo);
            productoDAO.actualizarFoto(producto.getId(), rutaNueva);
            producto.setFotoRuta(rutaNueva);
            FotoService.borrarFotoSiExiste(rutaAnterior);
            return true;
        } catch (Exception e) {
            Alerts.error("Error de imagen", "No se pudo guardar la imagen del producto.", e);
            return false;
        }
    }
}