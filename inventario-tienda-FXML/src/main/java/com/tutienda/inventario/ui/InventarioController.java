package com.tutienda.inventario.ui;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.ui.handler.*;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.InventarioService;

import com.tutienda.inventario.util.TablaInventarioConfigurator;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public class InventarioController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Producto> tabla;
    @FXML private Pagination pagination;
    @FXML private ComboBox<Marca> cbMarcas;
    @FXML private TextField txtRef, txtNombre, txtNuevaMarca;
    @FXML private Label lblInfo;
    @FXML private Label lblPaginaInfo;
    @FXML private ProgressIndicator progressIndicator;

    private final InventarioService inventarioService = new InventarioService(new ProductoDAO());
    private static final int PAGE_SIZE = 8;
    private PauseTransition searchDebounce;

    @FXML
    public void initialize() {
        tabla.setEditable(false);
        configurarColumnasTabla();
        cargarMarcas();

        // Debounce de búsqueda a 300ms
        searchDebounce = new PauseTransition(Duration.millis(300));
        searchDebounce.setOnFinished(e -> ejecutarBusqueda(txtBuscar.getText()));

        txtBuscar.textProperty().addListener((obs, oldVal, newVal) -> searchDebounce.playFromStart());

        pagination.currentPageIndexProperty().addListener((obs, oldIdx, newIdx) -> {
            cargarPaginaAsync(newIdx.intValue(), txtBuscar.getText());
        });

        ejecutarBusqueda("");
    }

    private void configurarColumnasTabla() {
        TablaInventarioConfigurator.configurar(tabla, inventarioService, this::refreshCurrentPage);
    }
    // Instancia del handler
    private final BusquedaPaginadaHandler busquedaHandler =
            new BusquedaPaginadaHandler(inventarioService, PAGE_SIZE);

    // En el controller queda extremadamente limpio:
    private void ejecutarBusqueda(String filtro) {
        busquedaHandler.ejecutarBusqueda(
                filtro,
                pagination,
                progressIndicator,
                lblInfo,
                this::cargarPaginaAsync
        );
    }

    private void cargarPaginaAsync(int pageIndex, String filtro) {
        busquedaHandler.cargarPaginaAsync(
                pageIndex,
                filtro,
                tabla,
                pagination,
                progressIndicator,
                lblPaginaInfo
        );
    }
    public void refreshCurrentPage() {
        cargarPaginaAsync(pagination.getCurrentPageIndex(), txtBuscar.getText());
    }

    @FXML
    public void onGuardarProducto() {
        boolean guardado = formHandler.guardarProducto(txtRef, txtNombre, cbMarcas);
        if (guardado) {
            ejecutarBusqueda(txtBuscar.getText());
        }
    }

    @FXML
    public void onAgregarProducto() {
        onGuardarProducto();
    }

    @FXML
    public void onGuardarMarca() {
        formHandler.guardarMarca(txtNuevaMarca, this::cargarMarcas);
    }

    @FXML
    public void onAgregarMarca() {
        onGuardarMarca();
    }

    private final BackupHandler backupHandler = new BackupHandler();
    private final HistorialHandler historialHandler = new HistorialHandler();
    private final FotoHandler fotoHandler = new FotoHandler(new ProductoDAO());
    private final CambioHandler cambioHandler = new CambioHandler(new ProductoDAO());
    private final FormHandler formHandler = new FormHandler(inventarioService);

    @FXML
    public void onRealizarCambio() {
        cambioHandler.mostrarDialogo(tabla);
    }

    @FXML
    public void onAgregarFoto() {
        Stage stage = (Stage) tabla.getScene().getWindow();
        fotoHandler.adjuntarFoto(tabla, stage);
    }

    @FXML
    public void onVerHistorialGeneral() {
        historialHandler.mostrarGeneral();
    }

    @FXML
    public void onVerHistorialSeleccionado() {
        historialHandler.mostrarSeleccionado(tabla);
    }

        @FXML
    public void onBackup() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        backupHandler.ejecutarBackup(stage);
    }

    @FXML
    public void onRestore() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        backupHandler.ejecutarRestore(stage);
    }

    private void cargarMarcas() {
        Task<List<Marca>> task = new Task<>() {
            @Override protected List<Marca> call() {
                return inventarioService.listarMarcas();
            }
        };
        task.setOnSucceeded(e -> cbMarcas.setItems(FXCollections.observableArrayList(task.getValue())));
        new Thread(task).start();
    }
}