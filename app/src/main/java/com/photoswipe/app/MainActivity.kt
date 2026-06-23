package com.photoswipe.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.photoswipe.app.data.Photo
import com.photoswipe.app.data.PhotoRepository
import com.photoswipe.app.ui.PhotoSwipeScreen
import com.photoswipe.app.ui.theme.PhotoSwipeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PhotoSwipeTheme {
                PhotoSwipeApp()
            }
        }
    }
}

@Composable
fun PhotoSwipeApp() {
    val context = LocalContext.current

    var hasPermission by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    // Foto que estamos esperando confirmar borrado (tras el diálogo del sistema)
    var pendingDeletePhoto by remember { mutableStateOf<Photo?>(null) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // Launcher para el diálogo nativo de "¿Borrar este elemento?"
    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // El usuario confirmó el borrado en el diálogo del sistema.
            pendingDeletePhoto?.let { deleted ->
                photos = photos.filterNot { it.id == deleted.id }
            }
        }
        // Si cancela, no quitamos la foto de la lista: simplemente se queda donde estaba.
        pendingDeletePhoto = null
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            photos = PhotoRepository.loadAllPhotos(context)
        }
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            hasPermission = true
            photos = PhotoRepository.loadAllPhotos(context)
        } else {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    PhotoSwipeScreen(
        hasPermission = hasPermission,
        photos = photos,
        onKeep = { photo ->
            // "Guardar": simplemente quitamos de la cola en memoria, no se toca el archivo.
            photos = photos.filterNot { it.id == photo.id }
        },
        onDeleteRequest = { photo ->
            // "Eliminar": pedimos confirmación real del sistema para borrar el archivo.
            pendingDeletePhoto = photo
            val deleteRequest = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(photo.uri)
            )
            val senderRequest = IntentSenderRequest.Builder(deleteRequest.intentSender).build()
            deleteRequestLauncher.launch(senderRequest)
        },
        onRequestPermissionAgain = {
            permissionLauncher.launch(permissionToRequest)
        }
    )
}
