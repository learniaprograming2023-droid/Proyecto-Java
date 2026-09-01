package com.tutienda.inventario.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;

public final class Alerts {
    private Alerts(){}

    public static void info(String title, String msg){
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
    public static void error(String title, String msg, Throwable ex){
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title); a.setHeaderText(msg);
        if(ex!=null){
            TextArea ta = new TextArea(ex.getMessage());
            ta.setEditable(false); ta.setWrapText(true);
            VBox box = new VBox(new Label("Detalle:"), ta);
            a.getDialogPane().setExpandableContent(box);
        }
        a.showAndWait();
    }
}