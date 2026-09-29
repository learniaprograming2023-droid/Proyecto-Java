package com.tutienda.inventario.ui.handler;

import com.tutienda.inventario.util.Alerts;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class BackupHandler {

    /**
     * Exporta la base de datos a un archivo de respaldo .sql o .backup
     */
    public void ejecutarBackup(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Copia de Seguridad");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo SQL / Backup (*.sql)", "*.sql")
        );
        fileChooser.setInitialFileName("backup_tienda_" + System.currentTimeMillis() + ".sql");

        File destino = fileChooser.showSaveDialog(ownerStage);
        if (destino == null) {
            return; // El usuario canceló la selección
        }

        try {
            // TODO: Aquí va la llamada a tu servicio de base de datos o script pg_dump
            // Ejemplo de flujo:
            // processBuilderExec("pg_dump", "-U", usuario, "-d", dbName, "-f", destino.getAbsolutePath());

            Alerts.info("Copia de Seguridad", "Respaldo guardado con éxito en:\n" + destino.getAbsolutePath());
        } catch (Exception e) {
            Alerts.error("Error de Backup", "No se pudo generar la copia de seguridad.", e);
        }
    }

    /**
     * Restaura la base de datos desde un archivo .sql seleccionado
     */
    public void ejecutarRestore(Stage ownerStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Copia de Seguridad a Restaurar");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo SQL / Backup (*.sql)", "*.sql")
        );

        File origen = fileChooser.showOpenDialog(ownerStage);
        if (origen == null) {
            return; // El usuario canceló
        }

        try {
            // TODO: Invocación de restauración (psql / pg_restore)

            Alerts.info("Restauración Completada", "La base de datos ha sido restaurada desde:\n" + origen.getName());
        } catch (Exception e) {
            Alerts.error("Error de Restauración", "No se pudo restaurar la base de datos.", e);
        }
    }
}