package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.service.InventarioService;
import com.tutienda.inventario.util.Alerts;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Label;
import javafx.scene.control.Pagination;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.function.BiConsumer;

public class BusquedaPaginadaHandler {

    private final InventarioService inventarioService;
    private final int pageSize;

    public BusquedaPaginadaHandler(InventarioService inventarioService, int pageSize) {
        this.inventarioService = inventarioService;
        this.pageSize = pageSize;
    }

    /**
     * Ejecuta la búsqueda asíncrona, calcula páginas y refresca los indicadores visuales.
     */
    public void ejecutarBusqueda(
            String filtro,
            Pagination pagination,
            ProgressIndicator progressIndicator,
            Label lblInfo,
            BiConsumer<Integer, String> cargarPaginaConsumer
    ) {
        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<Integer> countTask = new Task<>() {
            @Override
            protected Integer call() {
                return inventarioService.contar(filtro);
            }
        };

        countTask.setOnSucceeded(e -> {
            int total = countTask.getValue();
            int pageCount = (int) Math.ceil((double) total / pageSize);
            pageCount = Math.max(pageCount, 1);

            pagination.setPageCount(pageCount);
            if (lblInfo != null) {
                lblInfo.setText(total + " modelos registrados");
            }

            int currentPage = Math.min(pagination.getCurrentPageIndex(), pageCount - 1);
            cargarPaginaConsumer.accept(currentPage, filtro);
        });

        countTask.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            Alerts.error("Error BD", "Falló la consulta de recuento.", countTask.getException());
        });

        new Thread(countTask).start();
    }

    public void cargarPaginaAsync(
            int pageIndex,
            String filtro,
            TableView<Producto> tabla,
            Pagination pagination,
            ProgressIndicator progressIndicator,
            Label lblPaginaInfo
    ) {
        if (progressIndicator != null) progressIndicator.setVisible(true);

        Task<List<Producto>> listTask = new Task<>() {
            @Override
            protected List<Producto> call() {
                return inventarioService.listar(filtro, pageIndex, pageSize);
            }
        };

        listTask.setOnSucceeded(e -> {
            tabla.setItems(FXCollections.observableArrayList(listTask.getValue()));
            if (lblPaginaInfo != null && pagination != null) {
                lblPaginaInfo.setText("Página " + (pageIndex + 1) + " / " + pagination.getPageCount());
            }
            if (progressIndicator != null) progressIndicator.setVisible(false);
        });

        listTask.setOnFailed(e -> {
            if (progressIndicator != null) progressIndicator.setVisible(false);
            Alerts.error("Error BD", "No se pudo cargar la página solicitada.", listTask.getException());
        });

        new Thread(listTask).start();
    }
}