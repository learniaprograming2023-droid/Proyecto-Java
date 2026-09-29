package com.tutienda.inventario.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public final class ConexionDB {

    private static final HikariDataSource DS;

    static {
        HikariConfig config = new HikariConfig();
        try {
            Properties p = new Properties();
            try (InputStream in = ConexionDB.class.getClassLoader().getResourceAsStream("config.properties")) {
                if (in != null) {
                    p.load(in);
                    String host = p.getProperty("db.host","localhost");
                    String port = p.getProperty("db.port","5432");
                    String name = p.getProperty("db.name","tienda_inventario");
                    config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", host, port, name));
                    config.setUsername(p.getProperty("db.user","postgres"));
                    config.setPassword(p.getProperty("db.password","123456"));
                } else {
                    config.setJdbcUrl("jdbc:postgresql://localhost:5432/tienda_inventario");
                    config.setUsername("postgres");
                    config.setPassword("123456");
                }
            }
            // --- Configuración Senior ---
            config.setMaximumPoolSize(10); // Soporta 5000 productos sin caerse
            config.setMinimumIdle(2);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(30000);
            config.setLeakDetectionThreshold(2000); // Qodana: Detecta leaks
            config.setPoolName("InventarioPool");

            DS = new HikariDataSource(config);
            Runtime.getRuntime().addShutdownHook(new Thread(DS::close)); // Cierre automático
            System.out.println("✓ HikariCP Pool OK: " + config.getJdbcUrl());

        } catch (Exception e) {
            throw new ExceptionInInitializerError("✗ Error inicializando HikariPool: " + e.getMessage());
        }
    }

    private ConexionDB() {}

    public static Connection getConnection() throws SQLException {
        try {
            return DS.getConnection();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("✗ Error inicializando HikariPool: " + e.getMessage());
        }
    }

    public static boolean isHealthy() {
        try (Connection c = DS.getConnection()) {
            return c.isValid(2);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("✗ Error inicializando HikariPool: " + e.getMessage());
        }
    }

    public static void shutdown() {
        if (DS != null && !DS.isClosed()) {
            DS.close();
        }
    }
}















