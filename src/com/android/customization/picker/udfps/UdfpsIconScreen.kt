/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.customization.picker.udfps

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.themepicker.R

@Composable
fun UdfpsIconScreen() {
    val context = LocalContext.current
    val iconPackage = "org.derpfest.udfps.icons"

    val icons = remember {
        loadIconList(context, iconPackage)
    }

    if (icons == null || icons.isEmpty()) {
        return
    }

    var selectedIcon by remember {
        mutableIntStateOf(getSelectedIcon(context))
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
            itemsIndexed(icons) { index, iconName ->
                val isSelected = selectedIcon == index
                UdfpsIconItem(
                    context = context,
                    iconPackage = iconPackage,
                    iconName = iconName,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedIcon != index) {
                            selectedIcon = index
                            updateIconStyle(context, index)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun UdfpsIconItem(
    context: Context,
    iconPackage: String,
    iconName: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconDrawable = remember(iconName, iconPackage) {
        getDrawable(context, iconPackage, iconName)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(100.dp)
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(dimensionResource(R.dimen.option_tile_radius))
                ) else Modifier
            )
            .padding(20.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        iconDrawable?.let { drawable ->
            val bitmap = remember(drawable) {
                drawable.toBitmap(60, 60)
            }
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = iconName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }
    }
}

private fun loadIconList(
    context: Context,
    iconPackage: String
): List<String>? {
    return try {
        val packageManager = context.packageManager
        val resources = packageManager.getResourcesForApplication(iconPackage)
        val icons = resources.getStringArray(
            resources.getIdentifier(
                "udfps_icons",
                "array",
                iconPackage
            )
        )
        icons.toList()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getDrawable(
    context: Context,
    iconPackage: String,
    drawableName: String
): Drawable? {
    return try {
        val pm = context.packageManager
        val resources: Resources = pm.getResourcesForApplication(iconPackage)
        val ctx = context.createPackageContext(
            iconPackage,
            Context.CONTEXT_IGNORE_SECURITY
        )
        val resId = resources.getIdentifier(drawableName, "drawable", iconPackage)
        if (resId != 0) {
            ctx.getDrawable(resId)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getSelectedIcon(context: Context): Int {
    return Settings.System.getInt(
        context.contentResolver,
        Settings.System.UDFPS_ICON,
        0
    )
}

private fun updateIconStyle(context: Context, iconIndex: Int) {
    Settings.System.putInt(
        context.contentResolver,
        Settings.System.UDFPS_ICON,
        iconIndex
    )
}
