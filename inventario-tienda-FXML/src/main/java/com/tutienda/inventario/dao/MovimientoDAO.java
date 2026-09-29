package com.tutienda.inventario.dao;

import com.tutienda.inventario.config.ConexionDB;
import com.tutienda.inventario.exception.DatabaseException;
import com.tutienda.inventario.model.Movimiento;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MovimientoDAO {

    public static record PaginaHistorial(List<Movimiento> movimientos, LocalDateTime lastFecha, int lastId, boolean hasMore, long totalCount) {}

    public List<Movimiento> listarPorProducto(int productoId) {
        String sql = "SELECT m.*, p.referencia, p.nombre as producto_nombre, ma.nombre as marca_nombre " +
                "FROM movimientos m JOIN productos p ON p.id = m.producto_id " +
                "JOIN marcas ma ON ma.id = p.marca_id WHERE m.producto_id = ? ORDER BY m.fecha DESC LIMIT 100";
        List<Movimiento> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Error listando historial", e);
        }
        return lista;
    }


    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.setId(rs.getInt("id"));
        m.setProductoId(rs.getInt("producto_id"));
        m.setTalla(rs.getInt("talla"));
        m.setCantidad(rs.getInt("cantidad"));

        try { m.setStockAnterior(rs.getInt("stock_anterior")); } catch(Exception e) { m.setStockAnterior(0); }
        try { m.setStockNuevo(rs.getInt("stock_nuevo")); } catch(Exception e) { m.setStockNuevo(0); }
        try { m.setPrecio(rs.getDouble("precio")); } catch(Exception e) { m.setPrecio(0.0); }
        try { m.setMetodoPago(rs.getString("metodo_pago")); } catch(Exception e) { m.setMetodoPago("Efectivo"); }

        m.setTipo(rs.getString("tipo"));
        m.setObservacion(rs.getString("observacion"));

        Timestamp ts = rs.getTimestamp("fecha");
        if(ts != null) m.setFecha(ts.toLocalDateTime());

        m.setProductoRef(rs.getString("referencia"));
        m.setProductoNombre(rs.getString("producto_nombre"));
        m.setMarcaNombre(rs.getString("marca_nombre"));
        return m;
    }

    public PaginaHistorial listarPaginaGeneral(String filtro, LocalDate desde, LocalDate hasta, String tipoFiltro, String metodoPagoFilter, LocalDateTime cursorFecha, Integer cursorId, int pageSize) {
        List<Movimiento> lista = new ArrayList<>();
        long total = 0;

        try {
            // 1. COUNT total
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM movimientos m JOIN productos p ON p.id=m.producto_id JOIN marcas ma ON ma.id=p.marca_id WHERE m.fecha::date BETWEEN ? AND ? ");
            List<Object> countParams = new ArrayList<>();
            countParams.add(java.sql.Date.valueOf(desde));
            countParams.add(java.sql.Date.valueOf(hasta));

            if(tipoFiltro != null && !tipoFiltro.equalsIgnoreCase("TODOS")) {
                countSql.append(" AND m.tipo = ? ");
                countParams.add(tipoFiltro);
            }
            if(metodoPagoFilter != null && !metodoPagoFilter.equalsIgnoreCase("TODOS")) {
                countSql.append(" AND m.metodo_pago ILIKE ? ");
                countParams.add(metodoPagoFilter);
            }
            if(filtro != null && !filtro.isBlank()) {
                countSql.append(" AND (p.referencia ILIKE ? OR p.nombre ILIKE ? OR ma.nombre ILIKE ? OR m.observacion ILIKE ?) ");
                String like = "%" + filtro + "%";
                countParams.add(like); countParams.add(like); countParams.add(like); countParams.add(like);
            }

            try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(countSql.toString())) {
                for (int i = 0; i < countParams.size(); i++) {
                    if (countParams.get(i) instanceof java.sql.Date) ps.setDate(i + 1, (java.sql.Date) countParams.get(i));
                    else ps.setString(i + 1, (String) countParams.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }

            // 2. DATA con Keyset Pagination
            StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.producto_id, m.talla, m.tipo, m.cantidad, m.stock_anterior, m.stock_nuevo, 
                   m.precio, m.metodo_pago, m.observacion, m.fecha,
                   p.referencia, p.nombre as producto_nombre, ma.nombre as marca_nombre
            FROM movimientos m
            JOIN productos p ON p.id=m.producto_id
            JOIN marcas ma ON ma.id=p.marca_id
            WHERE m.fecha::date BETWEEN ? AND ?
        """);
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(desde));
            params.add(java.sql.Date.valueOf(hasta));

            if (tipoFiltro != null && !tipoFiltro.equalsIgnoreCase("TODOS")) {
                sql.append(" AND m.tipo = ? ");
                params.add(tipoFiltro);
            }
            if (metodoPagoFilter != null && !metodoPagoFilter.equalsIgnoreCase("TODOS")) {
                sql.append(" AND m.metodo_pago ILIKE ? ");
                params.add(metodoPagoFilter);
            }
            if (cursorFecha != null && cursorId != null) {
                sql.append(" AND (m.fecha, m.id) < (?, ?) ");
                params.add(java.sql.Timestamp.valueOf(cursorFecha));
                params.add(cursorId);
            }
            if (filtro != null && !filtro.isBlank()) {
                sql.append(" AND (p.referencia ILIKE ? OR p.nombre ILIKE ? OR ma.nombre ILIKE ? OR m.observacion ILIKE ?) ");
                String like = "%" + filtro + "%";
                params.add(like); params.add(like); params.add(like); params.add(like);
            }
            sql.append(" ORDER BY m.fecha DESC, m.id DESC LIMIT ? ");
            params.add(pageSize + 1);

            try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    Object o = params.get(i);
                    if (o instanceof java.sql.Date) ps.setDate(i + 1, (java.sql.Date) o);
                    else if (o instanceof java.sql.Timestamp) ps.setTimestamp(i + 1, (java.sql.Timestamp) o);
                    else if (o instanceof Integer) ps.setInt(i + 1, (Integer) o);
                    else ps.setString(i + 1, (String) o);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }

            boolean hasMore = lista.size() > pageSize;
            if (hasMore) lista.remove(lista.size() - 1);
            LocalDateTime lastFecha = lista.isEmpty() ? null : lista.get(lista.size() - 1).getFecha();
            int lastId = lista.isEmpty() ? 0 : lista.get(lista.size() - 1).getId();

            return new PaginaHistorial(lista, lastFecha, lastId, hasMore, total);

        } catch (Exception e) {
            throw new DatabaseException("Error paginación general: " + e.getMessage(), e);
        }
    }

    // Sobrecarga para compatibilidad
    public PaginaHistorial listarPaginaGeneral(String filtro, LocalDate desde, LocalDate hasta, LocalDateTime cursorFecha, Integer cursorId, int pageSize) {
        return listarPaginaGeneral(filtro, desde, hasta, "TODOS", "TODOS", cursorFecha, cursorId, pageSize);
    }
    /**
     * Actualiza un movimiento existente ajustando el stock de la talla anterior y la nueva de forma atómica.
     */
    public boolean actualizarMovimiento(
            int movimientoId,
            int productoId,
            String tallaViejaStr,
            int nuevaTalla,
            int cantidadVieja,
            int nuevaCantidad,
            String nuevoMetodoPago,
            double nuevoPrecio,
            String tipoMovimiento
    ) {
        int tallaVieja = Integer.parseInt(tallaViejaStr);

        String sqlUpdateMov = "UPDATE movimientos SET talla = ?, cantidad = ?, metodo_pago = ?, precio = ? WHERE id = ?";
        // CAMBIO AQUÍ: inventario_tallas -> inventario
        String sqlDevolverStock = "UPDATE inventario SET cantidad = cantidad + ? WHERE producto_id = ? AND talla = ?";
        String sqlDescontarStock = "UPDATE inventario SET cantidad = cantidad - ? WHERE producto_id = ? AND talla = ?";

        Connection conn = null;
        try {
            conn = ConexionDB.getConnection();
            conn.setAutoCommit(false); // Transacción atómica

            // 1. Revertir el stock de la talla anterior
            try (PreparedStatement psDev = conn.prepareStatement(sqlDevolverStock)) {
                psDev.setInt(1, cantidadVieja);
                psDev.setInt(2, productoId);
                psDev.setInt(3, tallaVieja);
                psDev.executeUpdate();
            }

            // 2. Aplicar el descuento del stock a la nueva talla
            try (PreparedStatement psDesc = conn.prepareStatement(sqlDescontarStock)) {
                psDesc.setInt(1, nuevaCantidad);
                psDesc.setInt(2, productoId);
                psDesc.setInt(3, nuevaTalla);
                psDesc.executeUpdate();
            }

            // 3. Actualizar la tabla de movimientos
            try (PreparedStatement psMov = conn.prepareStatement(sqlUpdateMov)) {
                psMov.setInt(1, nuevaTalla);
                psMov.setInt(2, nuevaCantidad);
                psMov.setString(3, nuevoMetodoPago);
                psMov.setDouble(4, nuevoPrecio);
                psMov.setInt(5, movimientoId);
                psMov.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
        }
    }

    public List<Integer> obtenerTallasDisponibles(int productoId, int tallaActual) {
        Set<Integer> tallasSet = new TreeSet<>();

        // CAMBIO AQUÍ: inventario_tallas -> inventario
        String sqlStock = "SELECT talla FROM inventario WHERE producto_id = ? AND cantidad > 0";

        try (Connection conn = ConexionDB.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlStock)) {

            ps.setInt(1, productoId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tallasSet.add(rs.getInt("talla"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar tallas en BD para producto_id " + productoId + ": " + e.getMessage());
        }

        tallasSet.add(tallaActual);
        return new ArrayList<>(tallasSet);
    }
}