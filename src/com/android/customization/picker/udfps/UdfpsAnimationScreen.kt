/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.customization.picker.udfps

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.android.themepicker.R
import com.android.wallpaper.R as WallpaperR

@Composable
fun UdfpsAnimationScreen() {
    val context = LocalContext.current
    val animationPackage = "org.derpfest.overlay.customization.udfps.animations"

    val animationData = remember {
        loadAnimationData(context, animationPackage)
    }

    if (animationData == null) {
        return
    }

    val (animations, animationPreviews, animationTitles) = animationData
    var selectedAnim by remember {
        mutableIntStateOf(getSelectedAnimation(context, animations.size))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 7.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(animations) { index, _ ->
                val isSelected = selectedAnim == index
                UdfpsAnimationItem(
                    context = context,
                    animationPackage = animationPackage,
                    previewDrawableName = animationPreviews[index],
                    animationDrawableName = animations[index],
                    title = animationTitles[index],
                    isSelected = isSelected,
                    onClick = {
                        if (selectedAnim != index) {
                            selectedAnim = index
                            updateAnimationStyle(context, index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UdfpsAnimationItem(
    context: Context,
    animationPackage: String,
    previewDrawableName: String,
    animationDrawableName: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val previewDrawable = remember(previewDrawableName, animationPackage) {
        getDrawable(context, animationPackage, previewDrawableName)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .then(
                    if (isSelected) Modifier.border(
                        3.dp,
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(dimensionResource(R.dimen.option_tile_radius))
                    ) else Modifier
                )
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            previewDrawable?.let { drawable ->
                val bitmap = remember(drawable) {
                    drawable.toBitmap(80, 80)
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(
                ContextCompat.getColor(context, WallpaperR.color.system_on_surface)
                    .toLong() and 0xFFFFFFFFL
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

private fun loadAnimationData(
    context: Context,
    animationPackage: String
): Triple<List<String>, List<String>, List<String>>? {
    return try {
        val packageManager = context.packageManager
        val resources = packageManager.getResourcesForApplication(animationPackage)
        val animations = resources.getStringArray(
            resources.getIdentifier(
                "udfps_animation_styles",
                "array",
                animationPackage
            )
        ).toList()
        val animationPreviews = resources.getStringArray(
            resources.getIdentifier(
                "udfps_animation_previews",
                "array",
                animationPackage
            )
        ).toList()
        val animationTitles = resources.getStringArray(
            resources.getIdentifier(
                "udfps_animation_titles",
                "array",
                animationPackage
            )
        ).toList()
        Triple(animations, animationPreviews, animationTitles)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getDrawable(
    context: Context,
    animationPackage: String,
    drawableName: String
): Drawable? {
    return try {
        val pm = context.packageManager
        val resources: Resources = pm.getResourcesForApplication(animationPackage)
        resources.getDrawable(
            resources.getIdentifier(
                drawableName,
                "drawable",
                animationPackage
            ),
            null
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getSelectedAnimation(context: Context, animationsSize: Int): Int {
    val udfpsStyle = Settings.System.getInt(
        context.contentResolver,
        Settings.System.UDFPS_ANIM_STYLE,
        0
    )
    // return 0 for unsupported values as like we did in SystemUI.
    return if (udfpsStyle < 0 || udfpsStyle >= animationsSize) 0 else udfpsStyle
}

private fun updateAnimationStyle(context: Context, animIndex: Int) {
    Settings.System.putInt(
        context.contentResolver,
        Settings.System.UDFPS_ANIM_STYLE,
        animIndex
    )
}
