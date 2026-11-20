package com.sportapp.tiendasportmobile.util

import com.sportapp.tiendasportmobile.BuildConfig

/**
 * Las fotos subidas desde la página web se guardan como "/api/imagenes/<id>" (ruta relativa).
 * Esta función les antepone la dirección del servidor (la de BuildConfig.BASE_URL) para que
 * Glide pueda descargarlas. Las direcciones completas (https://...) se dejan igual.
 */
object ImageUrl {
    fun resolver(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (!url.startsWith("/api/")) return url
        val origen = BuildConfig.BASE_URL.trimEnd('/').removeSuffix("/api")
        return origen + url
    }
}
