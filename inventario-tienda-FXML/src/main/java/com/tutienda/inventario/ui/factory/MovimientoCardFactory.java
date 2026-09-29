package com.tutienda.inventario.ui.factory;

import com.tutienda.inventario.model.Movimiento;
import com.tutienda.inventario.util.FormatUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

public class MovimientoCardFactory {

    /**
     * Construye la tarjeta contenedora para un grupo de movimientos de un mismo día.
     */
    public static VBox crearTarjetaDia(LocalDate fechaDia, List<Movimiento> movs, String colorHeader, Consumer<Movimiento> onEditarCallback) {
        VBox tarjetaDia = new VBox(0);
        tarjetaDia.setStyle("-fx-background-color: #1e293b; -fx-background-radius: 8px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 5, 0, 0, 2);");

        // 1. Cabecera del Día
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 15, 10, 15));
        header.setStyle(colorHeader + " -fx-background-radius: 8px 8px 0 0;");

        Label lblFecha = new Label("📅  " + FormatUtils.formatoDiaHeader(fechaDia));
        lblFecha.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        double totalDia = movs.stream().mapToDouble(m -> m.getPrecio() * m.getCantidad()).sum();
        Label lblTotalDia = new Label("Total del día: " + FormatUtils.formatoMoneda(totalDia));
        lblTotalDia.setStyle("-fx-text-fill: #ffffff; -fx-font-weight: bold; -fx-font-size: 13px;");

        header.getChildren().addAll(lblFecha, spacer, lblTotalDia);
        tarjetaDia.getChildren().add(header);

        // 2. Filas internas de movimientos
        for (int i = 0; i < movs.size(); i++) {
            Movimiento m = movs.get(i);
            HBox fila = crearFilaMovimiento(m, onEditarCallback);

            if (i % 2 == 0) {
                fila.setStyle("-fx-background-color: #1e293b; -fx-padding: 8 15 8 15; -fx-alignment: CENTER_LEFT;");
            } else {
                fila.setStyle("-fx-background-color: #0f172a; -fx-padding: 8 15 8 15; -fx-alignment: CENTER_LEFT;");
            }
            tarjetaDia.getChildren().add(fila);
        }

        return tarjetaDia;
    }

    /**
     * Construye la fila/tarjeta individual para un movimiento.
     */
    public static HBox crearFilaMovimiento(Movimiento m, Consumer<Movimiento> onEditarCallback) {
        HBox fila = new HBox(15);
        fila.setAlignment(Pos.CENTER_LEFT);

        // 1. Hora
        Label lblHora = new Label(FormatUtils.formatoHora(m.getFecha()));
        lblHora.setPrefWidth(90);
        lblHora.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        // 2. Miniatura + Nombre y Referencia
        ImageView imgView = new ImageView();
        imgView.setFitWidth(32);
        imgView.setFitHeight(32);
        imgView.setPreserveRatio(true);
        try {
            imgView.setImage(new Image(MovimientoCardFactory.class.getResourceAsStream("/com/tutienda/inventario/img/shoe_placeholder.png")));
        } catch (Exception ignored) {}

        VBox boxNombre = new VBox(2);
        Label lblNombre = new Label(m.getProductoNombre() != null ? m.getProductoNombre() : "Sin Nombre");
        lblNombre.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 12px;");
        Label lblRef = new Label(m.getProductoRef() != null ? m.getProductoRef() : "N/A");
        lblRef.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        boxNombre.getChildren().addAll(lblNombre, lblRef);

        HBox boxProducto = new HBox(10, imgView, boxNombre);
        boxProducto.setAlignment(Pos.CENTER_LEFT);
        boxProducto.setPrefWidth(260);

        // 3. Talla
        Label lblTalla = new Label(String.valueOf(m.getTalla()));
        lblTalla.setPrefWidth(50);
        lblTalla.setAlignment(Pos.CENTER);
        lblTalla.setStyle("-fx-text-fill: #e2e8f0; -fx-font-weight: bold;");

        // 4. Cantidad
        Label lblCant = new Label(String.valueOf(m.getCantidad()));
        lblCant.setPrefWidth(50);
        lblCant.setAlignment(Pos.CENTER);
        lblCant.setStyle("-fx-text-fill: #cbd5e1;");

        // 5. Tipo (VENTA, ENTRADA, SALIDA)
        String tipoStr = m.getTipo() != null ? m.getTipo().toUpperCase() : "VENTA";
        Label lblTipo = new Label(tipoStr);
        lblTipo.setPrefWidth(80);
        lblTipo.setAlignment(Pos.CENTER);
        String colorTipo = switch (tipoStr) {
            case "VENTA" -> "-fx-text-fill: #10b981; -fx-font-weight: bold;";
            case "ENTRADA" -> "-fx-text-fill: #3b82f6; -fx-font-weight: bold;";
            case "SALIDA" -> "-fx-text-fill: #ef4444; -fx-font-weight: bold;";
            default -> "-fx-text-fill: #94a3b8;";
        };
        lblTipo.setStyle(colorTipo);

        // 6. Badge Método de Pago
        Label badgePago = new Label(m.getMetodoPago() != null ? m.getMetodoPago() : "Efectivo");
        String bgStyle = switch (badgePago.getText().toLowerCase()) {
            case "efectivo" -> "-fx-background-color: #065f46; -fx-text-fill: #34d399;";
            case "nequi" -> "-fx-background-color: #311b92; -fx-text-fill: #b388ff;";
            case "datafono", "tarjeta" -> "-fx-background-color: #1e3a8a; -fx-text-fill: #60a5fa;";
            case "bancolombia" -> "-fx-background-color: #78350f; -fx-text-fill: #fcd34d;";
            case "sistecredito", "addi" -> "-fx-background-color: #581c87; -fx-text-fill: #e9d5ff;";
            default -> "-fx-background-color: #334155; -fx-text-fill: #cbd5e1;";
        };
        badgePago.setStyle(bgStyle + " -fx-padding: 3 10 3 10; -fx-background-radius: 12px; -fx-font-size: 11px; -fx-font-weight: bold;");
        badgePago.setPrefWidth(120);
        badgePago.setAlignment(Pos.CENTER);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 7. Precio
        Label lblPrecio = new Label(FormatUtils.formatoMoneda(m.getPrecio()));
        lblPrecio.setPrefWidth(100);
        lblPrecio.setAlignment(Pos.CENTER_RIGHT);
        lblPrecio.setStyle("-fx-text-fill: #f8fafc; -fx-font-weight: bold; -fx-font-size: 13px;");

        // 8. Botón Editar
        Button btnEditar = new Button("Editar");
        btnEditar.setPrefWidth(75);
        btnEditar.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-cursor: hand; -fx-font-size: 15px; -fx-background-radius: 4px;");
        btnEditar.setOnMouseEntered(e -> btnEditar.setStyle("-fx-background-color: #0284c7; -fx-text-fill: #ffffff; -fx-cursor: hand; -fx-font-size: 15px; -fx-background-radius: 4px;"));
        btnEditar.setOnMouseExited(e -> btnEditar.setStyle("-fx-background-color: #334155; -fx-text-fill: #f8fafc; -fx-cursor: hand; -fx-font-size: 15px; -fx-background-radius: 4px;"));

        btnEditar.setOnAction(e -> {
            if (onEditarCallback != null) {
                onEditarCallback.accept(m);
            }
        });

        HBox boxAccion = new HBox(btnEditar);
        boxAccion.setPrefWidth(75);
        boxAccion.setAlignment(Pos.CENTER);

        fila.getChildren().addAll(lblHora, boxProducto, lblTalla, lblCant, lblTipo, badgePago, spacer, lblPrecio, boxAccion);
        return fila;
    }
}