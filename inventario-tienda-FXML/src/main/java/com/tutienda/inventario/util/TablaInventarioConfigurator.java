package com.tutienda.inventario.util;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.InventarioService;
import com.tutienda.inventario.ui.dialog.InventarioDialogs;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.File;

public class TablaInventarioConfigurator {

    public static void configurar(
            TableView<Producto> tabla,
            InventarioService inventarioService,
            Runnable onRefreshRequired
    ) {
        tabla.getColumns().clear();

        // 1. Columna Marca / Referencia
        TableColumn<Producto, String> colRef = new TableColumn<>("MARCA / REFERENCIA");
        colRef.setPrefWidth(285);
        colRef.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMarcaNombre() + " - " + c.getValue().getReferencia() + "\n" + c.getValue().getNombre()
        ));
        colRef.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #ffffff; -fx-font-size: 13px; -fx-font-weight: bold; -fx-cursor: hand; -fx-line-spacing: 1.5;");
                }
            }
        });
        tabla.getColumns().add(colRef);

        // 2. Columnas Dinámicas para Tallas (34 a 45)
        for (int t = 34; t <= 45; t++) {
            final int talla = t;
            TableColumn<Producto, Integer> colTalla = new TableColumn<>(String.valueOf(talla));
            colTalla.setPrefWidth(50);
            colTalla.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTallas().getOrDefault(talla, 0)));
            colTalla.setCellFactory(c -> new TableCell<>() {
                {
                    setAlignment(Pos.CENTER);
                    setOnMouseClicked(e -> {
                        if (getTableRow() != null && !isEmpty()) {
                            Producto p = getTableRow().getItem();
                            if (p != null) {
                                InventarioDialogs.mostrarDialogoMovimiento(p, talla, inventarioService, onRefreshRequired);
                            }
                        }
                    });
                }
                @Override protected void updateItem(Integer cant, boolean empty) {
                    super.updateItem(cant, empty);
                    if (empty || cant == null) {
                        setText(null); setStyle("");
                    } else if (cant == 0) {
                        setText("—");
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #3a3a3a; -fx-font-weight: bold; -fx-alignment: center;");
                        setCursor(Cursor.HAND);
                    } else {
                        setText(String.valueOf(cant));
                        setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: 900; -fx-alignment: center; -fx-background-radius: 6;");
                        setCursor(Cursor.HAND);
                    }
                }
            });
            tabla.getColumns().add(colTalla);
        }

        // 3. Columna Total
        TableColumn<Producto, Integer> colTotal = new TableColumn<>("TOTAL");
        colTotal.setPrefWidth(72);
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(Integer total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null); setStyle("");
                } else {
                    setText(String.valueOf(total));
                    if (total == 0) setStyle("-fx-text-fill: #555; -fx-alignment: center;");
                    else if (total <= 3) setStyle("-fx-background-color: #F59E0B; -fx-text-fill: black; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 10;");
                    else setStyle("-fx-background-color: #065F46; -fx-text-fill: #6EE7B7; -fx-font-weight: bold; -fx-alignment: center; -fx-background-radius: 10;");
                }
            }
        });
        tabla.getColumns().add(colTotal);

        // 4. Columna Foto
        TableColumn<Producto, Producto> colFoto = new TableColumn<>("FOTO");
        colFoto.setPrefWidth(110);
        colFoto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colFoto.setCellFactory(column -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            private final Label lblAdd = new Label("📷 Agregar");
            {
                iv.setFitWidth(80); iv.setFitHeight(50); iv.setPreserveRatio(true);
                lblAdd.setStyle("-fx-text-fill: #888; -fx-font-size: 11px; -fx-cursor: hand;");
            }
            @Override protected void updateItem(Producto p, boolean empty) {
                super.updateItem(p, empty);
                if (empty || p == null) { setGraphic(null); return; }

                String ruta = p.getFotoRuta();
                if (ruta == null || ruta.isBlank()) {
                    setGraphic(lblAdd);
                    setOnMouseClicked(e -> {
                        if (FotoActions.seleccionarYGuardar(p, new ProductoDAO(), getScene().getWindow())) {
                            tabla.refresh();
                        }
                    });
                } else {
                    File f = new File(ruta);
                    if (f.exists()) {
                        Image cached = ImageCache.getInstance().get(f);
                        iv.setImage(cached);
                        setGraphic(iv);
                        setOnMouseClicked(e -> InventarioDialogs.mostrarFotoGrande(f, p));
                    } else {
                        setGraphic(lblAdd);
                    }
                }
            }
        });
        tabla.getColumns().add(colFoto);
    }
}