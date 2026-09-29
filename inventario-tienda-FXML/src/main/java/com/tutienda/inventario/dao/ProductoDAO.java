package com.tutienda.inventario.dao;

import com.tutienda.inventario.config.ConexionDB;
import com.tutienda.inventario.exception.DatabaseException;
import com.tutienda.inventario.exception.ValidationException;
import com.tutienda.inventario.model.Marca;
import com.tutienda.inventario.model.Producto;

import java.sql.*;
import java.util.*;

public class ProductoDAO {

    public List<Producto> listar(String filtro, int page, int pageSize) {
        List<Producto> lista = new ArrayList<>();
        String sql = """
                SELECT p.id, p.marca_id, p.referencia, p.nombre, p.foto_ruta, m.nombre AS marca_nombre
                FROM productos p 
                LEFT JOIN marcas m ON p.marca_id = m.id
                WHERE p.referencia ILIKE ? OR p.nombre ILIKE ? OR m.nombre ILIKE ?
                ORDER BY p.id ASC
                LIMIT ? OFFSET ?
                """;

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String likePattern = "%" + (filtro == null ? "" : filtro.trim()) + "%";
            ps.setString(1, likePattern);
            ps.setString(2, likePattern);
            ps.setString(3, likePattern);
            ps.setInt(4, pageSize);
            ps.setInt(5, page * pageSize);

            Map<Integer, Producto> mapProductos = new LinkedHashMap<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getInt("id"));
                    p.setMarcaId(rs.getInt("marca_id"));
                    p.setMarcaNombre(rs.getString("marca_nombre"));
                    p.setReferencia(rs.getString("referencia"));
                    p.setNombre(rs.getString("nombre"));
                    p.setFotoRuta(rs.getString("foto_ruta"));
                    p.setTallas(new HashMap<>());

