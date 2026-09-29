package com.tutienda.inventario.util;

import javafx.scene.image.Image;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ImageCache {
    private static final ImageCache INSTANCE = new ImageCache();
    private final Map<String, Image> cache = new ConcurrentHashMap<>();

    private ImageCache() {}

    public static ImageCache getInstance() { return INSTANCE; }

    public Image get(File f) {
        String key = f.getAbsolutePath() + "_" + f.lastModified();
        return cache.computeIfAbsent(key, k -> new Image(f.toURI().toString(), 90, 55, true, true, true));
    }

    public void invalidate(String path) {
        cache.keySet().removeIf(k -> k.startsWith(path));
    }
}