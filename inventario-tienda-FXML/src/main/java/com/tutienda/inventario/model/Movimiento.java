package com.tutienda.inventario.model;

import java.time.LocalDateTime;

public class Movimiento {

    private int id;
    private int productoId;
    private int talla;
    private int cantidad;
    private int stockAnterior;
    private int stockNuevo;
    private double precio; // Monto o valor del movimiento/venta
    private String tipo;
    private String metodoPago; // Efectivo, Nequi, Datafono, Bancolombia, Sistecredito, Addi, Separado
    private String observacion;
    private String productoRef;
    private String productoNombre;
    private String marcaNombre;
    private LocalDateTime fecha;

    // --- GETTERS ---
    public int getId() { return id; }
    public int getProductoId() { return productoId; }
    public int getTalla() { return talla; }
    public int getCantidad() { return cantidad; }
    public int getStockAnterior() { return stockAnterior; }
    public int getStockNuevo() { return stockNuevo; }
    public double getPrecio() { return precio; }
    public String getTipo() { return tipo; }
    public String getMetodoPago() { return metodoPago; }
    public String getObservacion() { return observacion; }
    public String getProductoRef() { return productoRef; }
    public String getProductoNombre() { return productoNombre; }
    public String getMarcaNombre() { return marcaNombre; }
    public LocalDateTime getFecha() { return fecha; }

    // --- SETTERS ---
    public void setId(int id) { this.id = id; }
    public void setProductoId(int productoId) { this.productoId = productoId; }
    public void setTalla(int talla) { this.talla = talla; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setStockAnterior(int stockAnterior) { this.stockAnterior = stockAnterior; }
    public void setStockNuevo(int stockNuevo) { this.stockNuevo = stockNuevo; }
    public void setPrecio(double precio) { this.precio = precio; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
    public void setProductoRef(String productoRef) { this.productoRef = productoRef; }
    public void setProductoNombre(String productoNombre) { this.productoNombre = productoNombre; }
    public void setMarcaNombre(String marcaNombre) { this.marcaNombre = marcaNombre; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}