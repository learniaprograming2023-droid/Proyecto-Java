package com.tutienda.inventario.util;

/**
 * Reglas de paginación de inventario, independientes de JavaFX.
 */
final class InventarioPagination {
    private InventarioPagination() {
    }

    static int pageCount(int totalItems, int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("El tamaño de página debe ser mayor que cero");
        }
        return Math.max(1, (int) Math.ceil((double) Math.max(totalItems, 0) / pageSize));
    }

    static int clampPageIndex(int pageIndex, int pageCount) {
        return Math.max(0, Math.min(pageIndex, Math.max(1, pageCount) - 1));
    }
}
