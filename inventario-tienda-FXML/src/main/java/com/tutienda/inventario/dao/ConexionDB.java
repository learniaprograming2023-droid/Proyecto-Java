package com.tutienda.inventario.dao;

import com.tutienda.inventario.exception.DatabaseException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionDB {

    private static String URL;
    private static String USER;
    private static String PASS;
    private static String ultimoError = null;

    static {
        try {
            Properties p = new Properties();
            try (InputStream in = ConexionDB.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    p.load(in);
                    URL = String.format("jdbc:postgresql://%s:%s/%s",
                            p.getProperty("db.host","localhost"),
                            p.getProperty("db.port","5432"),
                            p.getProperty("db.name","tienda_inventario"));
                    USER = p.getProperty("db.user","postgres");
                    PASS = p.getProperty("db.password","123456");
                } else {
                    // Fallback por si aún no creaste config.properties
                    URL = "jdbc:postgresql://localhost:5432/tienda_inventario";
                    USER = "postgres";
                    PASS = "123456";
                }
            }
            try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
                System.out.println("✓ Conexión OK: " + URL);
            }
        } catch (Exception e) {
            ultimoError = e.getMessage();
            System.err.println("✗ Error inicializando ConexionDB: " + ultimoError);
        }
    }

    private ConexionDB() {}

    public static Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(true);
            return conn;
        } catch (SQLException e) {
            ultimoError = e.getMessage();
            throw new DatabaseException(
                    "No se pudo conectar a PostgreSQL. Verifica que el servicio esté iniciado en Windows.", e);
        }
    }

    public static boolean isHealthy() {
        try (Connection c = getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            ultimoError = e.getMessage();
            return false;
        }
    }

    public static String getUltimoError() { return ultimoError; }
}



















