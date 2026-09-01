package com.tutienda.inventario.util;

import javafx.scene.image.Image;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();
    private static final ImageCache INSTANCE = new ImageCache();
    private ImageCache(){}
    public static ImageCache getInstance(){ return INSTANCE; }

    public Image get(File f, double w, double h) {
        if (f == null || !f.exists()) return null;
        String key = f.getAbsolutePath() + "_" + f.lastModified() + "_" + w + "x" + h;
        return CACHE.computeIfAbsent(key, k -> {
            try {
                return new Image(f.toURI().toString(), w, h, true, true, true);
            } catch (Exception e) { return null; }
        });
    }
    public void invalidate(String path) {
        if (path == null) return;
        CACHE.keySet().removeIf(k -> k.startsWith(path));
    }
    public void clear(){ CACHE.clear(); }
}
