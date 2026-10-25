package com.danielcarpio.tucosecha.util;

import android.graphics.Bitmap;
import android.util.LruCache;

public class BitmapCache {
    private static final int cacheSize = 4 * 1024 * 1024; // 4MB
    private static final LruCache<String, Bitmap> lruCache = new LruCache<String, Bitmap>(cacheSize) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return bitmap.getByteCount();
        }
    };

    public static Bitmap getBitmap(String key) {
        return lruCache.get(key);
    }

    public static void putBitmap(String key, Bitmap bitmap) {
        if (key != null && bitmap != null) {
            lruCache.put(key, bitmap);
        }
    }
}
