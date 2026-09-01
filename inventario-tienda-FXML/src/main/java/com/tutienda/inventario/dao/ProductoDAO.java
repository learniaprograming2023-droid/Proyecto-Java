package com.tutienda.inventario.dao;

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
                SELECT p.*, m.nombre AS marca_nombre
                FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id
                WHERE p.referencia ILIKE ? OR p.nombre ILIKE ? OR m.nombre ILIKE ?
                ORDER BY p.id LIMIT ? OFFSET ?
                """;
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (filtro == null ? "" : filtro) + "%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            ps.setInt(4, pageSize); ps.setInt(5, page * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto p = new Producto();
                    p.setId(rs.getInt("id"));
                    p.setMarcaId(rs.getInt("marca_id"));
                    p.setMarcaNombre(rs.getString("marca_nombre"));
                    p.setReferencia(rs.getString("referencia"));
                    p.setNombre(rs.getString("nombre"));
                    p.setFotoRuta(rs.getString("foto_ruta"));
                    lista.add(p);
                }
            }
            // --- OPTIMIZACIÓN: 1 sola query para TODAS las tallas ---
            if(!lista.isEmpty()){
                String ids = lista.stream().map(pr -> String.valueOf(pr.getId())).collect(java.util.stream.Collectors.joining(","));
                String sqlTallas = "SELECT producto_id, talla, cantidad FROM inventario WHERE producto_id IN ("+ids+")";
                try(Statement st = conn.createStatement(); ResultSet rsT = st.executeQuery(sqlTallas)){
                    Map<Integer, Map<Integer,Integer>> mapa = new java.util.HashMap<>();
                    while(rsT.next()){
                        int pid = rsT.getInt("producto_id");
                        mapa.computeIfAbsent(pid, k-> new java.util.HashMap<>())
                                .put(rsT.getInt("talla"), rsT.getInt("cantidad"));
                    }
                    for(Producto p : lista){
                        p.setTallas(mapa.getOrDefault(p.getId(), new java.util.HashMap<>()));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo listar los productos. Intenta de nuevo.", e);
        }
        return lista;
    }
    public Map<Integer, Integer> getTallas(int productoId) {
        Map<Integer, Integer> map = new HashMap<>();
        String sql = "SELECT talla, cantidad FROM inventario WHERE producto_id = ?";
        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getInt("talla"), rs.getInt("cantidad"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo cargar las tallas del producto " + productoId, e);
        }
        return map;
    }

    public void registrarMovimiento(int productoId, int talla, String tipo, int cantidad, String observacion) {
        String sqlGet = "SELECT cantidad FROM inventario WHERE producto_id=? AND talla=?";
        String sqlMov = "INSERT INTO movimientos(producto_id, talla, tipo, cantidad, stock_anterior, stock_nuevo, observacion, fecha) VALUES(?,?,?,?,?,?,?, NOW())";
        String sqlUpsert = "INSERT INTO inventario(producto_id, talla, cantidad) VALUES(?,?,?) ON CONFLICT (producto_id, talla) DO UPDATE SET cantidad=EXCLUDED.cantidad";

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);
            int anterior = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlGet)) {
                ps.setInt(1, productoId); ps.setInt(2, talla);
                try (ResultSet rs = ps.executeQuery()) { if (rs.next()) anterior = rs.getInt(1); }
            }
            int nuevo = tipo.equals("ENTRADA") ? anterior + cantidad : anterior - cantidad;
            if (nuevo < 0) { conn.rollback(); throw new DatabaseException("Stock negativo no permitido. Stock actual: " + anterior, null); }
            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, productoId); ps.setInt(2, talla); ps.setString(3, tipo); ps.setInt(4, cantidad);
                ps.setInt(5, anterior); ps.setInt(6, nuevo); ps.setString(7, observacion); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                ps.setInt(1, productoId); ps.setInt(2, talla); ps.setInt(3, nuevo); ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            throw new DatabaseException("Error movimiento: "+e.getMessage(), e);
        }
    }

    // METODO NUEVO PARA CAMBIOS - ATOMICO
    public void realizarCambio(int productoOrigenId, int tallaOrigen, int productoDestinoId, int tallaDestino, int cantidad, String motivo) {
        String sqlGet = "SELECT cantidad FROM inventario WHERE producto_id=? AND talla=?";
        String sqlMov = "INSERT INTO movimientos(producto_id, talla, tipo, cantidad, stock_anterior, stock_nuevo, observacion, fecha) VALUES(?,?,?,?,?,?,?, NOW())";
        String sqlUpsert = "INSERT INTO inventario(producto_id, talla, cantidad) VALUES(?,?,?) ON CONFLICT (producto_id, talla) DO UPDATE SET cantidad=EXCLUDED.cantidad";

        try (Connection conn = ConexionDB.getConnection()) {
            conn.setAutoCommit(false);

            int stockOrigen = 0, stockDestino = 0;
            try (PreparedStatement ps = conn.prepareStatement(sqlGet)) {
                ps.setInt(1, productoOrigenId); ps.setInt(2, tallaOrigen);
                try (ResultSet rs = ps.executeQuery()) { if(rs.next()) stockOrigen = rs.getInt(1); }
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlGet)) {
                ps.setInt(1, productoDestinoId); ps.setInt(2, tallaDestino);
                try (ResultSet rs = ps.executeQuery()) { if(rs.next()) stockDestino = rs.getInt(1); }
            }

            if (stockDestino < cantidad) { conn.rollback(); throw new DatabaseException("No hay stock en destino. Talla "+tallaDestino+" solo tiene "+stockDestino, null); }

            String obsCambio = "CAMBIO: Devuelve P"+productoOrigenId+" T"+tallaOrigen+" -> Lleva P"+productoDestinoId+" T"+tallaDestino+" | Motivo: "+motivo;

            // 1. Entrada del devuelto
            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, productoOrigenId); ps.setInt(2, tallaOrigen); ps.setString(3, "ENTRADA");
                ps.setInt(4, cantidad); ps.setInt(5, stockOrigen); ps.setInt(6, stockOrigen+cantidad);
                ps.setString(7, obsCambio); ps.executeUpdate();
            }
            // 2. Salida del nuevo
            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, productoDestinoId); ps.setInt(2, tallaDestino); ps.setString(3, "SALIDA");
                ps.setInt(4, cantidad); ps.setInt(5, stockDestino); ps.setInt(6, stockDestino-cantidad);
                ps.setString(7, obsCambio); ps.executeUpdate();
            }
            // 3. Actualizar inventarios
            try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                ps.setInt(1, productoOrigenId); ps.setInt(2, tallaOrigen); ps.setInt(3, stockOrigen+cantidad); ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(sqlUpsert)) {
                ps.setInt(1, productoDestinoId); ps.setInt(2, tallaDestino); ps.setInt(3, stockDestino-cantidad); ps.executeUpdate();
            }

            conn.commit();
            System.out.println("✓ CAMBIO OK: "+obsCambio);
        } catch (SQLException e) { throw new DatabaseException("Error en cambio: "+e.getMessage(), e); }
    }

    public void crearMarca(String nombre) {
        String sql = "INSERT INTO marcas (nombre) VALUES (?) ON CONFLICT (nombre) DO NOTHING";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nombre); ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("No se pudo crear la marca " + nombre, e); }
    }

    public int contar(String filtro) {
        String sql = "SELECT COUNT(*) FROM productos p LEFT JOIN marcas m ON p.marca_id = m.id WHERE p.referencia ILIKE ? OR p.nombre ILIKE ? OR m.nombre ILIKE ?";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + (filtro == null ? "" : filtro) + "%";
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { throw new DatabaseException("No se pudo contar los productos.", e); }
        return 0;
    }

    public List<Marca> listarMarcas() {
        List<Marca> list = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM marcas ORDER BY nombre")) {
            while (rs.next()) list.add(new Marca(rs.getInt("id"), rs.getString("nombre")));
        } catch (SQLException e) { throw new DatabaseException("No se pudieron cargar las marcas.", e); }
        return list;
    }

    public void actualizarFoto(int productoId, String ruta) {
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE productos SET foto_ruta = ? WHERE id = ?")) {
            ps.setString(1, ruta); ps.setInt(2, productoId); ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("No se pudo guardar la ruta de la foto.", e); }
    }

    public void actualizarDatosProducto(int productoId, String nuevaReferencia, String nuevoNombre, int nuevaMarcaId) {
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement("UPDATE productos SET referencia = ?, nombre = ?, marca_id = ? WHERE id = ?")) {
            ps.setString(1, nuevaReferencia); ps.setString(2, nuevoNombre); ps.setInt(3, nuevaMarcaId); ps.setInt(4, productoId); ps.executeUpdate();
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) throw new ValidationException("Ya existe un producto con esa referencia para esa marca.");
            throw new DatabaseException("No se pudo actualizar el producto.", e);
        }
    }

    public ResultadoCrearProducto crearProducto(String referencia, String nombre, int marcaId) {
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement("INSERT INTO productos (referencia, nombre, marca_id) VALUES (?, ?, ?)")) {
            ps.setString(1, referencia); ps.setString(2, nombre); ps.setInt(3, marcaId); ps.executeUpdate(); return ResultadoCrearProducto.CREADO;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) return ResultadoCrearProducto.DUPLICADO;
            throw new DatabaseException("Error al crear el producto " + referencia, e);
        }
    }

    public void actualizarCantidad(int productoId, int talla, int nueva) {
        String sql = "INSERT INTO inventario(producto_id, talla, cantidad) VALUES(?,?,?) ON CONFLICT (producto_id, talla) DO UPDATE SET cantidad=EXCLUDED.cantidad";
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId); ps.setInt(2, talla); ps.setInt(3, nueva); ps.executeUpdate();
        } catch (SQLException e) { throw new DatabaseException("Error actualizar cantidad", e); }
    }
}