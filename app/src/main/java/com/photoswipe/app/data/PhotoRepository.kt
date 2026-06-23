package com.photoswipe.app.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore

/**
 * Consulta el MediaStore del sistema para obtener las fotos guardadas en el
 * dispositivo (cámara, descargas, WhatsApp, etc), de forma paginada para no
 * cargar miles de filas de golpe en galerías muy grandes.
 */
object PhotoRepository {

    private const val PAGE_SIZE = 60

    fun loadPhotosPage(context: Context, offset: Int): List<Photo> {
        val photos = mutableListOf<Photo>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val queryArgs = Bundle().apply {
                putString(
                    android.content.ContentResolver.QUERY_ARG_SQL_SORT_ORDER,
                    "${MediaStore.Images.Media.DATE_ADDED} DESC"
                )
                putInt(android.content.ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
                putInt(android.content.ContentResolver.QUERY_ARG_OFFSET, offset)
            }
            context.contentResolver.query(collection, projection, queryArgs, null)
        } else {
            context.contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )
        }

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

            var skipped = 0
            while (it.moveToNext()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && skipped < offset) {
                    skipped++
                    continue
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && photos.size >= PAGE_SIZE) {
                    break
                }

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
