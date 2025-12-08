/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.wallpaper.customization.ui.binder

import android.content.Context
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.wallpaper.customization.ui.view.ShapeTileDrawable
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import kotlinx.coroutines.DisposableHandle

object ShapeIconViewBinder {
    const val TAG = "ShapeIconViewBinder"

    fun bind(view: ImageView, shapeIcon: ShapeIconViewModel) {
        view.setImageDrawable(ShapeTileDrawable(view.context, shapeIcon.path))
    }

    fun bindIconStyleAndShapePreviewIcon(
        view: ImageView,
        icon: Icon?,
        shapeIcon: ShapeIconViewModel? = null,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        val iconDrawable = icon?.getDrawable(view.context)
        val shapeIconDrawable =
            if (iconDrawable is ShapeTileDrawable) {
                ShapeTileDrawable(
                    context = view.context,
                    path = shapeIcon?.path,
                    icon = iconDrawable.icon?.constantState?.newDrawable(),
                    isThemed = iconDrawable.isThemed,
                )
            } else if (shapeIcon?.path != null) {
                ShapeTileDrawable(
                    context = view.context,
                    path = shapeIcon.path,
                    icon = iconDrawable,
                    isThemed = false,
                )
            } else iconDrawable
        view.setImageDrawable(shapeIconDrawable)
        val disposableHandle =
            if (shapeIconDrawable is ShapeTileDrawable && shapeIconDrawable.isThemed) {
                bindPreviewIconColor(
                    shapeTileDrawable = shapeIconDrawable,
                    colorUpdateViewModel = colorUpdateViewModel,
                    shouldAnimateColor = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            } else null
        return disposableHandle
    }

    fun bindShapeAndThemedIconPreviewIcon(
        view: ImageView,
        appIconDrawable: AdaptiveIconDrawable?,
        shapeIcon: ShapeIconViewModel? = null,
        isThemed: Boolean,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle {
        val shapeTileDrawable =
            ShapeTileDrawable(
                context = view.context,
                path = shapeIcon?.path,
                icon = appIconDrawable,
                isThemed = isThemed,
            )
        view.setImageDrawable(shapeTileDrawable)
        val bindingForeground =
            if (isThemed) {
                ColorUpdateBinder.bind(
                    setColor = { color -> shapeTileDrawable.setThemedIconForegroundColor(color) },
                    color = colorUpdateViewModel.themedIconColor,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            } else null
        val bindingBackground =
            if (isThemed) {
                ColorUpdateBinder.bind(
                    setColor = { color -> shapeTileDrawable.setThemedIconBackgroundColor(color) },
                    color = colorUpdateViewModel.themedIconBackgroundColor,
                    shouldAnimate = shouldAnimateColor,
                    lifecycleOwner = lifecycleOwner,
                )
            } else null
        return DisposableHandle {
            bindingForeground?.destroy()
            bindingBackground?.destroy()
        }
    }

    fun bindPreviewIconColor(
        shapeTileDrawable: ShapeTileDrawable,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle {
        val bindingForeground =
            ColorUpdateBinder.bind(
                setColor = { color -> shapeTileDrawable.setThemedIconForegroundColor(color) },
                color = colorUpdateViewModel.themedIconColor,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val bindingBackground =
            ColorUpdateBinder.bind(
                setColor = { color -> shapeTileDrawable.setThemedIconBackgroundColor(color) },
                color = colorUpdateViewModel.themedIconBackgroundColor,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        return DisposableHandle {
            bindingForeground.destroy()
            bindingBackground.destroy()
        }
    }

    fun bindButtonIconColor(
        foreground: ImageView,
        background: ImageView,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle {
        val bindingForeground =
            ColorUpdateBinder.bind(
                setColor = { color -> foreground.setColorFilter(color) },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val bindingBackground =
            ColorUpdateBinder.bind(
                setColor = { color -> background.setColorFilter(color) },
                color = colorUpdateViewModel.colorSurfaceContainer,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        return DisposableHandle {
            bindingForeground.destroy()
            bindingBackground.destroy()
        }
    }

    fun loadAppIcon(context: Context, packageName: String): Drawable? {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: NameNotFoundException) {
            Log.d(TAG, "Couldn't find resource $packageName for app icon preview")
            null
        }
    }
}
