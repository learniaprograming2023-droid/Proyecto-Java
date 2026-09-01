package com.tutienda.inventario.util;

import javafx.scene.image.Image;
import java.io.File;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class FotoService {
    private FotoService(){}

    private static final long MAX_SIZE = 15 * 1024 * 1024;
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private static Path getFotosDir(){
        Path dir = Paths.get(System.getProperty("user.home"), "tienda_inventario", "fotos");
        try{ Files.createDirectories(dir); } catch(Exception e){}
        return dir;
    }

    public static String guardarFoto(int productoId, File archivoOrigen) throws Exception {
        if(archivoOrigen==null || !archivoOrigen.exists()) throw new IllegalArgumentException("Archivo no existe");
        if(archivoOrigen.length() > MAX_SIZE) throw new IllegalArgumentException("Imagen >15MB");

        String name = archivoOrigen.getName().toLowerCase();
        if(!(name.endsWith(".jpg")||name.endsWith(".jpeg")||name.endsWith(".png")||name.endsWith(".webp"))){
            throw new IllegalArgumentException("Solo JPG, PNG, WEBP");
        }
        String ext = name.substring(name.lastIndexOf('.'));
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        Path destino = getFotosDir().resolve("prod_"+productoId+"_"+ts+ext);
        Files.copy(archivoOrigen.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);
        CACHE.clear();
        return destino.toAbsolutePath().toString();
    }

    public static Image loadImage(String ruta, double w, double h){
        if(ruta==null || ruta.isBlank()) return null;
        try{
            File f = new File(ruta);
            if(!f.exists()) return null;
            return new Image(f.toURI().toString(), w, h, true, true, true);
        }catch(Exception e){ return null; }
    }

    public static void borrarFotoSiExiste(String ruta) {
        if(ruta==null || ruta.isEmpty()) return;
        try{ Files.deleteIfExists(Paths.get(ruta)); } catch(Exception e){}
    }
}