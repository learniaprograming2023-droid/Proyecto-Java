package com.tutienda.inventario.util;

import com.tutienda.inventario.model.Marca;

public final class Validador {

    private Validador(){}

    public static String validarYNormalizarReferencia(String ref) {
        if(ref==null || ref.trim().isEmpty()) throw new IllegalArgumentException("Referencia vacía");
        String r = ref.trim().toUpperCase().replaceAll("\\s+", " ");
        if(r.length()<2) throw new IllegalArgumentException("Referencia muy corta");
        return r;
    }

    public static String validarYNormalizarNombre(String nombre) {
        if(nombre==null || nombre.trim().isEmpty()) throw new IllegalArgumentException("Nombre vacío");
        String n = nombre.trim().toUpperCase().replaceAll("\\s+", " ");
        if(n.length()<2) throw new IllegalArgumentException("Nombre muy corto");
        return n;
    }

    public static Marca validarMarcaSeleccionada(Marca marca) {
        if(marca==null) throw new IllegalArgumentException("Selecciona una marca");
        return marca;
    }

    public static String validarYNormalizarMarca(String nombre) {
        if(nombre==null || nombre.trim().isEmpty()) throw new IllegalArgumentException("Marca vacía");
        String n = nombre.trim().toUpperCase().replaceAll("\\s+", " ");
        if(n.length()<2) throw new IllegalArgumentException("Marca muy corta");
        return n;
    }
}