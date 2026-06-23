package com.photoswipe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photoswipe.app.data.Photo
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PhotoSwipeScreen(
    hasPermission: Boolean,
    photos: List<Photo>,
    onKeep: (Photo) -> Unit,
    onDeleteRequest: (Photo) -> Unit,
    onRequestPermissionAgain: () -> Unit
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF111111)),
            contentAlignment = Alignment.Center
        ) {
            when {
                !hasPermission -> PermissionMessage(onRequestPermissionAgain)
                photos.isEmpty() -> EmptyState()
                else -> {
                    val currentPhoto = photos.first()
                    SwipeablePhotoCard(
                        key = currentPhoto.id,
                        photo = currentPhoto,
                        remaining = photos.size,
                        onKeep = { onKeep(currentPhoto) },
                        onDelete = { onDeleteRequest(currentPhoto) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionMessage(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Necesitamos permiso para ver tus fotos",
            color = Color.White,
            fontSize = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Conceder permiso")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "¡Revisaste todas las fotos!",
            color = Color.White,
            fontSize = 20.sp
        )
    }
}

/**
 * Tarjeta de foto que se puede arrastrar horizontalmente.
 * Deslizar a la derecha más allá del umbral = guardar.
 * Deslizar a la izquierda más allá del umbral = eliminar.
 */
@Composable
private fun SwipeablePhotoCard(
    key: Long,
    photo: Photo,
    remaining: Int,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { 1000.dp.toPx() } // referencia amplia, suficiente como umbral relativo
    val swipeThresholdPx = with(density) { 120.dp.toPx() }

    // offsetX se reinicia automáticamente cuando cambia "key" (nueva foto)
    val offsetX = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragAccumulated by remember(key) { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Contador arriba
        Text(
            text = "$remaining foto${if (remaining == 1) "" else "s"} por revisar",
            color = Color.LightGray,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = (offsetX.value / 40f).coerceIn(-12f, 12f)
                }
                .pointerInput(key) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    dragAccumulated > swipeThresholdPx -> {
                                        // Swipe a la derecha: animamos fuera de pantalla y guardamos
                                        offsetX.animateTo(
                                            targetValue = screenWidthPx,
                                            animationSpec = tween(250)
                                        )
                                        onKeep()
                                    }
                                    dragAccumulated < -swipeThresholdPx -> {
                                        // Swipe a la izquierda: animamos fuera de pantalla y eliminamos
                                        offsetX.animateTo(
                                            targetValue = -screenWidthPx,
                                            animationSpec = tween(250)
                                        )
                                        onDelete()
                                    }
                                    else -> {
                                        // No llegó al umbral: vuelve al centro
                                        offsetX.animateTo(0f, animationSpec = tween(200))
                                    }
                                }
                                dragAccumulated = 0f
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount)
                        }
                    }
                }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.displayName,
                    modifier = Modifier.fillMaxSize()
                )

                // Indicador "GUARDAR" que aparece al arrastrar a la derecha
                if (offsetX.value > 30f) {
                    SwipeBadge(
                        text = "GUARDAR",
                        color = Color(0xFF2ECC71),
                        alignment = Alignment.TopStart,
                        alpha = (offsetX.value / swipeThresholdPx).coerceIn(0f, 1f)
                    )
                }
                // Indicador "ELIMINAR" al arrastrar a la izquierda
                if (offsetX.value < -30f) {
                    SwipeBadge(
                        text = "ELIMINAR",
                        color = Color(0xFFE74C3C),
                        alignment = Alignment.TopEnd,
                        alpha = (-offsetX.value / swipeThreshold
