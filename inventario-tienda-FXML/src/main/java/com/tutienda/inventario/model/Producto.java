package com.tutienda.inventario.model;

import java.util.HashMap;
import java.util.Map;

public class Producto {
    private int id;
    private int marcaId;
    private String marcaNombre;
    private String referencia;
    private String nombre;
    private String fotoRuta;
    private Map<Integer, Integer> tallas = new HashMap<>();

    public int getTotal() {
        return tallas.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getMarcaId() { return marcaId; }
    public void setMarcaId(int marcaId) { this.marcaId = marcaId; }
    public String getMarcaNombre() { return marcaNombre; }
    public void setMarcaNombre(String marcaNombre) { this.marcaNombre = marcaNombre; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getFotoRuta() { return fotoRuta; }
    public void setFotoRuta(String fotoRuta) { this.fotoRuta = fotoRuta; }
    public Map<Integer, Integer> getTallas() { return tallas; }
    public void setTallas(Map<Integer, Integer> tallas) { this.tallas = tallas; }
    
    public String getDisplay() { return referencia + " - " + marcaNombre + " " + nombre; }
}
