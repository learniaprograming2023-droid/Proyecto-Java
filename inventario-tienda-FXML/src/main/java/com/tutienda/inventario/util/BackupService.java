
package com.tutienda.inventario.util;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class BackupService {
    private static final ConfigService cfg = ConfigService.getInstance();

    public static class ResultadoBackup {
        public final boolean ok; public final String archivo; public final String error;
        public ResultadoBackup(boolean ok, String archivo, String error) { this.ok=ok; this.archivo=archivo; this.error=error; }
    }

    public static ResultadoBackup crearBackup() {
        try {
            String pgDump = cfg.getPgDumpPath();
            File fDump = new File(pgDump);
            if (!fDump.exists()) {
                return new ResultadoBackup(false, null, "pg_dump no encontrado en: " + pgDump + "\nInstala PostgreSQL 18 o corrige pg_dump.path en config.properties");
            }
            File dir = new File("backups");
            if (!dir.exists()) dir.mkdirs();

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            File out = new File(dir, "backup_inventario_" + fecha + ".sql");

            String url = cfg.getDbUrl(); // jdbc:postgresql://localhost:5432/inventario_tienda
            String dbName = url.substring(url.lastIndexOf('/')+1);
            String host = "localhost"; String port="5432";
            try {
                String hostPort = url.substring(url.indexOf("://")+3, url.lastIndexOf('/'));
                if (hostPort.contains(":")) { host = hostPort.split(":")[0]; port = hostPort.split(":")[1]; }
                else host = hostPort;
            } catch(Exception ignore){}

            ProcessBuilder pb = new ProcessBuilder(
                pgDump,
                "-h", host, "-p", port, "-U", cfg.getDbUser(),
                "-F", "c", "-b", "-v", "-f", out.getAbsolutePath(), dbName
            );
            pb.environment().put("PGPASSWORD", cfg.getDbPassword());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder log = new StringBuilder();
            try(BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))){
                String line; while((line=br.readLine())!=null) log.append(line).append("\n");
            }
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished) { p.destroyForcibly(); return new ResultadoBackup(false, null, "Timeout pg_dump\n"+log); }
            if (p.exitValue()==0 && out.exists() && out.length()>0) {
                return new ResultadoBackup(true, out.getAbsolutePath(), null);
            } else {
                return new ResultadoBackup(false, null, "pg_dump exit="+p.exitValue()+"\n"+log);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ResultadoBackup(false, null, e.getMessage());
        }
    }
}
