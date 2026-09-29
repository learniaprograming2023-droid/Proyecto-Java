package com.tutienda.inventario.service;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Producto;

/** Reglas para registrar entradas, salidas y ventas de inventario. */
public class MovimientoService {
    private final ProductoDAO productoDAO;

    public MovimientoService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public void registrar(Producto producto, int talla, String tipo, int cantidad,
                         String metodoPago, String observacion) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad debe ser >0");
        }
        String observacionFinal = observacion == null ? "" : observacion;
        if ("VENTA".equals(tipo)) {
            observacionFinal = "[PAGO: " + metodoPago + "] " + observacionFinal;
        }
        productoDAO.registrarMovimiento(producto.getId(), talla, tipo, cantidad, observacionFinal);
        producto.setTallas(productoDAO.getTallas(producto.getId()));
    }
}
