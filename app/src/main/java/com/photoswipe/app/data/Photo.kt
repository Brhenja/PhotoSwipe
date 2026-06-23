package com.photoswipe.app.data

import android.net.Uri

/**
 * Representa una foto encontrada en el almacenamiento local del dispositivo.
 *
 * Nota sobre Google Fotos: la Google Photos Library API eliminó el scope de
 * solo lectura (photoslibrary.readonly) en marzo de 2025. Desde entonces,
 * ninguna app de terceros puede listar ni leer la galería completa de Google
 * Fotos de un usuario; solo puede acceder a contenido que la propia app subió.
 * Por eso PhotoSwipe trabaja únicamente con las fotos locales del dispositivo
 * vía MediaStore, que es además donde Google Fotos ya respalda las fotos del
 * usuario en la mayoría de los casos.
 */
data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long
)
