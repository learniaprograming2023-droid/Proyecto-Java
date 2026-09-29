package com.tutienda.inventario.util;

import com.tutienda.inventario.service.BackupService;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

/** Acciones de interfaz relacionadas con respaldo y restauración. */
final class BackupActions {
    private BackupActions() {
    }

    static void crearBackup() {
        try {
            var resultado = BackupService.crearBackup();
            if (resultado.ok) {
                Alerts.info("Backup OK", "Backup creado en:\n" + resultado.archivo);
            } else {
                Alerts.error("Error Backup", resultado.error, null);
            }
        } catch (Exception error) {
            Alerts.error("Error Backup", error.getMessage(), error);
        }
    }

    static void solicitarRestauracion(Window owner) {
        try {
            FileChooser selector = new FileChooser();
            selector.setTitle("Seleccionar backup");
            selector.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Backups", "*.sql", "*.backup", "*.dump"));
            File directorioBackups = new File("backups");
            selector.setInitialDirectory(directorioBackups.exists() ? directorioBackups : new File("."));

            File archivo = selector.showOpenDialog(owner);
            if (archivo == null || !confirmarRestauracion(archivo)) {
                return;
            }
            Alerts.info("Restore", "Para restaurar .sql usa:\npsql -U postgres -d tienda_inventario -f \""
                    + archivo.getAbsolutePath() + "\"\n\nDesde CMD. Usa pg_restore para .backup.");
        } catch (Exception error) {
            Alerts.error("Error Restore", error.getMessage(), error);
        }
    }

    private static boolean confirmarRestauracion(File archivo) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Restaurar Backup");
        confirmacion.setHeaderText("¿Restaurar desde:\n" + archivo.getName() + "?");
        confirmacion.setContentText("Esto SOBRESCRIBIRÁ los datos actuales de tienda_inventario.\n¿Continuar?");
        return confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
}
