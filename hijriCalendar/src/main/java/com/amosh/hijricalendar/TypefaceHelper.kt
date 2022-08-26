package com.amosh.hijricalendar

import android.content.Context
import android.graphics.Typeface
import androidx.collection.SimpleArrayMap

object TypefaceHelper {
    private val cache = SimpleArrayMap<String, Typeface>()
    operator fun get(c: Context, name: String): Typeface? {
        synchronized(cache) {
            if (!cache.containsKey(name)) {
                val t = Typeface.createFromAsset(
                    c.assets, String.format("fonts/%s.ttf", name))
                cache.put(name, t)
                return t
            }
            return cache[name]
        }
    }
}
