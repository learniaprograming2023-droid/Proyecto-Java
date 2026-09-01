package com.tutienda.inventario.dao;

import com.tutienda.inventario.exception.DatabaseException;
import com.tutienda.inventario.model.Movimiento;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimientoDAO {

    public List<Movimiento> listarPorProducto(int productoId) {
        String sql = "SELECT m.*, p.referencia, p.nombre as producto_nombre, ma.nombre as marca_nombre " +
                "FROM movimientos m JOIN productos p ON p.id = m.producto_id " +
                "JOIN marcas ma ON ma.id = p.marca_id WHERE m.producto_id = ? ORDER BY m.fecha DESC LIMIT 100";
        List<Movimiento> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productoId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new DatabaseException("Error listando historial", e); }
        return lista;
    }

    public List<Movimiento> listarTodos(String filtro) {
        String sql = "SELECT m.*, p.referencia, p.nombre as producto_nombre, ma.nombre as marca_nombre " +
                "FROM movimientos m JOIN productos p ON p.id = m.producto_id " +
                "JOIN marcas ma ON ma.id = p.marca_id " +
                "WHERE p.referencia ILIKE ? OR p.nombre ILIKE ? OR ma.nombre ILIKE ? " +
                "ORDER BY m.fecha DESC LIMIT 200";
        List<Movimiento> lista = new ArrayList<>();
        try (Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            String f = "%" + filtro + "%";
            ps.setString(1, f); ps.setString(2, f); ps.setString(3, f);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) { throw new DatabaseException("Error listando historial general", e); }
        return lista;
    }

    private Movimiento mapear(ResultSet rs) throws SQLException {
        Movimiento m = new Movimiento();
        m.id = rs.getInt("id");
        m.productoId = rs.getInt("producto_id");
        m.talla = rs.getInt("talla");
        m.tipo = rs.getString("tipo");
        m.cantidad = rs.getInt("cantidad");
        try { m.stockAnterior = rs.getInt("stock_anterior"); } catch(Exception e){ m.stockAnterior = 0; }
        try { m.stockNuevo = rs.getInt("stock_nuevo"); } catch(Exception e){ m.stockNuevo = 0; }
        m.observacion = rs.getString("observacion");
        Timestamp ts = rs.getTimestamp("fecha");
        if(ts!=null) m.fecha = ts.toLocalDateTime();
        m.productoRef = rs.getString("referencia");
        m.productoNombre = rs.getString("producto_nombre");
        m.marcaNombre = rs.getString("marca_nombre");
        return m;
    }
    // Agrega arriba de la clase
    public static record PaginaHistorial(List<Movimiento> movimientos, LocalDateTime lastFecha, int lastId, boolean hasMore, long totalCount) {}

    public PaginaHistorial listarPaginaGeneral(String filtro, LocalDate desde, LocalDate hasta, String tipoFiltro, LocalDateTime cursorFecha, Integer cursorId, int pageSize){
        List<Movimiento> lista = new ArrayList<>();
        long total = 0;

        try{
            // 1. COUNT total
            StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM movimientos m JOIN productos p ON p.id=m.producto_id JOIN marcas ma ON ma.id=p.marca_id WHERE m.fecha::date BETWEEN ? AND ? ");
            List<Object> countParams = new ArrayList<>();
            countParams.add(java.sql.Date.valueOf(desde));
            countParams.add(java.sql.Date.valueOf(hasta));
            if(tipoFiltro != null && !tipoFiltro.equals("TODOS")){
                countSql.append(" AND m.tipo = ? ");
                countParams.add(tipoFiltro);
            }
            if(filtro!=null && !filtro.isBlank()){
                countSql.append(" AND (p.referencia ILIKE ? OR ma.nombre ILIKE ? OR m.observacion ILIKE ?) ");
                String like="%"+filtro+"%";
                countParams.add(like); countParams.add(like); countParams.add(like);
            }
            try(Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(countSql.toString())){
                for(int i=0;i<countParams.size();i++){
                    if(countParams.get(i) instanceof java.sql.Date) ps.setDate(i+1,(java.sql.Date)countParams.get(i));
                    else ps.setString(i+1,(String)countParams.get(i));
                }
                try(ResultSet rs=ps.executeQuery()){ if(rs.next()) total=rs.getLong(1); }
            }

            // 2. DATA con Keyset Pagination
            StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.producto_id, m.talla, m.tipo, m.cantidad, m.stock_anterior, m.stock_nuevo, m.observacion, m.fecha,
                   p.referencia as producto_ref, p.nombre as producto_nombre, ma.nombre as marca_nombre
            FROM movimientos m
            JOIN productos p ON p.id=m.producto_id
            JOIN marcas ma ON ma.id=p.marca_id
            WHERE m.fecha::date BETWEEN ? AND ?
        """);
            List<Object> params = new ArrayList<>();
            params.add(java.sql.Date.valueOf(desde));
            params.add(java.sql.Date.valueOf(hasta));

            if(tipoFiltro != null && !tipoFiltro.equals("TODOS")){
                sql.append(" AND m.tipo = ? ");
                params.add(tipoFiltro);
            }

            if(cursorFecha != null && cursorId != null){
                sql.append(" AND (m.fecha, m.id) < (?, ?) ");
                params.add(java.sql.Timestamp.valueOf(cursorFecha));
                params.add(cursorId);
            }
            if(filtro!=null && !filtro.isBlank()){
                sql.append(" AND (p.referencia ILIKE ? OR ma.nombre ILIKE ? OR m.observacion ILIKE ?) ");
                String like="%"+filtro+"%";
                params.add(like); params.add(like); params.add(like);
            }
            sql.append(" ORDER BY m.fecha DESC, m.id DESC LIMIT ? ");
            params.add(pageSize + 1);

            try(Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())){
                for(int i=0;i<params.size();i++){
                    Object o = params.get(i);
                    if(o instanceof java.sql.Date) ps.setDate(i+1,(java.sql.Date)o);
                    else if(o instanceof java.sql.Timestamp) ps.setTimestamp(i+1,(java.sql.Timestamp)o);
                    else if(o instanceof Integer) ps.setInt(i+1,(Integer)o);
                    else ps.setString(i+1,(String)o);
                }
                try(ResultSet rs=ps.executeQuery()){
                    while(rs.next()){
                        Movimiento m = new Movimiento();
                        m.id = rs.getInt("id"); m.productoId=rs.getInt("producto_id"); m.talla=rs.getInt("talla");
                        m.tipo=rs.getString("tipo"); m.cantidad=rs.getInt("cantidad");
                        m.stockAnterior=rs.getInt("stock_anterior"); m.stockNuevo=rs.getInt("stock_nuevo");
                        m.observacion=rs.getString("observacion");
                        java.sql.Timestamp ts=rs.getTimestamp("fecha"); if(ts!=null) m.fecha=ts.toLocalDateTime();
                        m.productoRef=rs.getString("producto_ref"); m.productoNombre=rs.getString("producto_nombre"); m.marcaNombre=rs.getString("marca_nombre");
                        lista.add(m);
                    }
                }
            }

            boolean hasMore = lista.size() > pageSize;
            if(hasMore) lista.remove(lista.size()-1);
            LocalDateTime lastFecha = lista.isEmpty()? null : lista.get(lista.size()-1).fecha;
            int lastId = lista.isEmpty()? 0 : lista.get(lista.size()-1).id;

            return new PaginaHistorial(lista, lastFecha, lastId, hasMore, total);

        }catch(Exception e){ throw new DatabaseException("Error paginación general: "+e.getMessage(), e); }
    }

    // Compatibilidad con el viejo
    public PaginaHistorial listarPaginaGeneral(String filtro, LocalDate desde, LocalDate hasta, LocalDateTime cursorFecha, Integer cursorId, int pageSize){
        return listarPaginaGeneral(filtro, desde, hasta, "TODOS", cursorFecha, cursorId, pageSize);
    }

}