                    lista.add(p);
                    mapProductos.put(p.getId(), p);
                }
            }

            if (!mapProductos.isEmpty()) {
                String sqlTallas = "SELECT producto_id, talla, cantidad FROM inventario WHERE producto_id = ANY(?)";
                try (PreparedStatement psTallas = conn.prepareStatement(sqlTallas)) {
                    Integer[] idsArray = mapProductos.keySet().toArray(new Integer[0]);
                    Array sqlArray = conn.createArrayOf("INTEGER", idsArray);
                    psTallas.setArray(1, sqlArray);

                    try (ResultSet rsT = psTallas.executeQuery()) {
                        while (rsT.next()) {
                            int pid = rsT.getInt("producto_id");
                            int talla = rsT.getInt("talla");
                            int cant = rsT.getInt("cantidad");

                            Producto prod = mapProductos.get(pid);
                            if (prod != null) {
                                prod.getTallas().put(talla, cant);
                            }
                        }
                    }
                    sqlArray.free();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar catálogo de productos.", e);
        }
        return lista;
    }

    public int contar(String filtro) {
        String sql = """
                SELECT COUNT(*) 
                FROM productos p 
                LEFT JOIN marcas m ON p.marca_id = m.id 
                WHERE p.referencia ILIKE ? OR p.nombre ILIKE ? OR m.nombre ILIKE ?
                """;
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String likePattern = "%" + (filtro == null ? "" : filtro.trim()) + "%";
            ps.setString(1, likePattern);
            ps.setString(2, likePattern);
            ps.setString(3, likePattern);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar total de registros.", e);
        }
        return 0;
    }

    public void registrarMovimiento(int productoId, int talla, String tipo, int cantidad, String observacion) {
        String sqlSelect = "SELECT cantidad FROM inventario WHERE producto_id=? AND talla=? FOR UPDATE";
        String sqlInsertMov = """
                INSERT INTO movimientos(producto_id, talla, tipo, cantidad, stock_anterior, stock_nuevo, observacion, fecha) 
                VALUES(?,?,?,?,?,?,?, NOW())
                """;
        String sqlUpsertInv = """
                INSERT INTO inventario(producto_id, talla, cantidad) 
                VALUES(?,?,?) 
                ON CONFLICT (producto_id, talla) DO UPDATE SET cantidad=EXCLUDED.cantidad
                """;

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int anterior = 0;
                try (PreparedStatement psSelect = conn.prepareStatement(sqlSelect)) {
                    psSelect.setInt(1, productoId);
                    psSelect.setInt(2, talla);
                    try (ResultSet rs = psSelect.executeQuery()) {
                        if (rs.next()) anterior = rs.getInt("cantidad");
                    }
                }

                int nuevo = "ENTRADA".equalsIgnoreCase(tipo) ? anterior + cantidad : anterior - cantidad;
                if (nuevo < 0) {
                    throw new ValidationException("Stock insuficiente para talla " + talla + ". Stock actual: " + anterior);
                }

                try (PreparedStatement psMov = conn.prepareStatement(sqlInsertMov)) {
                    psMov.setInt(1, productoId);
                    psMov.setInt(2, talla);
                    psMov.setString(3, tipo);
                    psMov.setInt(4, cantidad);
                    psMov.setInt(5, anterior);
                    psMov.setInt(6, nuevo);
                    psMov.setString(7, observacion);
                    psMov.executeUpdate();
                }

                try (PreparedStatement psInv = conn.prepareStatement(sqlUpsertInv)) {
                    psInv.setInt(1, productoId);
                    psInv.setInt(2, talla);
                    psInv.setInt(3, nuevo);
                    psInv.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ValidationException) throw (ValidationException) e;
                throw new DatabaseException("Error al procesar movimiento de inventario.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error de conexión al registrar movimiento.", e);
        }
    }

    public void realizarCambio(int origenId, int tallaOrigen, int destinoId, int tallaDestino, int cantidad, String motivo) {
        String sqlSelect = "SELECT cantidad FROM inventario WHERE producto_id=? AND talla=? FOR UPDATE";
        String sqlMov = """
                INSERT INTO movimientos(producto_id, talla, tipo, cantidad, stock_anterior, stock_nuevo, observacion, fecha) 
                VALUES(?,?,?,?,?,?,?, NOW())
                """;
        String sqlUpsert = """
                INSERT INTO inventario(producto_id, talla, cantidad) 
                VALUES(?,?,?) 
                ON CONFLICT (producto_id, talla) DO UPDATE SET cantidad=EXCLUDED.cantidad
                """;

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int stockOrigen = 0, stockDestino = 0;

                try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                    ps.setInt(1, origenId); ps.setInt(2, tallaOrigen);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) stockOrigen = rs.getInt(1); }
                }

                try (PreparedStatement ps = conn.prepareStatement(sqlSelect)) {
                    ps.setInt(1, destinoId); ps.setInt(2, tallaDestino);
                    try (ResultSet rs = ps.executeQuery()) { if (rs.next()) stockDestino = rs.getInt(1); }
                }

                if (stockDestino < cantidad) {
                    throw new ValidationException("Stock insuficiente en destino. Talla " + tallaDestino + " disponible: " + stockDestino);
                }

                String obsCambio = "CAMBIO: Devuelve P#" + origenId + " T" + tallaOrigen + " -> Lleva P#" + destinoId + " T" + tallaDestino + " | Motivo: " + motivo;

                // 1. Entrada del producto devuelto
                try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                    ps.setInt(1, origenId); ps.setInt(2, tallaOrigen); ps.setString(3, "CAMBIO_DEVOLUCION");
                    ps.setInt(4, cantidad); ps.setInt(5, stockOrigen); ps.setInt(6, stockOrigen + cantidad);
                    ps.setString(7, obsCambio); ps.executeUpdate();
                }

                // 2. Salida del producto entregado
                try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                    ps.setInt(1, destinoId); ps.setInt(2, tallaDestino); ps.setString(3, "CAMBIO_ENTREGA");
                    ps.setInt(4, cantidad); ps.setInt(5, stockDestino); ps.setInt(6, stockDestino - cantidad);
                    ps.setString(7, obsCambio); ps.executeUpdate();
                }

                // 3. Actualizar tablas de inventario
                try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                    ps.setInt(1, origenId); ps.setInt(2, tallaOrigen); ps.setInt(3, stockOrigen + cantidad); ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                    ps.setInt(1, destinoId); ps.setInt(2, tallaDestino); ps.setInt(3, stockDestino - cantidad); ps.executeUpdate();
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof ValidationException) throw (ValidationException) e;
                throw new DatabaseException("Error en transacción de cambio de producto.", e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error de conexión en cambio de producto.", e);
        }
    }

    public List<Marca> listarMarcas() {
        List<Marca> list = new ArrayList<>();
        String sql = "SELECT id, nombre FROM marcas ORDER BY nombre ASC";
        try (Connection conn = ConexionDB.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Marca(rs.getInt("id"), rs.getString("nombre")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener listado de marcas.", e);
        }
        return list;
    }

    public void crearMarca(String nombre) {
        String sql = "INSERT INTO marcas (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre.trim().toUpperCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo crear la marca " + nombre, e);
        }
    }

    public Producto crearProducto(String referencia, String nombre, int marcaId) {
        String sql = "INSERT INTO productos (referencia, nombre, marca_id) VALUES (?, ?, ?) RETURNING id";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, referencia.trim());
            ps.setString(2, nombre.trim());
            ps.setInt(3, marcaId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getInt(1));
                    p.setReferencia(referencia);
                    p.setNombre(nombre);
                    p.setMarcaId(marcaId);
                    p.setTallas(new HashMap<>());
                    return p;
                }
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new ValidationException("La referencia '" + referencia + "' ya existe en la base de datos.");
            }
            throw new DatabaseException("Error al registrar el producto.", e);
        }
        throw new DatabaseException("No se pudo obtener el ID del producto creado.", null);
    }

    public void actualizarFoto(int productoId, String ruta) {
        String sql = "UPDATE productos SET foto_ruta = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ruta);
            ps.setInt(2, productoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo actualizar la ruta de la imagen.", e);
        }
    }

    public void actualizarDatosProducto(int productoId, String nuevaReferencia, String nuevoNombre, int nuevaMarcaId) {
        String sql = "UPDATE productos SET referencia = ?, nombre = ?, marca_id = ? WHERE id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nuevaReferencia.trim());
            ps.setString(2, nuevoNombre.trim());
            ps.setInt(3, nuevaMarcaId);
            ps.setInt(4, productoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new ValidationException("La referencia '" + nuevaReferencia + "' ya pertenece a otro producto.");
            }
            throw new DatabaseException("Error al actualizar la información del producto.", e);
        }
    }
    public Map<Integer, Integer> getTallas(int productoId) {
        Map<Integer, Integer> tallas = new HashMap<>();
        String sql = "SELECT talla, cantidad FROM inventario WHERE producto_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tallas.put(rs.getInt("talla"), rs.getInt("cantidad"));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al obtener las tallas del producto " + productoId, e);
        }
        return tallas;
    }
    public void guardarOActualizarTalla(int productoId, int talla, int cantidad) {
        String sql = """
                INSERT INTO inventario (producto_id, talla, cantidad) 
                VALUES (?, ?, ?) 
                ON CONFLICT (producto_id, talla) 
                DO UPDATE SET cantidad = EXCLUDED.cantidad
                """;

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, productoId);
            ps.setInt(2, talla);
            ps.setInt(3, cantidad);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("Error al guardar/actualizar talla para el producto " + productoId, e);
        }
    }
}