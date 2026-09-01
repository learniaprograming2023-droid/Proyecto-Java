
package com.tutienda.inventario.util;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

public class ConfigService {
    private static ConfigService instance;
    private final Properties props = new Properties();

    private ConfigService() {
        try (InputStream is = getClass().getResourceAsStream("/config.properties")) {
            if (is != null) props.load(is);
            else System.out.println("WARN: config.properties no encontrado, usando defaults");
        } catch (Exception e) { e.printStackTrace(); }
    }
    public static ConfigService getInstance() {
        if (instance == null) instance = new ConfigService();
        return instance;
    }
    public String getDbUrl() { return props.getProperty("db.url", "jdbc:postgresql://localhost:5432/inventario_tienda"); }
    public String getDbUser() { return props.getProperty("db.user", "postgres"); }
    public String getDbPassword() { return props.getProperty("db.password", "1234"); }
    public String getPgDumpPath() {
        String p = props.getProperty("pg_dump.path", "");
        if (p.isBlank()) {
            // autodetect profesional PostgreSQL 18 / 17 / 16
            String[] tryPaths = {
                "C:\\Program Files\\PostgreSQL\\18\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\17\\bin\\pg_dump.exe",
                "C:\\Program Files\\PostgreSQL\\16\\bin\\pg_dump.exe"
            };
            for (String tp : tryPaths) if (new File(tp).exists()) return tp;
            return tryPaths[0]; // devuelve 18 aunque no exista, no crashea
        }
        return p;
    }
}
