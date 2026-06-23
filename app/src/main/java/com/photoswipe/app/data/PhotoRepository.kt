package com.photoswipe.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Consulta el MediaStore del sistema para obtener todas las fotos
 * guardadas en el dispositivo (cámara, descargas, WhatsApp, etc).
 */
object PhotoRepository {

    fun loadAllPhotos(context: Context): List<Photo> {
        val photos = mutableListOf<Photo>()

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        // Ordenamos por fecha, las más recientes primero
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val name = it.getString(nameColumn) ?: "Sin nombre"
                val date = it.getLong(dateColumn)

                val uri = ContentUris.withAppendedId(collection, id)

                photos.add(Photo(id = id, uri = uri, displayName = name, dateAddedSeconds = date))
            }
        }

        return photos
    }
}
