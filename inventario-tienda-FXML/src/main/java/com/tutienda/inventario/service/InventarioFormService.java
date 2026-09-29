package com.tutienda.inventario.service;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.Validador;

/** Reglas de validación y persistencia de los formularios de inventario. */
public class InventarioFormService {
    private final ProductoDAO productoDAO;

    public InventarioFormService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public void crearMarca(String nombre) {
        productoDAO.crearMarca(Validador.validarYNormalizarMarca(nombre));
    }

    public Producto crearProducto(String referencia, String nombre, Marca marca) {
        String referenciaNormalizada = Validador.validarYNormalizarReferencia(referencia);
        String nombreNormalizado = Validador.validarYNormalizarNombre(nombre);
        Marca marcaValidada = Validador.validarMarcaSeleccionada(marca);
        return productoDAO.crearProducto(referenciaNormalizada, nombreNormalizado, marcaValidada.getId());
    }

    public String normalizarReferencia(String referencia) {
        return Validador.validarYNormalizarReferencia(referencia);
    }
}
