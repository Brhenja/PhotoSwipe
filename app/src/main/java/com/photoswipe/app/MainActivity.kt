package com.photoswipe.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.photoswipe.app.data.Photo
import com.photoswipe.app.data.PhotoRepository
import com.photoswipe.app.data.TrashRepository
import com.photoswipe.app.ui.AboutScreen
import com.photoswipe.app.ui.PhotoSwipeScreen
import com.photoswipe.app.ui.SplashScreen
import com.photoswipe.app.ui.TrashScreen
import com.photoswipe.app.ui.theme.PhotoSwipeTheme

private enum class AppScreen { SWIPE, TRASH, ABOUT }

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

    var isLoading by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }
    var photos by remember { mutableStateOf<List<Photo>>(emptyList()) }
    var nextOffset by remember { mutableStateOf(0) }
    var currentScreen by remember { mutableStateOf(AppScreen.SWIPE) }

    var deleteQueue by remember { mutableStateOf<List<Photo>>(emptyList()) }

    val permissionToRequest = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun loadNextPage() {
        val page = PhotoRepository.loadPhotosPage(context, nextOffset)
        val filtered = page.filterNot { TrashRepository.contains(it.id) }
        photos = photos + filtered
        nextOffset += page.size
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val current = deleteQueue.firstOrNull()
        if (result.resultCode == android.app.Activity.RESULT_OK && current != null) {
            TrashRepository.remove(current)
        }
        deleteQueue = deleteQueue.drop(1)
    }

    LaunchedEffect(deleteQueue) {
        val next = deleteQueue.firstOrNull()
        if (next != null) {
            val deleteRequest = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(next.uri)
            )
            val senderRequest = IntentSenderRequest.Builder(deleteRequest.intentSender).build()
            deleteRequestLauncher.launch(senderRequest)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (granted) {
            loadNextPage()
        }
        isLoading = false
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context, permissionToRequest
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            hasPermission = true
            loadNextPage()
            isLoading = false
        } else {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    if (isLoading) {
        SplashScreen()
        return
    }

    when (currentScreen) {
        AppScreen.SWIPE -> {
            PhotoSwipeScreen(
                hasPermission = hasPermission,
                photos = photos,
                trashCount = TrashRepository.items.size,
                onKeep = { photo ->
                    photos = photos.filterNot { it.id == photo.id }
                    if (photos.size < 5) loadNextPage()
                },
                onTrash = { photo ->
                    TrashRepository.add(photo)
                    photos = photos.filterNot { it.id == photo.id }
                    if (photos.size < 5) loadNextPage()
                },
                onOpenTrash = { currentScreen = AppScreen.TRASH },
                onOpenAbout = { currentScreen = AppScreen.ABOUT },
                onRequestPermissionAgain = {
                    permissionLauncher.launch(permissionToRequest)
                }
            )
        }

        AppScreen.TRASH -> {
            TrashScreen(
                trashedPhotos = TrashRepository.items,
                onBack = { currentScreen = AppScreen.SWIPE },
                onRestore = { photo ->
                    TrashRepository.restore(photo)
                    photos = listOf(photo) + photos
                },
                onConfirmDelete = { toDelete ->
                    deleteQueue = deleteQueue + toDelete
                }
            )
        }

        AppScreen.ABOUT -> {
            AboutScreen(onBack = { currentScreen = AppScreen.SWIPE })
        }
    }
}
