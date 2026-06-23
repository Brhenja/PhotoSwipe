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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photoswipe.app.data.Photo
import kotlinx.coroutines.launch

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

@Composable
private fun SwipeablePhotoCard(
    key: Long,
    photo: Photo,
    remaining: Int,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val screenWidthPx = with(density) { 1000.dp.toPx() }
    val swipeThresholdPx = with(density) { 120.dp.toPx() }

    val offsetX = remember(key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragAccumulated by remember(key) { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
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
                                        offsetX.animateTo(
                                            targetValue = screenWidthPx,
                                            animationSpec = tween(250)
                                        )
                                        onKeep()
                                    }
                                    dragAccumulated < -swipeThresholdPx -> {
                                        offsetX.animateTo(
                                            targetValue = -screenWidthPx,
                                            animationSpec = tween(250)
                                        )
                                        onDelete()
                                    }
                                    else -> {
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

                if (offsetX.value > 30f) {
                    SwipeBadge(
                        text = "GUARDAR",
                        color = Color(0xFF2ECC71),
                        alignment = Alignment.TopStart,
                        alpha = (offsetX.value / swipeThresholdPx).coerceIn(0f, 1f)
                    )
                }
                if (offsetX.value < -30f) {
                    SwipeBadge(
                        text = "ELIMINAR",
                        color = Color(0xFFE74C3C),
                        alignment = Alignment.TopEnd,
                        alpha = (-offsetX.value / swipeThresholdPx).coerceIn(0f, 1f)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE74C3C)),
                onClick = {
                    scope.launch {
                        offsetX.animateTo(-screenWidthPx, animationSpec = tween(250))
                        onDelete()
                    }
                }
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Eliminar", tint = Color.White)
            }

            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2ECC71)),
                onClick = {
                    scope.launch {
                        offsetX.animateTo(screenWidthPx, animationSpec = tween(250))
                        onKeep()
                    }
                }
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Guardar", tint = Color.White)
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeBadge(
    text: String,
    color: Color,
    alignment: Alignment,
    alpha: Float
) {
    Box(
        modifier = Modifier
            .align(alignment)
            .padding(20.dp)
            .background(color.copy(alpha = 0.85f * alpha))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text, color = Color.White, fontSize = 18.sp)
    }
}
