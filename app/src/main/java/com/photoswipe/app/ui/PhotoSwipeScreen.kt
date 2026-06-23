package com.photoswipe.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.photoswipe.app.data.Photo
import kotlin.math.abs
import kotlinx.coroutines.launch

@Composable
fun PhotoSwipeScreen(
    hasPermission: Boolean,
    photos: List<Photo>,
    trashCount: Int,
    onKeep: (Photo) -> Unit,
    onTrash: (Photo) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenAbout: () -> Unit,
    onRequestPermissionAgain: () -> Unit
) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF111111))
        ) {
            when {
                !hasPermission -> PermissionMessage(
                    modifier = Modifier.fillMaxSize(),
                    onRetry = onRequestPermissionAgain
                )
                photos.isEmpty() -> EmptyState(modifier = Modifier.fillMaxSize())
                else -> {
                    val currentPhoto = photos.first()
                    SwipeablePhotoCard(
                        key = currentPhoto.id,
                        photo = currentPhoto,
                        remaining = photos.size,
                        onKeep = { onKeep(currentPhoto) },
                        onTrash = { onTrash(currentPhoto) }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TopBarButton(label = "Papelera ($trashCount)", onClick = onOpenTrash)
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onOpenAbout) {
                    Icon(Icons.Filled.Info, contentDescription = "Acerca de", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TopBarButton(label: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.height(18.dp))
        Text(label, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun PermissionMessage(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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
}

@Composable
private fun SwipeablePhotoCard(
    key: Long,
    photo: Photo,
    remaining: Int,
    onKeep: () -> Unit,
    onTrash: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val containerWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) {
            maxWidth.toPx()
        }
        val swipeThresholdPx = with(androidx.compose.ui.platform.LocalDensity.current) { 110.dp.toPx() }

        val offsetX = remember(key) { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var dragAccumulated by remember(key) { mutableStateOf(0f) }

        var scale by remember(key) { mutableStateOf(1f) }
        var panX by remember(key) { mutableStateOf(0f) }
        var panY by remember(key) { mutableStateOf(0f) }
        val isZoomed = scale > 1.01f

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
                .pointerInput(key, isZoomed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    isZoomed -> {
                                        dragAccumulated = 0f
                                    }
                                    dragAccumulated > swipeThresholdPx -> {
                                        offsetX.animateTo(
                                            targetValue = containerWidthPx * 1.4f,
                                            animationSpec = tween(280)
                                        )
                                        onKeep()
                                    }
                                    dragAccumulated < -swipeThresholdPx -> {
                                        offsetX.animateTo(
                                            targetValue = -containerWidthPx * 1.4f,
                                            animationSpec = tween(280)
                                        )
                                        onTrash()
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, animationSpec = tween(220))
                                        dragAccumulated = 0f
                                    }
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        if (!isZoomed) {
                            change.consume()
                            dragAccumulated += dragAmount
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount)
                            }
                        }
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(key) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 4f)
                            scale = newScale
                            if (newScale > 1f) {
                                panX += pan.x
                                panY += pan.y
                            } else {
                                panX = 0f
                                panY = 0f
                            }
                        }
                    }
            ) {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.displayName,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = panX
                            translationY = panY
                        }
                )

                if (offsetX.value > 30f) {
                    SwipeBadge(
                        text = "GUARDAR",
                        color = Color(0xFF2ECC71),
                        alignment = Alignment.TopStart,
                        alpha = (abs(offsetX.value) / swipeThresholdPx).coerceIn(0f, 1f)
                    )
                }
                if (offsetX.value < -30f) {
                    SwipeBadge(
                        text = "PAPELERA",
                        color = Color(0xFFE74C3C),
                        alignment = Alignment.TopEnd,
                        alpha = (abs(offsetX.value) / swipeThresholdPx).coerceIn(0f, 1f)
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
                        offsetX.animateTo(-containerWidthPx * 1.4f, animationSpec = tween(280))
                        onTrash()
                    }
                }
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Mandar a la papelera", tint = Color.White)
            }

            FilledIconButton(
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2ECC71)),
                onClick = {
                    scope.launch {
                        offsetX.animateTo(containerWidthPx * 1.4f, animationSpec = tween(280))
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
