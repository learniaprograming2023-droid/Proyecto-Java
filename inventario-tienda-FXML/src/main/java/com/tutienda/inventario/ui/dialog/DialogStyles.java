package com.tutienda.inventario.ui.dialog;

import javafx.scene.control.DialogPane;

final class DialogStyles {
    private static final String[] DARK_STYLE_SHEETS = {
            "/css/dialog-dark.css",
            "/css/dark-theme.css",
            "/com/tutienda/inventario/css/dialog-dark.css",
            "/com/tutienda/inventario/ui/dark-dialog.css"
    };

    private DialogStyles() {
    }

    static void applyDarkTheme(DialogPane pane, Class<?> resourceOwner) {
        for (String path : DARK_STYLE_SHEETS) {
            var url = resourceOwner.getResource(path);
            if (url != null) {
                pane.getStylesheets().add(url.toExternalForm());
                return;
            }
        }
    }
}
