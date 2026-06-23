package com.photoswipe.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photoswipe.app.data.Photo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    trashedPhotos: List<Photo>,
    onBack: () -> Unit,
    onRestore: (Photo) -> Unit,
    onConfirmDelete: (List<Photo>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var photosPendingDelete by remember { mutableStateOf<List<Photo>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Papelera (${trashedPhotos.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (trashedPhotos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("La papelera está vacía", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(trashedPhotos, key = { it.id }) { photo ->
                        val isSelected = selectedIds.contains(photo.id)
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                            )
                            IconButton(
                                onClick = {
                                    selectedIds = if (isSelected) {
                                        selectedIds - photo.id
                                    } else {
                                        selectedIds + photo.id
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Seleccionada",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val toRestore = if (selectedIds.isEmpty()) trashedPhotos else
                                trashedPhotos.filter { selectedIds.contains(it.id) }
                            toRestore.forEach(onRestore)
                            selectedIds = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (selectedIds.isEmpty()) "Restaurar todas" else "Restaurar selección")
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE74C3C)),
                        onClick = {
                            photosPendingDelete = if (selectedIds.isEmpty()) trashedPhotos else
                                trashedPhotos.filter { selectedIds.contains(it.id) }
                            showConfirmDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text(
                            if (selectedIds.isEmpty()) "Vaciar papelera" else "Eliminar selección",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Eliminar definitivamente?") },
            text = {
                Text(
                    "Se va a pedir confirmación del sistema para borrar " +
                        "${photosPendingDelete.size} foto(s) de tu dispositivo. " +
                        "Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    onConfirmDelete(photosPendingDelete)
                    selectedIds = emptySet()
                }) {
                    Text("Eliminar", color = Color(0xFFE74C3C))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
