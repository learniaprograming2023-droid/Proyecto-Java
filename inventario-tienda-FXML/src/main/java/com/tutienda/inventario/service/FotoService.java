package com.tutienda.inventario.service;

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
    // FIX QODANA: Cache que SI se consulta
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private static Path getFotosDir(){
        Path dir = Paths.get(System.getProperty("user.home"), "tienda_inventario", "fotos");
        try{
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.err.println("Error creando dir fotos: " + e.getMessage());
        }
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

        // FIX: invalidamos solo esa entrada, no todo el cache
        CACHE.remove(destino.toAbsolutePath().toString());
        return destino.toAbsolutePath().toString();
    }

    public static Image loadImage(String ruta, double w, double h){
        if(ruta==null || ruta.isBlank()) return null;

        String key = ruta + "_" + w + "_" + h;
        // FIX QODANA: Ahora SI consultamos el CACHE
        Image cached = CACHE.get(key);
        if (cached != null) return cached;

        try{
            File f = new File(ruta);
            if(!f.exists()) return null;
            Image img = new Image(f.toURI().toString(), w, h, true, true, true);
            if (!img.isError()) {
                CACHE.put(key, img);
            }
            return img;
        } catch(Exception e){
            // FIX QODANA: Empty catch block -> ahora loguea
            System.err.println("Error loadImage ["+ruta+"]: " + e.getMessage());
            return null;
        }
    }

    public static void borrarFotoSiExiste(String ruta) {
        if(ruta==null || ruta.isEmpty()) return;
        try{
            Files.deleteIfExists(Paths.get(ruta));
            CACHE.entrySet().removeIf(entry -> entry.getKey().startsWith(ruta));
        } catch (Exception e) {
            System.err.println("Error borrando foto: " + e.getMessage());
        }
    }

    public static void limpiarCache() {
        CACHE.clear();
    }
}