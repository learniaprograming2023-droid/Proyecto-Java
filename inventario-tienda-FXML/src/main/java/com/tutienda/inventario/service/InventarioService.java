package com.tutienda.inventario.service;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.exception.ValidationException;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.util.ImageCache;

import java.io.File;
import java.util.List;

public class InventarioService {

    private final ProductoDAO productoDAO;

    public InventarioService(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public int contar(String filtro) {
        return productoDAO.contar(filtro);
    }

    public List<Producto> listar(String filtro, int page, int size) {
        return productoDAO.listar(filtro, page, size);
    }

    public List<Marca> listarMarcas() {
        return productoDAO.listarMarcas();
    }

    public void crearMarca(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidationException("El nombre de la marca no puede estar vacío.");
        }
        productoDAO.crearMarca(nombre.trim());
    }

    public Producto crearProducto(String ref, String nombre, int marcaId) {
        if (ref == null || ref.isBlank()) throw new ValidationException("La referencia es obligatoria.");
        if (nombre == null || nombre.isBlank()) throw new ValidationException("El nombre del producto es obligatorio.");
        if (marcaId <= 0) throw new ValidationException("Debe seleccionar una marca válida.");

        return productoDAO.crearProducto(ref, nombre, marcaId);
    }

    public void actualizarFoto(Producto p, File nuevoArchivo) throws Exception {
        if (p == null || nuevoArchivo == null) return;
        String oldFoto = p.getFotoRuta();
        String nuevaRuta = FotoService.guardarFoto(p.getId(), nuevoArchivo);

        productoDAO.actualizarFoto(p.getId(), nuevaRuta);
        p.setFotoRuta(nuevaRuta);

        if (oldFoto != null) {
            ImageCache.getInstance().invalidate(oldFoto);
            FotoService.borrarFotoSiExiste(oldFoto);
        }
    }

    public void actualizarDatosProducto(Producto p, String ref, String nombre, int marcaId) {
        if (ref == null || ref.isBlank()) throw new ValidationException("La referencia no puede quedar vacía.");
        if (nombre == null || nombre.isBlank()) throw new ValidationException("El nombre no puede quedar vacío.");

        productoDAO.actualizarDatosProducto(p.getId(), ref, nombre, marcaId);
        p.setReferencia(ref);
        p.setNombre(nombre);
        p.setMarcaId(marcaId);
    }

    public void registrarMovimiento(int productoId, int talla, String tipo, int cantidad, String observacion) {
        if (cantidad <= 0) throw new ValidationException("La cantidad debe ser mayor a cero.");
        productoDAO.registrarMovimiento(productoId, talla, tipo, cantidad, observacion);
    }

    public void realizarCambio(int origenId, int tallaOrigen, int destinoId, int tallaDestino, int cantidad, String motivo) {
        if (cantidad <= 0) throw new ValidationException("La cantidad debe ser mayor a cero.");
        if (origenId == destinoId && tallaOrigen == tallaDestino) {
            throw new ValidationException("El producto/talla de origen y destino no pueden ser exactamente iguales.");
        }
        productoDAO.realizarCambio(origenId, tallaOrigen, destinoId, tallaDestino, cantidad, motivo);
    }
}