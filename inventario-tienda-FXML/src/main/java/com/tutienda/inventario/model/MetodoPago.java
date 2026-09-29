package com.tutienda.inventario.model;

public enum MetodoPago {
    TODOS("Todos"),
    EFECTIVO("Efectivo"),
    NEQUI("Nequi"),
    DATAFONO("Datafono"),
    BANCOLOMBIA("Bancolombia"),
    SISTECREDITO("Sistecredito"),
    ADDI("Addi"),
    SEPARADO("Separado");

    private final String etiqueta;

    MetodoPago(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}