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

package com.android.wallpaper.customization.ui.view

import android.annotation.ColorInt
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.PathParser
import androidx.core.graphics.withSave
import com.android.customization.model.ResourceConstants
import com.android.wallpaper.R

/**
 * Drawable that draws a shape tile with a given path. If given an icon, it will also draw the icon
 * within the shape path.
 *
 * @param path Path of the shape assuming drawing on a 100x100 canvas.
 * @param icon The adaptive icon to draw within the path, or null if no icon should be drawn.
 * @param isThemed Whether the adaptive icon should be drawn in monochrome.
 */
class ShapeTileDrawable(
    context: Context,
    path: String? = null,
    private val icon: Drawable? = null,
    private val isThemed: Boolean = false,
) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path =
        PathParser.createPathFromPathData(
            path
                ?:
                // Get default shape path
                context.resources.getString(
                    Resources.getSystem()
                        .getIdentifier(
                            ResourceConstants.CONFIG_ICON_MASK,
                            "string",
                            ResourceConstants.ANDROID_PACKAGE,
                        )
                )
        )
    // The path scaled with regard to the update of drawable bounds
    private val scaledPath = Path(this.path)
    private val scaleMatrix = Matrix()
    private var backgroundColor = context.getColor(R.color.themed_icon_background_color)
    private var foregroundColor = context.getColor(R.color.themed_icon_color)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        scaleMatrix.setScale(bounds.width() / PATH_SIZE, bounds.height() / PATH_SIZE)
        path.transform(scaleMatrix, scaledPath)
        icon?.bounds = bounds
    }

    override fun draw(canvas: Canvas) {
        canvas.clipPath(scaledPath)
        canvas.drawPath(scaledPath, paint)
        canvas.withSave {
            if (icon as? AdaptiveIconDrawable != null) {
                if (isThemed) {
                    icon.monochrome?.let {
                        canvas.drawColor(backgroundColor)
                        it.setTint(foregroundColor)
                        it.draw(this)
                    }
                    // TODO (b/402161932): explore whether to handle case of icon w/o monochrome
                } else {
                    icon.background?.draw(this)
                    icon.foreground?.draw(this)
                }
            } else {
                icon?.draw(this)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    fun setThemedIconBackgroundColor(@ColorInt backgroundColor: Int) {
        this.backgroundColor = backgroundColor
        invalidateSelf()
    }

    fun setThemedIconForegroundColor(@ColorInt foregroundColor: Int) {
        this.foregroundColor = foregroundColor
        invalidateSelf()
    }

    @Deprecated(
        "getOpacity() is deprecated",
        ReplaceWith("setAlpha(int)", "android.graphics.drawable.Drawable"),
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    companion object {
        const val PATH_SIZE = 100f
    }
}
