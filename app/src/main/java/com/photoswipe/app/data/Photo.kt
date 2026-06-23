package com.photoswipe.app.data

import android.net.Uri

/**
 * Representa una foto encontrada en el almacenamiento del dispositivo.
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long
)
