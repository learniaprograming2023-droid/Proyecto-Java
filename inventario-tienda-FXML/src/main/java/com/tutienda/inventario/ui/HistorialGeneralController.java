package com.tutienda.inventario.ui;

import com.tutienda.inventario.dao.MovimientoDAO;
import com.tutienda.inventario.dao.MovimientoDAO.PaginaHistorial;
import com.tutienda.inventario.model.Movimiento;
import com.tutienda.inventario.ui.factory.MovimientoCardFactory;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HistorialGeneralController {

    @FXML private DatePicker dpFechaInicio;
    @FXML private DatePicker dpFechaFin;
    @FXML private TextField txtBuscar;
    @FXML private ToggleButton btnMetodoTodos;
    @FXML private ToggleButton btnMetodoEfectivo;
    @FXML private ToggleButton btnMetodoNequi;
    @FXML private ToggleButton btnMetodoDatafono;
    @FXML private ToggleButton btnMetodoBancolombia;
    @FXML private ToggleButton btnMetodoSistecredito;
    @FXML private ToggleButton btnMetodoAddi;
    @FXML private ToggleButton btnMetodoSeparado;
    @FXML private Label lblResumen;
    @FXML private VBox contenedorBloques;
    @FXML private Pagination paginacion;

    private final MovimientoDAO movimientoDAO = new MovimientoDAO();
    private final ToggleGroup grupoMetodosPago = new ToggleGroup();

    @FXML
    public void initialize() {
        configurarFiltros();

        // Configurar paginación nativa JavaFX
        paginacion.setPageFactory(pageIndex -> {
            cargarDatos(pageIndex);
            return new VBox(); // Elemento ficticio requerido por JavaFX Pagination
        });
    }

    private void configurarFiltros() {
        btnMetodoTodos.setToggleGroup(grupoMetodosPago);
        btnMetodoEfectivo.setToggleGroup(grupoMetodosPago);
        btnMetodoNequi.setToggleGroup(grupoMetodosPago);
        btnMetodoDatafono.setToggleGroup(grupoMetodosPago);
        btnMetodoBancolombia.setToggleGroup(grupoMetodosPago);
        btnMetodoSistecredito.setToggleGroup(grupoMetodosPago);
        btnMetodoAddi.setToggleGroup(grupoMetodosPago);
        btnMetodoSeparado.setToggleGroup(grupoMetodosPago);

        grupoMetodosPago.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                oldVal.setSelected(true);
            } else {
                cargarDatos(paginacion.getCurrentPageIndex());
            }
        });

        dpFechaInicio.valueProperty().addListener((obs, oldV, newV) -> cargarDatos(0));
        dpFechaFin.valueProperty().addListener((obs, oldV, newV) -> cargarDatos(0));
        txtBuscar.textProperty().addListener((obs, oldV, newV) -> cargarDatos(0));
    }

    private void cargarDatos(int pageIndex) {
        LocalDate desde = dpFechaInicio.getValue() != null ? dpFechaInicio.getValue() : LocalDate.now().minusMonths(1);
        LocalDate hasta = dpFechaFin.getValue() != null ? dpFechaFin.getValue() : LocalDate.now();
        String filtroText = txtBuscar.getText();

        ToggleButton seleccionado = (ToggleButton) grupoMetodosPago.getSelectedToggle();
        String metodoPago = (seleccionado != null) ? seleccionado.getText() : "Todos";

        int pageSize = 15;
        PaginaHistorial pagina = movimientoDAO.listarPaginaGeneral(
                filtroText, desde, hasta, "TODOS", metodoPago, null, null, pageSize
        );

        renderizarBloquesDiarios(pagina.movimientos());

        long totalRegistros = pagina.totalCount();
        lblResumen.setText("Mostrando " + pagina.movimientos().size() + " de " + totalRegistros + " registros");

        int pageCount = (int) Math.ceil((double) totalRegistros / pageSize);
        paginacion.setPageCount(pageCount > 0 ? pageCount : 1);
    }

    private void renderizarBloquesDiarios(List<Movimiento> listaMovimientos) {
        contenedorBloques.getChildren().clear();

        if (listaMovimientos.isEmpty()) {
            Label lblVacio = new Label("No se encontraron registros de movimientos para el filtro seleccionado.");
            lblVacio.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-padding: 20;");
            contenedorBloques.getChildren().add(lblVacio);
            return;
        }

        // Agrupar por fecha manteniendo el orden descendente
        Map<LocalDate, List<Movimiento>> agrupados = listaMovimientos.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getFecha().toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        String[] coloresCabecera = {
                "-fx-background-color: #0369a1;", // Azul primario
                "-fx-background-color: #0f766e;", // Verde azulado
                "-fx-background-color: #4338ca;"  // Morado profundo
        };
        int colorIdx = 0;

        for (Map.Entry<LocalDate, List<Movimiento>> entry : agrupados.entrySet()) {
            VBox tarjetaDia = MovimientoCardFactory.crearTarjetaDia(
                    entry.getKey(),
                    entry.getValue(),
                    coloresCabecera[colorIdx % coloresCabecera.length],
                    this::abrirModalEditar
            );
            contenedorBloques.getChildren().add(tarjetaDia);
            colorIdx++;
        }
    }

    private void abrirModalEditar(Movimiento movimiento) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/tutienda/inventario/ui/modal_editar_movimiento.fxml"));
            Parent root = loader.load();

            EditarMovimientoModalController controller = loader.getController();
            controller.setMovimiento(movimiento, () -> cargarDatos(paginacion.getCurrentPageIndex()));

            Stage stage = new Stage();
            stage.setTitle("Editar Movimiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Error al abrir el modal de edición: " + e.getMessage());
            alert.showAndWait();
        }
    }
}