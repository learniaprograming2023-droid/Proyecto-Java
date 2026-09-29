package com.tutienda.inventario.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class FormatUtils {

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter HEADER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("es", "ES"));

    public static String formatoMoneda(double valor) {
        return CURRENCY_FORMAT.format(valor);
    }

    public static String formatoDiaHeader(LocalDate fecha) {
        return fecha != null ? fecha.format(HEADER_DATE_FORMATTER) : "";
    }

    public static String formatoHora(java.time.LocalDateTime fechaHora) {
        return fechaHora != null ? fechaHora.format(TIME_FORMATTER) : "";
    }
}