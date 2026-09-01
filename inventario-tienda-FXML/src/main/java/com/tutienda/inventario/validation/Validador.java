package com.tutienda.inventario.validation;

import com.tutienda.inventario.exception.ValidationException;
import com.tutienda.inventario.model.Marca;

public final class Validador {

    private Validador() {}

    public static String validarYNormalizarMarca(String input) {
        if (input == null) throw new ValidationException("La marca no puede estar vacía.");
        String v = input.trim().replaceAll("\\s+", " ").toUpperCase();
        if (v.isEmpty()) throw new ValidationException("Escribe el nombre de la marca.");
        if (v.length() < 2) throw new ValidationException("La marca debe tener al menos 2 letras.");
        if (v.length() > 100) throw new ValidationException("La marca es demasiado larga (máx 100).");
        if (!v.matches("[A-Z0-9 ÁÉÍÓÚÑ\\-\\.]+")) throw new ValidationException("La marca solo puede contener letras, números, espacios, guión y punto.");
        return v;
    }

    public static String validarYNormalizarReferencia(String input) {
        if (input == null) throw new ValidationException("La referencia no puede estar vacía.");
        String v = input.trim().replaceAll("\\s+", " ").toUpperCase();
        if (v.isEmpty()) throw new ValidationException("Escribe la referencia del producto. Ej: 001-808");
        if (v.length() < 2) throw new ValidationException("Referencia muy corta.");
        if (v.length() > 100) throw new ValidationException("Referencia muy larga (máx 100).");
        return v;
    }

    public static String validarYNormalizarNombre(String input) {
        if (input == null) throw new ValidationException("El nombre no puede estar vacío.");
        String v = input.trim().replaceAll("\\s+", " "); // Nombre lo dejamos con mayúsculas/minúsculas originales pero sin espacios dobles
        if (v.isEmpty()) throw new ValidationException("Escribe el nombre del producto. Ej: NIKE FOR ONE BLANCO");
        if (v.length() < 3) throw new ValidationException("El nombre debe tener al menos 3 caracteres.");
        if (v.length() > 200) throw new ValidationException("Nombre muy largo (máx 200).");
        return v.toUpperCase(); // Tú ya normalizabas a mayúsculas, lo mantenemos
    }

    public static Marca validarMarcaSeleccionada(Marca marca) {
        if (marca == null) throw new ValidationException("Selecciona una marca de la lista.");
        return marca;
    }

    public static int validarCantidad(Integer cantidad) {
        if (cantidad == null) return 0;
        if (cantidad < 0) throw new ValidationException("La cantidad no puede ser negativa.");
        if (cantidad > 9999) throw new ValidationException("Cantidad máxima 9999 por talla. Si tienes más, usa 2 registros.");
        return cantidad;
    }

    public static void validarTalla(int talla) {
        if (talla < 30 || talla > 50) throw new ValidationException("Talla fuera de rango permitido (30-50).");
    }
}