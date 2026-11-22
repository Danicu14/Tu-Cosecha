package com.danielcarpio.tucosecha.util;

import android.graphics.Bitmap;
import android.util.LruCache;

public class CacheBitmap {
    private static final int TAMANO_CACHE = 4 * 1024 * 1024; // 4MB
    private static final LruCache<String, Bitmap> cacheLru = new LruCache<String, Bitmap>(TAMANO_CACHE) {
        @Override
        protected int sizeOf(String clave, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    public static Bitmap obtenerBitmap(String clave) {
        return cacheLru.get(clave);
    }

    public static void guardarBitmap(String clave, Bitmap bitmap) {
        if (clave != null && bitmap != null) {
            cacheLru.put(clave, bitmap);
        }
    }
}
