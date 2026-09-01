package com.tutienda.inventario.model;
import java.time.LocalDateTime;
public class Movimiento {
    public int id, productoId, talla, cantidad, stockAnterior, stockNuevo;
    public String tipo, observacion, productoRef, marcaNombre;
    public LocalDateTime fecha;
    public String productoNombre;// agrega

    // GETTERS para que compile la plantilla de columnas
    public int getId(){ return id; }
    public int getProductoId(){ return productoId; }
    public int getTalla(){ return talla; }
    public int getCantidad(){ return cantidad; }
    public int getStockAnterior(){ return stockAnterior; }
    public int getStockNuevo(){ return stockNuevo; }
    public String getTipo(){ return tipo; }
    public String getObservacion(){ return observacion; }
    public String getProductoRef(){ return productoRef; }
    public String getMarcaNombre(){ return marcaNombre; }
    public String getProductoNombre(){ return productoNombre; }
    public LocalDateTime getFecha(){ return fecha; }

    // SETTERS que usa tu DAO
    public void setId(int id){ this.id=id; }
    public void setProductoId(int v){ this.productoId=v; }
    public void setTalla(int v){ this.talla=v; }
    public void setCantidad(int v){ this.cantidad=v; }
    public void setStockAnterior(int v){ this.stockAnterior=v; }
    public void setStockNuevo(int v){ this.stockNuevo=v; }
    public void setTipo(String v){ this.tipo=v; }
    public void setObservacion(String v){ this.observacion=v; }
    public void setProductoRef(String v){ this.productoRef=v; }
    public void setMarcaNombre(String v){ this.marcaNombre=v; }
    public void setProductoNombre(String v){ this.productoNombre=v; }
    public void setFecha(LocalDateTime v){ this.fecha=v; }

}