package com.tutienda.inventario.model;

import com.tutienda.inventario.dao.ConexionDB;
import com.tutienda.inventario.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Marca {
    private int id;
    private String nombre;

    public Marca() {}

    public Marca(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    @Override
    public String toString() { return nombre; }

    public Marca obtenerPorId(int id){
        String sql = "SELECT * FROM marcas WHERE id = ?";
        try(Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setInt(1, id);
            try(ResultSet rs = ps.executeQuery()){
                if(rs.next()) return new Marca(rs.getInt("id"), rs.getString("nombre"));
            }
        } catch(SQLException e){ throw new DatabaseException("No se pudo obtener marca", e); }
        return null;
    }

    public void actualizar(Marca marca){
        String sql = "UPDATE marcas SET nombre = ? WHERE id = ?";
        try(Connection conn = ConexionDB.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)){
            ps.setString(1, marca.getNombre());
            ps.setInt(2, marca.getId());
            ps.executeUpdate();
        } catch(SQLException e){ throw new DatabaseException("No se pudo actualizar marca", e); }
    }
}

