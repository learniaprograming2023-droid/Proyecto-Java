package com.tutienda.inventario.ui;

import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.FotoService;
import com.tutienda.inventario.util.Alerts;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;

public class FotoCell extends TableCell<Producto, Producto> {
    private final ImageView iv = new ImageView();
    private final Label lbl = new Label("📷 Agregar");

    public FotoCell(TableView<Producto> tabla, InventarioController controller) {
        iv.setFitWidth(90); iv.setFitHeight(55); iv.setPreserveRatio(true);
        lbl.setStyle("-fx-text-fill: #10B981; -fx-font-weight: bold; -fx-cursor: hand; -fx-border-color: #10B981; -fx-border-radius: 6; -fx-padding: 8 12; -fx-background-color: #0d2818;");
        setAlignment(Pos.CENTER);
        setStyle("-fx-background-color: #1e1e1e; -fx-alignment: center;");
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        lbl.setOnMouseClicked(e -> elegirFoto(tabla));
        iv.setOnMouseClicked(e -> {
            Producto p = getItem();
            if(p!=null && p.getFotoRuta()!=null) controller.mostrarFotoGrandePublic(new File(p.getFotoRuta()), p);
        });
    }

    private void elegirFoto(TableView<Producto> tabla){
        Producto p = getItem(); if(p==null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Foto para "+p.getReferencia());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.jpg","*.png","*.jpeg","*.webp"));
        File sel = fc.showOpenDialog(tabla.getScene().getWindow());
        if(sel!=null){
            try{
                String anterior = p.getFotoRuta();
                String nueva = FotoService.guardarFoto(p.getId(), sel);
                new com.tutienda.inventario.dao.ProductoDAO().actualizarFoto(p.getId(), nueva);
                if(anterior!=null) FotoService.borrarFotoSiExiste(anterior);
                p.setFotoRuta(nueva);
                tabla.refresh();
            }catch(Exception ex){ Alerts.error("Error foto", ex.getMessage(), ex); }
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