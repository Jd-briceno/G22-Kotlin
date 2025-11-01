package com.g22.orbitsoundkotlin.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.g22.orbitsoundkotlin.data.cache.ImageCacheManager

/**
 * Image component with optimized multi-layer caching using Coil.
 * 
 * Automatically loads from memory → disk → network cache → network in that order.
 * Includes crossfade animation, loading state, and error handling.
 * 
 * @param imageUrl Image URL to load
 * @param contentDescription Accessibility description
 * @param modifier Modifier for styling
 * @param contentScale How to scale the image
 * @param placeholder Optional custom loading placeholder
 * @param error Optional custom error placeholder
 */
@Composable
fun CachedImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    val context = LocalContext.current
    val imageLoader = ImageCacheManager.getImageLoader(context)
    
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .crossfade(300)
            .build(),
        contentDescription = contentDescription,
        imageLoader = imageLoader,
        modifier = modifier,
        contentScale = contentScale,
        loading = {
            placeholder?.invoke() ?: Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        },
        error = { error?.invoke() }
    )
}

/**
 * Specialized cached image component for album covers.
 * Uses ContentScale.Crop for optimal display without distortion.
 */
@Composable
fun CachedAlbumCover(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    CachedImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = error
    )
}

/**
 * Specialized cached image component for user avatars.
 * Note: Apply Modifier.clip(CircleShape) for circular avatars.
 */
@Composable
fun CachedAvatar(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null
) {
    CachedImage(
        imageUrl = imageUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = error
    )
}

