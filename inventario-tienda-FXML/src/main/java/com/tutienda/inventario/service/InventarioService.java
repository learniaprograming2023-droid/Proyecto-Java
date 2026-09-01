package com.tutienda.inventario.service;

import com.tutienda.inventario.dao.ProductoDAO;
import com.tutienda.inventario.dao.MovimientoDAO;
import com.tutienda.inventario.exception.DatabaseException;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;
import com.tutienda.inventario.dao.ResultadoCrearProducto;
import com.tutienda.inventario.util.FotoService;
import com.tutienda.inventario.util.ImageCache;
import java.io.File;
import java.util.List;

public class InventarioService {
    private final ProductoDAO productoDAO;
    private final MovimientoDAO movimientoDAO;
    private final Marca marcaModel;

    public InventarioService(ProductoDAO productoDAO, MovimientoDAO movimientoDAO, Marca marcaModel) {
        this.productoDAO = productoDAO;
        this.movimientoDAO = movimientoDAO;
        this.marcaModel = marcaModel;
    }

    public int contar(String filtro) throws DatabaseException { return productoDAO.contar(filtro); }
    public List<Producto> listar(String filtro, int page, int size) throws DatabaseException { return productoDAO.listar(filtro, page, size); }
    public List<Marca> listarMarcas() throws DatabaseException { return productoDAO.listarMarcas(); }

    public Producto crearProducto(String ref, String nombre, int marcaId) throws DatabaseException {
        ResultadoCrearProducto r = productoDAO.crearProducto(ref, nombre, marcaId);
        if (r == ResultadoCrearProducto.DUPLICADO) throw new DatabaseException("Ya existe " + ref, null);
        // fetch last created - simplificado: listar y buscar ref
        return productoDAO.listar(ref, 0, 1).stream().findFirst().orElse(null);
    }

    public void actualizarFoto(Producto p, File nuevoArchivo) throws Exception {
        String old = p.getFotoRuta();
        String nuevaRuta = FotoService.guardarFoto(p.getId(), nuevoArchivo);
        productoDAO.actualizarFoto(p.getId(), nuevaRuta);
        p.setFotoRuta(nuevaRuta);
        if (old != null) ImageCache.getInstance().invalidate(old);
        FotoService.borrarFotoSiExiste(old);
    }

    public void registrarMovimiento(int productoId, int talla, String tipo, int cant, String obs) throws Exception {
        productoDAO.registrarMovimiento(productoId, talla, tipo, cant, obs);
    }

    public void realizarCambio(int origenId, int tallaOrigen, int destinoId, int tallaDestino, int cant, String motivo) throws Exception {
        productoDAO.realizarCambio(origenId, tallaOrigen, destinoId, tallaDestino, cant, motivo);
    }

    public ProductoDAO getProductoDAO(){ return productoDAO; }
    public MovimientoDAO getMovimientoDAO(){ return movimientoDAO; }
}
