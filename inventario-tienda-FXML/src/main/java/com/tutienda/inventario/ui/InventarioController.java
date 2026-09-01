package com.tutienda.inventario.ui;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.dao.MovimientoDAO;
import com.tutienda.inventario.exception.DatabaseException;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.model.Movimiento;
import com.tutienda.inventario.util.Alerts;
import com.tutienda.inventario.util.FotoService;
import com.tutienda.inventario.util.Validador;
import com.tutienda.inventario.util.BackupService;
import com.tutienda.inventario.util.ConfigService;
import com.tutienda.inventario.dao.ResultadoCrearProducto;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class InventarioController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<Producto> tabla;
    @FXML private Pagination pagination;
    @FXML private ComboBox<Marca> cbMarcas;
    @FXML private TextField txtRef, txtNombre, txtNuevaMarca;
    @FXML private Label lblInfo;
    @FXML private Label lblPaginaInfo;

    private final Marca marca = new Marca();
    private ProductoDAO dao = new ProductoDAO();
    private MovimientoDAO movDao = new MovimientoDAO();
    private final int PAGE_SIZE = 8;
    private int totalProductos = 0;
    private String filtroPendiente = "";

    // === FASE 1: Fixes Master ===
    // 1. Cache imágenes para no recargar disco + OOM
    private static final Map<String, Image> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    // 2. Debounce búsqueda
    private javafx.animation.PauseTransition searchDebounce;
    // 3. Executor único para historial (no new Thread() huérfano)
    private final java.util.concurrent.ExecutorService historialExecutor = java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "historial-loader");
        t.setDaemon(true);
        return t;
    });

    private Image loadImageCached(File f) {
        String key = f.getAbsolutePath() + "_" + f.lastModified();
        return IMAGE_CACHE.computeIfAbsent(key, k -> {
            try {
                return new Image(f.toURI().toString(), 90, 55, true, true, true);
            } catch (Exception e) {
                return null;
            }
        });
    }

    @FXML
    public void initialize() {
        tabla.setEditable(false);
        cargarMarcas();

        TableColumn<Producto, String> colRef = new TableColumn<>("MARCA / REFERENCIA");
        colRef.setPrefWidth(285);
        colRef.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        colRef.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getMarcaNombre() + " - " + c.getValue().getReferencia() + "\n" + c.getValue().getNombre()
        ));
        colRef.setCellFactory(tc -> {
            TableCell<Producto, String> cell = new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty){
                    super.updateItem(item, empty);
                    if(empty || item==null){
                        setText(null); setStyle("");
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #ffffff; -fx-background-color: transparent; -fx-font-size: 13.5; -fx-font-weight: bold; -fx-cursor: hand; -fx-line-spacing: 1.5;");
                    }
                }
            };
            cell.setOnMouseClicked(e -> {
                if(e.getClickCount() == 2 && !cell.isEmpty()){
                    Producto p = cell.getTableView().getItems().get(cell.getIndex());
                    if(p != null) mostrarDialogoEditarProducto(p);
                }
            });
            return cell;
        });
        TableColumn<Producto, Producto> colFoto = new TableColumn<>("FOTO MODELO");
        colFoto.setPrefWidth(110);
        colFoto.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue()));
        colFoto.setCellFactory(column -> new TableCell<>() {
            private final ImageView iv = new ImageView();
            private final Label lblAdd = new Label("📷 Agregar");
            {
                iv.setFitWidth(90); iv.setFitHeight(55); iv.setPreserveRatio(true);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                lblAdd.setStyle("-fx-text-fill: #666; -fx-font-size: 11px; -fx-cursor: hand; -fx-border-color: #333; -fx-border-radius: 6; -fx-padding: 12 15;");
            }
            private void elegirFoto(Producto prod){
                FileChooser fc = new FileChooser();
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.png","*.jpg","*.jpeg"));
                File sel = fc.showOpenDialog(getScene()!=null?getScene().getWindow():null);
                if(sel!=null){
                    try{
                        String oldRuta = prod.getFotoRuta();
                        String nuevaRuta = FotoService.guardarFoto(prod.getId(), sel);
                        dao.actualizarFoto(prod.getId(), nuevaRuta);
                        prod.setFotoRuta(nuevaRuta);
                        // Invalidar cache vieja
                        if (oldRuta != null) IMAGE_CACHE.remove(oldRuta);
                        // Borrar vieja SOLO después de actualizar DB OK
                        FotoService.borrarFotoSiExiste(oldRuta);
                        getTableView().refresh();
                    }catch(Exception ex){ Alerts.error("Error foto", ex.getMessage(), ex); }
                }
            }
            @Override protected void updateItem(Producto p, boolean empty){
                super.updateItem(p, empty);
                if(empty || p==null){ setGraphic(null); setCursor(Cursor.DEFAULT); setOnMouseClicked(null); setTooltip(null); return; }
                String ruta = p.getFotoRuta();
                if(ruta==null || ruta.isBlank()){
                    setGraphic(lblAdd);
                    setCursor(Cursor.HAND);
                    setStyle("-fx-background-color: #1e1e1e; -fx-alignment: center;");
                    setOnMouseClicked(e -> elegirFoto(p));
                    setTooltip(null);
                }else{
                    try{
                        File f = new File(ruta);
                        if(f.exists()){
                            Image cached = loadImageCached(f);
                            if (cached != null) iv.setImage(cached);
                            else iv.setImage(new Image(f.toURI().toString(),90,55,true,true,true));
                            setGraphic(iv);
                            setCursor(Cursor.HAND);
                            setStyle("-fx-background-color: #1e1e1e; -fx-alignment: center;");
                            setOnMouseClicked(e -> { if(e.getClickCount()==1) mostrarFotoGrande(f,p); });
                            setTooltip(new Tooltip("Click para ampliar - Doble click cambia"));
                        }else{
                            setGraphic(lblAdd);
                            setOnMouseClicked(e -> elegirFoto(p));
                        }
                    }catch(Exception ex){ setGraphic(lblAdd); }
                }
            }
        });

        tabla.getColumns().add(colRef);

        tabla.setFixedCellSize(58);

        for(int t=34; t<=45; t++){
            int talla = t;
            TableColumn<Producto, Integer> col = new TableColumn<>(String.valueOf(t));
            col.setPrefWidth(50);
            col.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTallas().getOrDefault(talla, 0)));
            col.setCellFactory(c -> new TableCell<>() {
                { setAlignment(Pos.CENTER); setOnMouseClicked(e -> { if(getTableRow()!=null && !isEmpty()){ Producto p = getTableRow().getItem(); if(p!=null) mostrarDialogoMovimientoDark(p, talla); } }); }
                @Override protected void updateItem(Integer cant, boolean empty){
                    super.updateItem(cant, empty);
                    if(empty || cant==null){ setText(null); setStyle(""); setGraphic(null); }
                    else if(cant==0){
                        // PRO: — discreto en lugar de X roja gritona
                        setText("—");
                        setStyle("-fx-background-color: transparent; -fx-text-fill: #3a3a3a; -fx-font-weight: bold; -fx-font-size: 14; -fx-alignment: center;");
                        setCursor(Cursor.HAND);
                    } else {
                        // PRO: badge verde redondeado
                        setText(String.valueOf(cant));
                        setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: 900; -fx-font-size: 13; -fx-alignment: center; -fx-background-radius: 8; -fx-min-width: 32; -fx-min-height: 26;");
                        setCursor(Cursor.HAND);
                    }
                }
            });
            tabla.getColumns().add(col);
        }

        TableColumn<Producto, Integer> colTotal = new TableColumn<>("TOTAL");
        colTotal.setPrefWidth(72);
        colTotal.setCellValueFactory(c -> new SimpleObjectProperty<>(c.getValue().getTotal()));
        colTotal.setCellFactory(c -> new TableCell<>() {
            { setAlignment(Pos.CENTER); }
            @Override protected void updateItem(Integer total, boolean empty){
                super.updateItem(total, empty);
                if(empty || total==null){ setText(null); setStyle(""); }
                else {
                    setText(String.valueOf(total));
                    if(total==0) setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #555; -fx-alignment: center; -fx-background-color: transparent;");
                    else if(total<=3) setStyle("-fx-font-weight: 900; -fx-font-size: 14; -fx-alignment: center; -fx-background-color: #F59E0B; -fx-text-fill: black; -fx-background-radius: 20; -fx-padding: 3 10;");
                    else setStyle("-fx-font-weight: 900; -fx-font-size: 14; -fx-alignment: center; -fx-background-color: #065F46; -fx-text-fill: #6EE7B7; -fx-background-radius: 20; -fx-border-color: #10B98155; -fx-border-width: 1; -fx-border-radius: 20; -fx-padding: 3 10;");
                }
            }
        });
        tabla.getColumns().add(colTotal);
        tabla.getColumns().add(colFoto);

        // PRO: zebra + hover
        tabla.setRowFactory(tv -> {
            TableRow<Producto> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) row.setStyle("");
                else {
                    if (row.getIndex() % 2 == 0) row.setStyle("-fx-background-color: #1A1A1C;");
                    else row.setStyle("-fx-background-color: #121212;");
                }
            });
            row.hoverProperty().addListener((obs, wasHover, isHover) -> {
                if (isHover && !row.isEmpty()) row.setStyle("-fx-background-color: #1e2e24;");
                else if (!row.isEmpty()){
                    if (row.getIndex() % 2 == 0) row.setStyle("-fx-background-color: #1A1A1C;");
                    else row.setStyle("-fx-background-color: #121212;");
                }
            });
            return row;
        });

        // FASE 1 FIX: Debounce 300ms - evita 4 queries al escribir NIKE
        searchDebounce = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
        searchDebounce.setOnFinished(e -> {
            filtroPendiente = txtBuscar.getText();
            actualizarPaginacion(filtroPendiente);
        });
        txtBuscar.textProperty().addListener((obs, old, nuevo) -> {
            searchDebounce.playFromStart();
        });
        pagination.currentPageIndexProperty().addListener((obs, old, nuevo) -> {
            cargarPagina(nuevo.intValue(), txtBuscar.getText());
            if (lblPaginaInfo != null) {
                lblPaginaInfo.setText("Página " + (nuevo.intValue() + 1) + "/" + pagination.getPageCount());
            }
        });
        actualizarPaginacion("");
    }

    private void actualizarPaginacion(String filtro) {
        try {
            totalProductos = dao.contar(filtro);
            int pageCount = (int) Math.ceil((double) totalProductos / PAGE_SIZE); if(pageCount==0) pageCount=1;
            pagination.setPageCount(pageCount);
            cargarPagina(pagination.getCurrentPageIndex(), filtro);
            // PRO MAX badge separado
            lblInfo.setText(totalProductos + " modelos");
            if (lblPaginaInfo != null) {
                lblPaginaInfo.setText("Página " + (pagination.getCurrentPageIndex() + 1) + "/" + pagination.getPageCount());
            }
        } catch (DatabaseException ex) { Alerts.error("Error al paginar", ex.getMessage(), ex); }
    }
    private void cargarPagina(int page, String filtro) {
        try { List<Producto> productos = dao.listar(filtro, page, PAGE_SIZE); tabla.setItems(FXCollections.observableArrayList(productos)); }
        catch (DatabaseException ex) { Alerts.error("Error al cargar inventario", ex.getMessage(), ex); }
    }
    private void cargarMarcas() { try { cbMarcas.setItems(FXCollections.observableArrayList(dao.listarMarcas())); } catch (DatabaseException ex) { Alerts.error("Error al cargar marcas", ex.getMessage(), ex); } }

    private void cargarCSSDark(DialogPane pane){
        String[] rutas = { "/css/dialog-dark.css", "/css/dark-theme.css", "/com/tutienda/inventario/css/dialog-dark.css", "/com/tutienda/inventario/ui/dark-dialog.css" };
        for(String ruta : rutas){
            try{ var url = getClass().getResource(ruta); if(url!=null){ pane.getStylesheets().add(url.toExternalForm()); return; } }catch(Exception e){}
        }
    }

    private void mostrarDialogoMovimientoDark(Producto p, int tallaPreseleccionada) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Movimiento - Talla " + tallaPreseleccionada);
        DialogPane pane = dialog.getDialogPane(); cargarCSSDark(pane); pane.setStyle("-fx-background-color: #1e1e1e;");
        Label lblProducto = new Label(p.getMarcaNombre() + " - " + p.getNombre()); lblProducto.setStyle("-fx-text-fill: white; -fx-font-size: 14; -fx-font-weight: bold; -fx-wrap-text: true;");
        Label lblRef = new Label("ART: " + p.getReferencia() + " | Stock T" + tallaPreseleccionada + ": " + p.getTallas().getOrDefault(tallaPreseleccionada,0)); lblRef.setStyle("-fx-text-fill: #888; -fx-font-size: 11px;");
        List<Integer> tallasDisponibles = p.getTallas().keySet().stream().sorted().collect(Collectors.toList());
        for(int i=34;i<=45;i++) if(!tallasDisponibles.contains(i)) tallasDisponibles.add(i);
        tallasDisponibles = tallasDisponibles.stream().sorted().collect(Collectors.toList());
        ComboBox<Integer> cbTalla = new ComboBox<>(FXCollections.observableArrayList(tallasDisponibles));
        cbTalla.setPrefWidth(200);
        cbTalla.setValue(tallaPreseleccionada);
        cbTalla.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item==null) setText(null);
                else setText(String.valueOf(item));
                setStyle("-fx-text-fill: white; -fx-background-color: #2d2d2d; -fx-opacity: 1;");
            }
        });
        cbTalla.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if(empty || item==null) setText(null);
                else setText(String.valueOf(item));
                setStyle("-fx-text-fill: white; -fx-background-color: #2d2d2d;");
            }
        });
        Label lblStockActual = new Label("Stock actual T" + tallaPreseleccionada + ": " + p.getTallas().getOrDefault(tallaPreseleccionada,0) + " pares"); lblStockActual.setStyle("-fx-text-fill: #4fc3f7; -fx-font-size: 12; -fx-font-weight: bold; -fx-background-color: #2a2a2a; -fx-padding: 6 10; -fx-background-radius: 4;");
        cbTalla.valueProperty().addListener((o,a,b)->{ if(b!=null) lblStockActual.setText("Stock actual T" + b + ": " + p.getTallas().getOrDefault(b,0) + " pares"); });
        ComboBox<String> cbTipo = new ComboBox<>(FXCollections.observableArrayList("VENTA","ENTRADA","SALIDA"));
        cbTipo.setPrefWidth(200);
        cbTipo.setValue("VENTA");
        cbTipo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? null : item);
                setStyle("-fx-text-fill: white; -fx-background-color: #2d2d2d; -fx-opacity: 1;");
            }
        });
        ComboBox<String> cbMetodoPago = new ComboBox<>(FXCollections.observableArrayList("EFECTIVO","NEQUI","BANCOLOMBIA","TARJETA","SISTECREDITO","ADDI","SEPARADO"));
        cbMetodoPago.setPrefWidth(200);
        cbMetodoPago.setValue("EFECTIVO");
        cbMetodoPago.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item==null ? null : item);
                setStyle("-fx-text-fill: white; -fx-background-color: #2d2d2d; -fx-opacity: 1;");
            }
        });
        Label lblMetodo = new Label("Método de pago:"); lblMetodo.setStyle("-fx-text-fill: #b0b0b0; -fx-font-size: 12;");
        HBox boxPago = new HBox(10, lblMetodo, cbMetodoPago); boxPago.setAlignment(Pos.CENTER_LEFT);
        cbTipo.valueProperty().addListener((o,a,b)-> { boolean esVenta = "VENTA".equals(b); boxPago.setVisible(esVenta); boxPago.setManaged(esVenta); });
        TextField txtCantidad = new TextField("1"); txtCantidad.setPrefWidth(200);
        TextField txtObs = new TextField(); txtObs.setPromptText("Observación opcional"); txtObs.setPrefWidth(200);
        String darkComboStyle = "-fx-background-color: #2d2d2d; -fx-border-color: #3d3d3d; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white;";
        String darkFieldStyle = "-fx-background-color: #2d2d2d; -fx-border-color: #3d3d3d; -fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: white; -fx-prompt-text-fill: #777;";
        cbTalla.setStyle(darkComboStyle); cbTipo.setStyle(darkComboStyle); cbMetodoPago.setStyle(darkComboStyle); txtCantidad.setStyle(darkFieldStyle); txtObs.setStyle(darkFieldStyle);
        Label lblTalla = new Label("Talla:"); lblTalla.setStyle("-fx-text-fill: #b0b0b0;"); Label lblTipo = new Label("Tipo movimiento:"); lblTipo.setStyle("-fx-text-fill: #b0b0b0;"); Label lblCant = new Label("Cantidad:"); lblCant.setStyle("-fx-text-fill: #b0b0b0;"); Label lblObs = new Label("Obs:"); lblObs.setStyle("-fx-text-fill: #b0b0b0;");
        VBox content = new VBox(14); content.setPadding(new Insets(20)); content.setStyle("-fx-background-color: #1e1e1e;"); content.getChildren().addAll(lblProducto, lblRef, lblTalla, cbTalla, lblStockActual, lblTipo, cbTipo, boxPago, lblCant, txtCantidad, lblObs, txtObs);
        pane.setContent(content); pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) pane.lookupButton(ButtonType.OK); btnOk.setText("Aceptar"); btnOk.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-pref-height: 36; -fx-pref-width: 90;");
        Button btnCancel = (Button) pane.lookupButton(ButtonType.CANCEL); btnCancel.setText("Cancelar"); btnCancel.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #b0b0b0; -fx-border-color: #3d3d3d; -fx-border-radius: 6; -fx-background-radius: 6; -fx-pref-height: 36;");
        dialog.showAndWait().ifPresent(r->{ if(r==ButtonType.OK){ try{ int talla = cbTalla.getValue(); int cant = Integer.parseInt(txtCantidad.getText()); if(cant<=0) throw new Exception("Cantidad debe ser >0"); String tipo = cbTipo.getValue(); String obsFinal = txtObs.getText(); if("VENTA".equals(tipo)){ obsFinal = "[PAGO: "+cbMetodoPago.getValue()+"] " + (obsFinal!=null?obsFinal:""); } dao.registrarMovimiento(p.getId(), talla, tipo, cant, obsFinal); p.setTallas(dao.getTallas(p.getId())); tabla.refresh(); actualizarPaginacion(txtBuscar.getText()); }catch(Exception ex){ Alerts.error("Error movimiento", ex.getMessage(), ex); } } });
    }

    @FXML void onAgregarFoto() { Producto sel = tabla.getSelectionModel().getSelectedItem(); if (sel == null) { Alerts.info("Selecciona", "Selecciona un producto"); return; } FileChooser fc = new FileChooser(); fc.setTitle("Foto - " + sel.getReferencia()); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.png","*.jpg","*.jpeg")); File file = fc.showOpenDialog(tabla.getScene().getWindow()); if (file != null) { try { String oldRuta = sel.getFotoRuta(); String nuevaRuta = FotoService.guardarFoto(sel.getId(), file); dao.actualizarFoto(sel.getId(), nuevaRuta); sel.setFotoRuta(nuevaRuta); if (oldRuta != null) IMAGE_CACHE.remove(oldRuta); FotoService.borrarFotoSiExiste(oldRuta); tabla.refresh(); } catch(Exception ex){ Alerts.error("Error", ex.getMessage(), ex); } } }
    @FXML void onAgregarMarca() { try { String nombreValidado = Validador.validarYNormalizarMarca(txtNuevaMarca.getText()); dao.crearMarca(nombreValidado); txtNuevaMarca.clear(); cargarMarcas(); } catch(Exception ex){ Alerts.error("Error", ex.getMessage(), ex); } }
    @FXML void onAgregarProducto() { try { String referencia = Validador.validarYNormalizarReferencia(txtRef.getText()); String nombre = Validador.validarYNormalizarNombre(txtNombre.getText()); Marca marca = Validador.validarMarcaSeleccionada(cbMarcas.getValue()); ResultadoCrearProducto resultado = dao.crearProducto(referencia, nombre, marca.getId()); switch (resultado) { case CREADO -> { txtRef.clear(); txtNombre.clear(); actualizarPaginacion(txtBuscar.getText()); } case DUPLICADO -> Alerts.error("Duplicado", "Ya existe " + referencia + " para " + marca.getNombre() + ".", null); case ERROR -> Alerts.error("Error", "Error inesperado.", null); } } catch(Exception ex){ Alerts.error("Error", ex.getMessage(), ex); } }
    @FXML void onVerHistorialGeneral() { mostrarHistorialGeneral(); }
    @FXML void onVerHistorialSeleccionado() { Producto sel = tabla.getSelectionModel().getSelectedItem(); if(sel==null){ Alerts.info("Selecciona", "Selecciona un producto"); return; } mostrarHistorialProducto(sel); }
    @FXML void onRealizarCambio() {
        Producto origen = tabla.getSelectionModel().getSelectedItem(); if(origen==null){ Alerts.info("Selecciona", "Selecciona primero el producto que el cliente devuelve"); return; }
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("🔄 Cambio"); DialogPane pane = dialog.getDialogPane(); cargarCSSDark(pane); pane.setStyle("-fx-background-color: #1e1e1e;");
        List<Producto> todos = dao.listar("",0,200); ComboBox<Producto> cbDestino = new ComboBox<>(FXCollections.observableArrayList(todos)); cbDestino.setPrefWidth(350); cbDestino.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
        cbDestino.setCellFactory(lv -> new ListCell<>(){ protected void updateItem(Producto pr, boolean e){ super.updateItem(pr,e); setText(e||pr==null?null:pr.getMarcaNombre()+" - "+pr.getReferencia()+" "+pr.getNombre()); setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;"); }}); cbDestino.setButtonCell(new ListCell<>(){ protected void updateItem(Producto pr, boolean e){ super.updateItem(pr,e); setText(e||pr==null?"Selecciona producto que lleva":pr.getMarcaNombre()+" "+pr.getReferencia()); setStyle("-fx-text-fill: white;"); }});
        ComboBox<Integer> cbTallaOrigen = new ComboBox<>(FXCollections.observableArrayList(origen.getTallas().keySet().stream().sorted().toList())); cbTallaOrigen.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
        ComboBox<Integer> cbTallaDestino = new ComboBox<>(); cbTallaDestino.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;"); Label lblStockDestino = new Label(); lblStockDestino.setStyle("-fx-text-fill: #4fc3f7;");
        cbDestino.valueProperty().addListener((o,a,b)->{ if(b!=null) { cbTallaDestino.setItems(FXCollections.observableArrayList(b.getTallas().keySet().stream().sorted().toList())); cbTallaDestino.valueProperty().addListener((o2,a2,b2)->{ if(b2!=null) lblStockDestino.setText("Stock destino T"+b2+": "+b.getTallas().getOrDefault(b2,0)); }); } });
        TextField txtMotivo = new TextField(); txtMotivo.setPromptText("No le quedó"); txtMotivo.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
        VBox box = new VBox(10, new Label("DEVUELVE:"), new Label(origen.getMarcaNombre()+" "+origen.getReferencia()), new Label("Talla que devuelve:"), cbTallaOrigen, new Separator(), new Label("LLEVA:"), cbDestino, new Label("Talla que lleva:"), cbTallaDestino, lblStockDestino, new Label("Motivo:"), txtMotivo); box.setPadding(new Insets(15)); box.setPrefWidth(400); box.setStyle("-fx-background-color: #1e1e1e;"); for(javafx.scene.Node n : box.getChildren()) if(n instanceof Label) n.setStyle("-fx-text-fill: #b0b0b0;");
        pane.setContent(box); pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.showAndWait().ifPresent(bt->{ if(bt==ButtonType.OK){ try{ if(cbDestino.getValue()==null || cbTallaOrigen.getValue()==null || cbTallaDestino.getValue()==null) throw new Exception("Completa todos los campos"); dao.realizarCambio(origen.getId(), cbTallaOrigen.getValue(), cbDestino.getValue().getId(), cbTallaDestino.getValue(), 1, txtMotivo.getText()); actualizarPaginacion(txtBuscar.getText()); Alerts.info("Cambio realizado", "Cambio OK"); } catch(Exception ex){ Alerts.error("Error cambio", ex.getMessage(), ex); } } });
    }
    public void mostrarFotoGrandePublic(File archivoFoto, Producto producto){
        mostrarFotoGrande(archivoFoto, producto);
    }

    private void mostrarFotoGrande(File archivoFoto, Producto producto) {
        try {
            Stage stage = new Stage(); stage.setTitle(producto.getMarcaNombre() + " - " + producto.getReferencia());
            ImageView ivGrande = new ImageView(new Image(archivoFoto.toURI().toString())); ivGrande.setPreserveRatio(true); ivGrande.setFitWidth(800); ivGrande.setFitHeight(600);
            Button btnCambiar = new Button("📷 Cambiar Foto");
            btnCambiar.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 4, 0, 2);");
            btnCambiar.setOnMouseEntered(e -> btnCambiar.setStyle("-fx-background-color: #0d9a6e; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));
            btnCambiar.setOnMouseExited(e -> btnCambiar.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));

            Button btnEliminar = new Button("🗑️ Eliminar Foto");
            btnEliminar.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #ef4444; -fx-border-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
            btnEliminar.setOnMouseEntered(e -> btnEliminar.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));
            btnEliminar.setOnMouseExited(e -> btnEliminar.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-color: #ef4444; -fx-border-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;"));

            Button btnCerrar = new Button("Cerrar");
            btnCerrar.setStyle("-fx-background-color: #3a3a3a; -fx-text-fill: #b0b0b0; -fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");            btnCambiar.setOnAction(e -> { FileChooser fc = new FileChooser(); fc.setTitle("Nueva foto para " + producto.getReferencia()); fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.png","*.jpg","*.jpeg")); File nuevo = fc.showOpenDialog(stage); if(nuevo != null){ try{ String nuevaRuta = FotoService.guardarFoto(producto.getId(), nuevo); FotoService.borrarFotoSiExiste(producto.getFotoRuta()); dao.actualizarFoto(producto.getId(), nuevaRuta); producto.setFotoRuta(nuevaRuta); ivGrande.setImage(new Image(new File(nuevaRuta).toURI().toString(), 800, 600, true, true, true)); tabla.refresh(); } catch(Exception ex){ Alerts.error("Error", ex.getMessage(), ex); } } });
            btnEliminar.setOnAction(e -> { if(new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar foto?", ButtonType.YES, ButtonType.NO).showAndWait().orElse(ButtonType.NO) == ButtonType.YES){ try{ FotoService.borrarFotoSiExiste(producto.getFotoRuta()); dao.actualizarFoto(producto.getId(), null); producto.setFotoRuta(null); tabla.refresh(); stage.close(); } catch(Exception ex){ Alerts.error("Error", ex.getMessage(), ex); } } });
            btnCerrar.setOnAction(e -> stage.close());
            HBox barraBotones = new HBox(12, btnCambiar, btnEliminar, btnCerrar);
            barraBotones.setAlignment(Pos.CENTER);
            barraBotones.setStyle("-fx-alignment: center; -fx-padding: 16; -fx-background-color: #1a1a1a; -fx-border-color: #2a2a2a; -fx-border-width: 1 0 0 0;");            VBox root = new VBox(); StackPane contenedorImg = new StackPane(ivGrande); contenedorImg.setStyle("-fx-background-color: #111; -fx-padding: 15;"); root.getChildren().addAll(contenedorImg, barraBotones);
            stage.setScene(new Scene(root)); stage.initModality(Modality.APPLICATION_MODAL); stage.initOwner(tabla.getScene().getWindow()); stage.showAndWait();
        } catch (Exception e) { Alerts.error("No se pudo abrir la foto", e.getMessage(), e); }
    }
    private void mostrarHistorialProducto(Producto p){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historial " + p.getReferencia());
        DialogPane pane = dialog.getDialogPane();
        cargarCSSDark(pane);
        pane.setPrefSize(980, 680);
        pane.setStyle("-fx-background-color: #121212;");

        // CSS EXTRA PARA TABLA DARK + LETRA GRANDE
        String darkTableCSS = """
        .table-view { -fx-background-color: #1a1a1a; -fx-control-inner-background: #1e1e1e; -fx-table-cell-border-color: #2a2a2a; -fx-border-color: #2a2a2a; }
        .table-view .column-header-background { -fx-background-color: #2a2a2a; }
        .table-view .column-header { -fx-background-color: #2a2a2a; -fx-border-color: #333; -fx-size: 38; }
        .table-view .column-header .label { -fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 12px; }
        .table-view .table-cell { -fx-border-color: #2a2a2a; -fx-font-size: 13px; }
        .table-row-cell { -fx-background-color: #1e1e1e; -fx-border-color: #2a2a2a; }
        .table-row-cell:odd { -fx-background-color: #252525; }
        .table-row-cell:selected { -fx-background-color: #1e3a2e; -fx-border-color: #10B981; }
        .date-picker { -fx-background-color: #2d2d2d; } .date-picker .text-field { -fx-background-color: #2d2d2d; -fx-text-fill: white; }
        .scroll-pane { -fx-background: #121212; -fx-background-color: #121212; } .scroll-bar { -fx-background-color: #1a1a1a; } .scroll-bar .thumb { -fx-background-color: #3a3a3a; }
    """;


        try{
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")+"/darkHist.css"), darkTableCSS);
            pane.getStylesheets().add(new java.io.File(System.getProperty("java.io.tmpdir")+"/darkHist.css").toURI().toString());
        }catch(Exception e){}

        Label lblTitulo = new Label(p.getMarcaNombre() + "  ART: " + p.getReferencia() + "  " + p.getNombre());
        lblTitulo.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 4 0 6 0;");

        DatePicker dpDesde = new DatePicker(java.time.LocalDate.now().minusDays(7));
        dpDesde.setPrefWidth(135);
        DatePicker dpHasta = new DatePicker(java.time.LocalDate.now());
        dpHasta.setPrefWidth(135);
        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-font-size: 13px; -fx-pref-height: 32; -fx-cursor: hand;");

        Label ld = new Label("Desde:"); ld.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px;");
        Label lh = new Label("Hasta:"); lh.setStyle("-fx-text-fill: #aaa; -fx-font-size: 13px;");
        HBox filtros = new HBox(10, ld, dpDesde, lh, dpHasta, btnFiltrar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.setPadding(new Insets(10));
        filtros.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 8;");

        VBox contenedorBloques = new VBox(14);
        contenedorBloques.setPadding(new Insets(12));
        contenedorBloques.setStyle("-fx-background-color: #121212;");

        Runnable cargarBloques = () -> {
            contenedorBloques.getChildren().clear();
            List<Movimiento> todos = movDao.listarPorProducto(p.getId());
            java.util.Map<String, List<Movimiento>> porDia = todos.stream()
                    .filter(m -> m.fecha!=null && !m.fecha.toLocalDate().isBefore(dpDesde.getValue()) && !m.fecha.toLocalDate().isAfter(dpHasta.getValue()))
                    .collect(Collectors.groupingBy(m -> m.fecha.toLocalDate().toString(), java.util.LinkedHashMap::new, Collectors.toList()));

            if(porDia.isEmpty()){
                Label vacio = new Label("No hay movimientos entre " + dpDesde.getValue() + " y " + dpHasta.getValue());
                vacio.setStyle("-fx-text-fill: #666; -fx-font-size: 13px; -fx-padding: 30;");
                contenedorBloques.getChildren().add(vacio); return;
            }

            for(String key : porDia.keySet()){
                List<Movimiento> movs = porDia.get(key);
                java.time.LocalDate ldate = java.time.LocalDate.parse(key);
                String titulo = ldate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", new java.util.Locale("es","ES"))).toUpperCase();

                Label lblFecha = new Label("📅 " + titulo + "  —  " + movs.size() + " movimientos");
                lblFecha.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: #2d2d2d; -fx-padding: 8 14; -fx-background-radius: 6;");
                lblFecha.setMaxWidth(Double.MAX_VALUE);

                TableView<Movimiento> tabla = new TableView<>();
                tabla.setFixedCellSize(38);
                tabla.setPrefHeight(movs.size()*38 + 42);
                tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                TableColumn<Movimiento, String> colHora = new TableColumn<>("Hora");
                colHora.setPrefWidth(85);
                colHora.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fecha.toLocalTime().toString().substring(0,8)));
                colHora.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle("-fx-text-fill: #aaa; -fx-background-color: transparent; -fx-alignment: center; -fx-font-size: 13px;"); } }});

                TableColumn<Movimiento, String> colTalla = new TableColumn<>("Talla");
                colTalla.setPrefWidth(65);
                colTalla.setCellValueFactory(c -> new SimpleStringProperty("T"+c.getValue().talla));
                colTalla.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle("-fx-text-fill: white; -fx-background-color: transparent; -fx-font-weight: bold; -fx-alignment: center; -fx-font-size: 13px;"); } }});

                TableColumn<Movimiento, String> colTipo = new TableColumn<>("Tipo / Cant");
                colTipo.setPrefWidth(130);
                colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tipo+" x"+c.getValue().cantidad));
                colTipo.setCellFactory(tc -> new TableCell<>(){
                    private final Label b = new Label(); { b.setPrefWidth(110); b.setAlignment(Pos.CENTER); setGraphic(b); setText(null); }
                    @Override protected void updateItem(String item, boolean empty){
                        super.updateItem(item,empty); if(empty||item==null){ setGraphic(null); setStyle("-fx-background-color: transparent;"); } else{ b.setText(item); b.setStyle(item.contains("VENTA")?"-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 10;":"-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 10;"); if(item.contains("SALIDA") && !item.contains("VENTA")) b.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px; -fx-padding: 5 10;"); setStyle("-fx-background-color: transparent; -fx-alignment: center;"); setGraphic(b); }
                    }
                });

                TableColumn<Movimiento, String> colStock = new TableColumn<>("Stock");
                colStock.setPrefWidth(85);
                colStock.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stockAnterior+" -> "+c.getValue().stockNuevo));
                colStock.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle("-fx-text-fill: #b0b0b0; -fx-background-color: transparent; -fx-alignment: center; -fx-font-size: 13px;"); } }});

                TableColumn<Movimiento, String> colPago = new TableColumn<>("Pago");
                colPago.setPrefWidth(120);
                colPago.setCellValueFactory(c -> { String o=c.getValue().observacion!=null?c.getValue().observacion:""; String pgo="---"; if(o.contains("[PAGO:")){ int a=o.indexOf("[PAGO:")+7; int b2=o.indexOf("]",a); pgo=o.substring(a,b2>0?b2:o.length()).trim().toUpperCase(); } return new SimpleStringProperty(pgo); });
                colPago.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle(i.equals("---")?"-fx-text-fill: #555; -fx-background-color: transparent; -fx-font-size: 13px;":"-fx-text-fill: #4fc3f7; -fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 13px;"); } }});

                TableColumn<Movimiento, String> colObs = new TableColumn<>("Obs / Motivo");
                colObs.setPrefWidth(260); // más ancho
                colObs.setCellValueFactory(c -> {
                    String obs = c.getValue().observacion!=null?c.getValue().observacion.replaceAll("\\[PAGO:.*?\\]","").trim():"---";
                    return new SimpleStringProperty(obs);
                });
                colObs.setCellFactory(tc -> new TableCell<>(){
                    private final Label label = new Label();
                    private final Tooltip tooltip = new Tooltip();
                    {
                        label.setWrapText(true);
                        label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
                        setGraphic(label);
                        // Doble click para ver completo
                        setOnMouseClicked(e -> {
                            if(e.getClickCount()==2 && getItem()!=null && !getItem().equals("---")){
                                // --- DIALOGO OBSERVACION PRO, SIMPLE Y TEXTO GRANDE ---
                                Dialog<ButtonType> obsDialog = new Dialog<>();
                                obsDialog.setTitle("Observación");
                                DialogPane obsPane = obsDialog.getDialogPane();
                                obsPane.setStyle("-fx-background-color: #121212;");
                                obsPane.getButtonTypes().add(ButtonType.CLOSE);
                                obsPane.setPrefWidth(600);

                                Label lblTexto = new Label(getItem());
                                lblTexto.setWrapText(true);
                                lblTexto.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 16px; -fx-font-weight: 500; -fx-line-spacing: 4px;");
                                lblTexto.setMaxWidth(540);

                                VBox content = new VBox(12, lblTexto);
                                content.setPadding(new Insets(20, 20, 10, 20));
                                content.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 12;");

                                obsPane.setContent(content);

// CSS botón cerrar
                                Button btnClose = (Button) obsPane.lookupButton(ButtonType.CLOSE);
                                btnClose.setText("Cerrar");
                                btnClose.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 90; -fx-pref-height: 34;");

// Quita el header feo
                                obsDialog.setHeaderText(null);
                                obsDialog.setGraphic(null);

                                obsDialog.showAndWait();
                            }
                        });
                    }
                    @Override protected void updateItem(String item, boolean empty){
                        super.updateItem(item, empty);
                        if(empty || item==null || item.equals("---")){
                            setGraphic(null); setTooltip(null);
                        } else {
                            boolean largo = item.length() > 35;
                            label.setText(largo ? item.substring(0,35)+"..." : item);
                            tooltip.setText(item);
                            tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 6;");
                            tooltip.setMaxWidth(400);
                            tooltip.setWrapText(true);
                            setTooltip(tooltip);
                            // Si es muy largo pinta amarillo suave
                            if(largo) label.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
                            else label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
                            setGraphic(label);
                            setStyle("-fx-background-color: transparent;");
                        }
                    }
                });
                tabla.getColumns().addAll(colHora, colTalla, colTipo, colStock, colPago, colObs);
                tabla.setItems(FXCollections.observableArrayList(movs));

                VBox bloque = new VBox(6, lblFecha, tabla);
                bloque.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 10;");
                contenedorBloques.getChildren().add(bloque);
            }
        };

        cargarBloques.run();
        btnFiltrar.setOnAction(e -> cargarBloques.run());

        ScrollPane scroll = new ScrollPane(contenedorBloques);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(560);
        scroll.setStyle("-fx-background: #121212;");

        VBox root = new VBox(10, lblTitulo, filtros, scroll);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #121212;");

        pane.setContent(root);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        Button btnClose = (Button) pane.lookupButton(ButtonType.CLOSE);
        btnClose.setText("Cerrar");
        btnClose.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-pref-width: 90;");

        dialog.showAndWait();
    }

    private void mostrarHistorialGeneral(){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Historial General");
        DialogPane pane = dialog.getDialogPane();
        cargarCSSDark(pane);
        pane.setPrefSize(1120, 780);
        pane.setStyle("-fx-background-color: #121212;");

        String darkCSS = """
        .table-view { -fx-background-color: #1a1a1a; -fx-control-inner-background: #1e1e1e; -fx-table-cell-border-color: #2a2a2a; }
        .table-view .column-header-background { -fx-background-color: #2a2a2a; }
        .table-view .column-header { -fx-background-color: #2a2a2a; -fx-border-color: #333; -fx-size: 40; }
        .table-view .column-header .label { -fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 12px; }
        .table-row-cell { -fx-background-color: #1e1e1e; -fx-fixed-cell-size: 38; }
        .table-row-cell:odd { -fx-background-color: #252525; }
        .table-row-cell:selected { -fx-background-color: #1e3a2e; }
    """;
        try{
            java.nio.file.Files.writeString(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")+"/darkBlock.css"), darkCSS);
            pane.getStylesheets().add(new java.io.File(System.getProperty("java.io.tmpdir")+"/darkBlock.css").toURI().toString());
        }catch(Exception e){}

        DatePicker dpDesde = new DatePicker(LocalDate.now().minusDays(7));
        DatePicker dpHasta = new DatePicker(LocalDate.now());
        dpDesde.setPrefWidth(135); dpHasta.setPrefWidth(135);
        TextField txtFiltro = new TextField(); txtFiltro.setPromptText("Buscar ART, marca...");
        txtFiltro.setPrefWidth(190);
        txtFiltro.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 6;");

        Button btnFiltrar = new Button("🔍 Filtrar");
        btnFiltrar.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-pref-height: 32;");
        Label lblContador = new Label("—"); lblContador.setStyle("-fx-text-fill: #888; -fx-font-size: 12px;");

        // --- 3 BOTONES PRO ---
        AtomicReference<String> tipoFiltro = new AtomicReference<>("TODOS");
        ToggleGroup groupTipo = new ToggleGroup();
        ToggleButton btnTodos = new ToggleButton("TODOS");
        ToggleButton btnVenta = new ToggleButton("💰 VENTA");
        ToggleButton btnEntrada = new ToggleButton("📦 ENTRADA");
        ToggleButton btnSalida = new ToggleButton("↩️ SALIDA");
        btnTodos.setToggleGroup(groupTipo); btnVenta.setToggleGroup(groupTipo);
        btnEntrada.setToggleGroup(groupTipo); btnSalida.setToggleGroup(groupTipo);
        btnTodos.setSelected(true);

        String baseBtn = "-fx-background-color: #2d2d2d; -fx-text-fill: #aaa; -fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #3a3a3a; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand; -fx-pref-height: 32; -fx-pref-width: 100;";
        String selTodos = "-fx-background-color: #3a3a3a; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-pref-height: 32; -fx-pref-width: 100;";
        String selVenta = "-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-pref-height: 32; -fx-pref-width: 100;";
        String selEntrada = "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-pref-height: 32; -fx-pref-width: 100;";
        String selSalida = "-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-pref-height: 32; -fx-pref-width: 100;";

        btnTodos.setStyle(selTodos); btnVenta.setStyle(baseBtn); btnEntrada.setStyle(baseBtn); btnSalida.setStyle(baseBtn);
        HBox boxTipos = new HBox(8, btnTodos, btnVenta, btnEntrada, btnSalida);
        boxTipos.setAlignment(Pos.CENTER_LEFT);
        boxTipos.setPadding(new Insets(6));

        HBox filtrosFechas = new HBox(10, new Label("Desde:"){{setStyle("-fx-text-fill:#aaa;");}}, dpDesde, new Label("Hasta:"){{setStyle("-fx-text-fill:#aaa;");}}, dpHasta, txtFiltro, btnFiltrar, lblContador);
        filtrosFechas.setAlignment(Pos.CENTER_LEFT);

        VBox filtros = new VBox(8, filtrosFechas, boxTipos);
        filtros.setPadding(new Insets(10));
        filtros.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 8;");

        VBox contenedorBloques = new VBox(12);
        contenedorBloques.setPadding(new Insets(12));
        contenedorBloques.setStyle("-fx-background-color: #121212;");

        AtomicReference<LocalDateTime> cursorFecha = new AtomicReference<>(null);
        AtomicInteger cursorId = new AtomicInteger(0);
        AtomicBoolean cargando = new AtomicBoolean(false);
        AtomicLong totalCount = new AtomicLong(0);

        Button btnCargarMas = new Button("Cargar 200 más ↓");
        btnCargarMas.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-border-color: #10B981; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-pref-height: 36;");
        btnCargarMas.setMaxWidth(Double.MAX_VALUE);

        Runnable cargarPagina = () -> {
            if(cargando.get()) return; cargando.set(true);
            btnCargarMas.setDisable(true); btnCargarMas.setText("Cargando...");
            historialExecutor.submit(() -> {
                MovimientoDAO.PaginaHistorial pag = movDao.listarPaginaGeneral(txtFiltro.getText(), dpDesde.getValue(), dpHasta.getValue(), tipoFiltro.get(), cursorFecha.get(), cursorId.get(), 200);
                Platform.runLater(() -> {
                    Map<String, List<Movimiento>> porDia = pag.movimientos().stream()
                            .collect(Collectors.groupingBy(m -> m.fecha.toLocalDate().toString(), LinkedHashMap::new, Collectors.toList()));

                    for(String key : porDia.keySet()){
                        List<Movimiento> movs = porDia.get(key);
                        LocalDate ld = LocalDate.parse(key);
                        String titulo = ld.format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", new Locale("es","ES"))).toUpperCase() + " — " + movs.size() + " movimientos";
                        Optional<VBox> bloqueExistente = contenedorBloques.getChildren().stream()
                                .filter(n -> n instanceof VBox && n.getUserData()!=null && n.getUserData().equals(key))
                                .map(n -> (VBox)n).findFirst();
                        if(bloqueExistente.isPresent()){
                            TableView<Movimiento> tablaExist = (TableView<Movimiento>) bloqueExistente.get().getChildren().get(1);
                            tablaExist.getItems().addAll(movs);
                            tablaExist.setPrefHeight(tablaExist.getItems().size()*38 + 42);
                            Label lbl = (Label) bloqueExistente.get().getChildren().get(0);
                            lbl.setText("📅 " + titulo);
                            continue;
                        }
                        Label lblFecha = new Label("📅 " + titulo);
                        lblFecha.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 13px; -fx-background-color: #2d2d2d; -fx-padding: 8 14; -fx-background-radius: 6;");
                        lblFecha.setMaxWidth(Double.MAX_VALUE);
                        TableView<Movimiento> tabla = new TableView<>();
                        tabla.setFixedCellSize(38);
                        tabla.setPrefHeight(movs.size()*38 + 42);
                        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                        TableColumn<Movimiento, String> colHora = new TableColumn<>("Hora");
                        colHora.setPrefWidth(80); colHora.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().fecha.toLocalTime().toString().substring(0,8)));
                        colHora.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle("-fx-text-fill: #aaa; -fx-background-color: transparent; -fx-alignment: center; -fx-font-size: 13px;"); } }});
                        TableColumn<Movimiento, String> colArt = new TableColumn<>("Artículo / Marca");
                        colArt.setPrefWidth(360); colArt.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().marcaNombre + " ART: " + c.getValue().productoRef + " " + (c.getValue().productoNombre!=null?c.getValue().productoNombre:"") + " (T"+c.getValue().talla+")"));
                        colArt.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle("-fx-text-fill: #e0e0e0; -fx-background-color: transparent; -fx-font-size: 13px;"); } }});
                        TableColumn<Movimiento, String> colTipo = new TableColumn<>("Tipo / Cant");
                        colTipo.setPrefWidth(125); colTipo.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().tipo+" x"+c.getValue().cantidad));
                        colTipo.setCellFactory(tc -> new TableCell<>(){
                            private final Label b = new Label(); { b.setPrefWidth(105); b.setAlignment(Pos.CENTER); setGraphic(b); }
                            @Override protected void updateItem(String item, boolean empty){ super.updateItem(item,empty); if(empty||item==null){ setGraphic(null); } else{ b.setText(item);
                                if(item.contains("VENTA")) b.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-size: 12px;");
                                else if(item.contains("ENTRADA")) b.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-size: 12px;");
                                else b.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-padding: 5 10; -fx-font-size: 12px;");
                                setStyle("-fx-background-color: transparent; -fx-alignment: center;"); setGraphic(b); } }
                        });
                        TableColumn<Movimiento, String> colStock = new TableColumn<>("Stock");
                        colStock.setPrefWidth(70); colStock.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().stockAnterior+"->"+c.getValue().stockNuevo));
                        TableColumn<Movimiento, String> colPago = new TableColumn<>("Pago");
                        colPago.setPrefWidth(105); colPago.setCellValueFactory(c -> { String o=c.getValue().observacion!=null?c.getValue().observacion:""; String p="---"; if(o.contains("[PAGO:")){ int a=o.indexOf("[PAGO:")+7; int b2=o.indexOf("]",a); p=o.substring(a,b2>0?b2:o.length()).trim().toUpperCase(); } return new SimpleStringProperty(p); });
                        colPago.setCellFactory(tc -> new TableCell<>(){ @Override protected void updateItem(String i, boolean e){ super.updateItem(i,e); if(e||i==null){ setText(null); setStyle(""); } else{ setText(i); setStyle(i.equals("---")?"-fx-text-fill: #555; -fx-background-color: transparent;":"-fx-text-fill: #4fc3f7; -fx-background-color: transparent; -fx-font-weight: bold; -fx-font-size: 12px;"); } }});
                        TableColumn<Movimiento, String> colObs = new TableColumn<>("Obs / Motivo");
                        colObs.setPrefWidth(260); // más ancho
                        colObs.setCellValueFactory(c -> {
                            String obs = c.getValue().observacion!=null?c.getValue().observacion.replaceAll("\\[PAGO:.*?\\]","").trim():"---";
                            return new SimpleStringProperty(obs);
                        });
                        colObs.setCellFactory(tc -> new TableCell<>(){
                            private final Label label = new Label();
                            private final Tooltip tooltip = new Tooltip();
                            {
                                label.setWrapText(true);
                                label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
                                setGraphic(label);
                                // Doble click para ver completo
                                setOnMouseClicked(e -> {
                                    if(e.getClickCount()==2 && getItem()!=null && !getItem().equals("---")){
                                        // --- DIALOGO OBSERVACION PRO, SIMPLE Y TEXTO GRANDE ---
                                        Dialog<ButtonType> obsDialog = new Dialog<>();
                                        obsDialog.setTitle("Observación");
                                        DialogPane obsPane = obsDialog.getDialogPane();
                                        obsPane.setStyle("-fx-background-color: #121212;");
                                        obsPane.getButtonTypes().add(ButtonType.CLOSE);
                                        obsPane.setPrefWidth(600);

                                        Label lblTexto = new Label(getItem());
                                        lblTexto.setWrapText(true);
                                        lblTexto.setStyle("-fx-text-fill: #e0e0e0; -fx-font-size: 16px; -fx-font-weight: 500; -fx-line-spacing: 4px;");
                                        lblTexto.setMaxWidth(540);

                                        VBox content = new VBox(12, lblTexto);
                                        content.setPadding(new Insets(20, 20, 10, 20));
                                        content.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 12;");

                                        obsPane.setContent(content);

// CSS botón cerrar
                                        Button btnClose = (Button) obsPane.lookupButton(ButtonType.CLOSE);
                                        btnClose.setText("Cerrar");
                                        btnClose.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-pref-width: 90; -fx-pref-height: 34;");

// Quita el header feo
                                        obsDialog.setHeaderText(null);
                                        obsDialog.setGraphic(null);

                                        obsDialog.showAndWait();
                                    }
                                });
                            }
                            @Override protected void updateItem(String item, boolean empty){
                                super.updateItem(item, empty);
                                if(empty || item==null || item.equals("---")){
                                    setGraphic(null); setTooltip(null);
                                } else {
                                    boolean largo = item.length() > 35;
                                    label.setText(largo ? item.substring(0,35)+"..." : item);
                                    tooltip.setText(item);
                                    tooltip.setStyle("-fx-font-size: 13px; -fx-background-color: #2d2d2d; -fx-text-fill: white; -fx-background-radius: 6;");
                                    tooltip.setMaxWidth(400);
                                    tooltip.setWrapText(true);
                                    setTooltip(tooltip);
                                    // Si es muy largo pinta amarillo suave
                                    if(largo) label.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 12px; -fx-font-weight: bold; -fx-cursor: hand;");
                                    else label.setStyle("-fx-text-fill: #ccc; -fx-font-size: 12px;");
                                    setGraphic(label);
                                    setStyle("-fx-background-color: transparent;");
                                }
                            }
                        });tabla.getColumns().addAll(colHora, colArt, colTipo, colStock, colPago, colObs);
                        tabla.setItems(FXCollections.observableArrayList(movs));
                        VBox bloque = new VBox(6, lblFecha, tabla);
                        bloque.setUserData(key);
                        bloque.setStyle("-fx-background-color: #1e1e1e; -fx-background-radius: 10; -fx-padding: 8; -fx-border-color: #2a2a2a; -fx-border-radius: 10;");
                        contenedorBloques.getChildren().add(bloque);
                    }
                    cursorFecha.set(pag.lastFecha()); cursorId.set(pag.lastId());
                    totalCount.set(pag.totalCount());
                    lblContador.setText("Mostrando " + contenedorBloques.getChildren().stream().mapToInt(v -> ((TableView<Movimiento>)((VBox)v).getChildren().get(1)).getItems().size()).sum() + " de " + pag.totalCount() + " movimientos");
                    btnCargarMas.setVisible(pag.hasMore()); btnCargarMas.setDisable(false);
                    btnCargarMas.setText("Cargar 200 más ↓ ("+pag.totalCount()+" total)");
                    cargando.set(false);
                });
            });
        };

        // Listeners de los 3 botones
        groupTipo.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            if(newV==null) return;
            ToggleButton sel = (ToggleButton)newV;
            tipoFiltro.set(sel==btnVenta ? "VENTA" : sel==btnEntrada ? "ENTRADA" : sel==btnSalida ? "SALIDA" : "TODOS");
            // colores
            btnTodos.setStyle(btnTodos.isSelected()?selTodos:baseBtn);
            btnVenta.setStyle(btnVenta.isSelected()?selVenta:baseBtn);
            btnEntrada.setStyle(btnEntrada.isSelected()?selEntrada:baseBtn);
            btnSalida.setStyle(btnSalida.isSelected()?selSalida:baseBtn);
            // recargar
            contenedorBloques.getChildren().clear(); cursorFecha.set(null); cursorId.set(0); cargarPagina.run();
        });

        btnFiltrar.setOnAction(e -> { contenedorBloques.getChildren().clear(); cursorFecha.set(null); cursorId.set(0); cargarPagina.run(); });
        txtFiltro.setOnAction(e -> btnFiltrar.fire());
        btnCargarMas.setOnAction(e -> cargarPagina.run());

        ScrollPane scroll = new ScrollPane(contenedorBloques);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #121212; -fx-background-color: #121212;");
        scroll.setPrefViewportHeight(600);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox root = new VBox(10, filtros, scroll, btnCargarMas);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #121212;");
        VBox.setVgrow(root, Priority.ALWAYS);

        pane.setContent(root);
        pane.getButtonTypes().add(ButtonType.CLOSE);
        Button close = (Button)pane.lookupButton(ButtonType.CLOSE);
        close.setText("Cerrar"); close.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-pref-width: 90;");

        cargarPagina.run();
        dialog.showAndWait();
    }


    private void mostrarDialogoEditarProducto(Producto p){
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Editar MARCA / REFERENCIA");
        DialogPane pane = dialog.getDialogPane();
        cargarCSSDark(pane);
        pane.setStyle("-fx-background-color: #1e1e1e;");

        TextField txtMarca = new TextField(p.getMarcaNombre());
        txtMarca.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
        TextField txtRef = new TextField(p.getReferencia());
        txtRef.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
        TextField txtNombre = new TextField(p.getNombre());
        txtNombre.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");

        VBox box = new VBox(8,
                new Label("Marca:"), txtMarca,
                new Label("Referencia:"), txtRef,
                new Label("Nombre:"), txtNombre
        );
        box.setPadding(new Insets(15));
        box.setStyle("-fx-background-color: #1e1e1e;");
        box.getChildren().forEach(n -> { if(n instanceof Label) n.setStyle("-fx-text-fill: #888;"); });

        pane.setContent(box);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button btnOk = (Button) pane.lookupButton(ButtonType.OK);
        btnOk.setText("💾 Guardar");
        btnOk.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");

        dialog.showAndWait().ifPresent(r -> {
            if(r == ButtonType.OK){
                try{
                    Marca m = marca.obtenerPorId(p.getMarcaId());
                    if(m != null && !m.getNombre().equalsIgnoreCase(txtMarca.getText().trim())){
                        m.setNombre(txtMarca.getText().trim().toUpperCase());
                        marca.actualizar(m);
                    }
                    dao.actualizarDatosProducto(p.getId(), txtRef.getText().trim().toUpperCase(), txtNombre.getText().trim().toUpperCase(), p.getMarcaId());
                    cargarMarcas();
                    cargarPagina(pagination.getCurrentPageIndex(), txtBuscar.getText());
                }catch(Exception ex){ ex.printStackTrace(); }
            }
        });
    }


    @FXML
    private void onBackup() {
        try {
            var result = com.tutienda.inventario.util.BackupService.crearBackup();
            if (result.ok) {
                com.tutienda.inventario.util.Alerts.info("Backup OK", "Backup creado en:\n" + result.archivo);
            } else {
                com.tutienda.inventario.util.Alerts.error("Error Backup", result.error, null);
            }
        } catch (Exception e) {
            com.tutienda.inventario.util.Alerts.error("Error Backup", e.getMessage(), e);
        }
    }

    @FXML
    private void onRestore() {
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Seleccionar backup .sql");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Backups","*.sql","*.backup","*.dump"));
            fc.setInitialDirectory(new java.io.File("backups").exists() ? new java.io.File("backups") : new java.io.File("."));
            java.io.File sel = fc.showOpenDialog(tabla.getScene().getWindow());
            if (sel == null) return;
            javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Restaurar Backup");
            confirm.setHeaderText("¿Restaurar desde:\n" + sel.getName() + " ?");
            confirm.setContentText("Esto SOBRESCRIBIRÁ los datos actuales de tienda_inventario.\n¿Continuar?");
            if (confirm.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != javafx.scene.control.ButtonType.OK) return;
            String pgRestore = com.tutienda.inventario.util.ConfigService.getInstance().getPgDumpPath().replace("pg_dump.exe","pg_restore.exe").replace("pg_dump","pg_restore");
            // Si es .sql plano usamos psql
            com.tutienda.inventario.util.Alerts.info("Restore", "Para restaurar .sql usa:\npsql -U postgres -d tienda_inventario -f \""+sel.getAbsolutePath()+"\"\n\nDesde CMD. pg_restore para .backup");
        } catch (Exception e) {
            com.tutienda.inventario.util.Alerts.error("Error Restore", e.getMessage(), e);
        }
    }

}
