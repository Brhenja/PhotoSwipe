package com.photoswipe.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Mantiene la lista de fotos que el usuario marcó para eliminar, pero que
 * todavía no fueron borradas de verdad. El borrado real del archivo sólo
 * ocurre cuando el usuario lo confirma explícitamente desde la pantalla de
 * Papelera (ver TrashScreen / MainActivity).
 *
 * Es un singleton en memoria: vive mientras la app esté abierta.
 */
object TrashRepository {

    private val _items: SnapshotStateList<Photo> = mutableStateListOf()

    val items: SnapshotStateList<Photo> get() = _items

    fun add(photo: Photo) {
        if (_items.none { it.id == photo.id }) {
            _items.add(photo)
        }
    }

    fun restore(photo: Photo) {
        _items.removeAll { it.id == photo.id }
    }

    fun remove(photo: Photo) {
        _items.removeAll { it.id == photo.id }
    }

    fun clear() {
        _items.clear()
    }

    fun contains(photoId: Long): Boolean = _items.any { it.id == photoId }
}
