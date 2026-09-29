package com.tutienda.inventario.ui.cell;

import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.FotoService;
import com.tutienda.inventario.util.FotoActions;
import com.tutienda.inventario.ui.InventarioController;
import com.tutienda.inventario.ui.dialog.InventarioDialogs;
import com.tutienda.inventario.util.Alerts;
import com.tutienda.inventario.dao.ProductoDAO;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import java.io.File;

public class FotoCell extends TableCell<Producto, Producto> {
    private final ImageView iv = new ImageView();
    private final Label lbl = new Label("📷 Agregar");
    private final ProductoDAO productoDAO = new ProductoDAO();

    public FotoCell(TableView<Producto> tabla, InventarioController controller) {
        iv.setFitWidth(90); iv.setFitHeight(55); iv.setPreserveRatio(true);
        lbl.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #10B981; -fx-border-radius: 6; -fx-padding: 8 12; -fx-background-color: #0d2818;");
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1e1e1e; -fx-alignment: center;");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        lbl.setOnMouseClicked(e -> elegirFoto(tabla));
        iv.setOnMouseClicked(e -> {
            Producto p = getItem();
            // CÓDIGO CORREGIDO:
            if (p != null && p.getFotoRuta() != null) {
                InventarioDialogs.mostrarFotoGrande(new File(p.getFotoRuta()), p);
            }
        });
    }
    private void elegirFoto(TableView<Producto> tabla){
        Producto p = getItem(); if(p==null) return;
        try {
            if (FotoActions.seleccionarYGuardar(p, productoDAO, tabla.getScene().getWindow())) {
                tabla.refresh();
            }
        } catch (Exception ex) {
            Alerts.error("Error foto", ex.getMessage(), ex);
        }
    }

    @Override protected void updateItem(Producto p, boolean empty){
        super.updateItem(p, empty);
        if(empty || p==null){ setGraphic(null); return; }
        String ruta = p.getFotoRuta();
        if(ruta==null || ruta.isBlank() || !new File(ruta).exists()){
            setGraphic(lbl);
        }else{
            iv.setImage(FotoService.loadImage(ruta, 90, 55));
            setGraphic(iv);
        }
    }
}
