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

package com.android.customization.model.grid

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.android.wallpaper.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultShapeGridManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) : ShapeGridManager {

    private val authorityMetadataKey: String =
        context.getString(R.string.grid_control_metadata_name)
    private val previewUtils: PreviewUtils =
        PreviewUtils(context, authorityMetadataKey, Screen.HOME_SCREEN)

    override suspend fun getGridOptions(): List<GridOptionModel> =
        withContext(bgDispatcher) {
            if (previewUtils.supportsPreview()) {
                context.contentResolver
                    .query(previewUtils.getUri(GRID_OPTIONS), null, null, null, null)
                    ?.use { cursor ->
                        buildList {
                                while (cursor.moveToNext()) {
                                    try {
                                        val rows = cursor.getInt(cursor.getColumnIndex(COL_ROWS))
                                        val cols = cursor.getInt(cursor.getColumnIndex(COL_COLS))
                                        val backUpTitle =
                                            context.getString(
                                                com.android.themepicker.R.string.grid_title_pattern,
                                                cols,
                                                rows,
                                            )
                                        val title =
                                            cursor.getColumnIndex(COL_GRID_TITLE).let { titleIndex
                                                ->
                                                if (titleIndex != -1) {
                                                    // Note that title can be null, even when the
                                                    // column field exists.
                                                    cursor.getString(titleIndex)
                                                } else {
                                                    null
                                                }
                                            } ?: backUpTitle

                                        add(
                                            GridOptionModel(
                                                key =
                                                    cursor.getString(
                                                        cursor.getColumnIndex(COL_GRID_KEY)
                                                    ),
                                                title = title,
                                                isCurrent =
                                                    cursor
                                                        .getString(
                                                            cursor.getColumnIndex(COL_IS_DEFAULT)
                                                        )
                                                        .toBoolean(),
                                                rows = rows,
                                                cols = cols,
                                                iconId =
                                                    cursor.getInt(
                                                        cursor.getColumnIndex(KEY_GRID_ICON_ID)
                                                    ),
                                            )
                                        )
                                    } catch (e: IllegalStateException) {
                                        Log.e(
                                            TAG,
                                            "Fail to read from the cursor to build GridOptionModel",
                                            e,
                                        )
                                    }
                                }
                            }
                            .let { list ->
                                if (list.isEmpty()) {
                                    throw IllegalStateException(
                                        "Grid option list can not be empty. It needs to have at least one item."
                                    )
                                }
                                // In this list, exactly one item should have isCurrent true.
                                val isCurrentCount = list.count { it.isCurrent }
                                if (isCurrentCount != 1) {
                                    throw IllegalStateException(
                                        "Exactly one grid option should have isCurrent = true. Found $isCurrentCount."
                                    )
                                }
                                list
                            }
                            .sortedByDescending { it.rows * it.cols }
                    } ?: emptyList()
            } else {
                emptyList()
            }
        }

    override suspend fun getShapeOptions(): List<ShapeOptionModel> =
        withContext(bgDispatcher) {
            if (previewUtils.supportsPreview()) {
                context.contentResolver
                    .query(previewUtils.getUri(SHAPE_OPTIONS), null, null, null, null)
                    ?.use { cursor ->
                        buildList {
                                while (cursor.moveToNext()) {
                                    add(
                                        ShapeOptionModel(
                                            key =
                                                cursor.getString(
                                                    cursor.getColumnIndex(COL_SHAPE_KEY)
                                                ),
                                            title =
                                                cursor.getString(
                                                    cursor.getColumnIndex(COL_SHAPE_TITLE)
                                                ),
                                            path =
                                                cursor.getString(cursor.getColumnIndex(COL_PATH)),
                                            isCurrent =
                                                cursor
                                                    .getString(
                                                        cursor.getColumnIndex(COL_IS_DEFAULT)
                                                    )
                                                    .toBoolean(),
                                        )
                                    )
                                }
                            }
                            .let { list ->
                                if (list.isEmpty()) {
                                    throw IllegalStateException(
                                        "Shape option list can not be empty. It needs to have at least one item."
                                    )
                                }
                                // In this list, exactly one item should have isCurrent true.
                                val isCurrentCount = list.count { it.isCurrent }
                                if (isCurrentCount != 1) {
                                    throw IllegalStateException(
                                        "Exactly one shape option should have isCurrent = true. Found $isCurrentCount."
                                    )
                                }
                                list
                            }
                    } ?: emptyList()
            } else {
                emptyList()
            }
        }

    override fun applyGridOption(gridKey: String) {
        context.contentResolver.update(
            previewUtils.getUri(SET_GRID),
            ContentValues().apply { put(COL_GRID_KEY, gridKey) },
            null,
            null,
        )
    }

    override fun applyShapeOption(shapeKey: String) =
        context.contentResolver.update(
            previewUtils.getUri(SET_SHAPE),
            ContentValues().apply { put(COL_SHAPE_KEY, shapeKey) },
            null,
            null,
        )

    override fun getGridOptionDrawable(iconId: Int): Drawable? {
        val launcherPackageName =
            context.getString(com.android.themepicker.R.string.launcher_overlayable_package)
        try {
            val drawable =
                ResourcesCompat.getDrawable(
                    context.packageManager.getResourcesForApplication(launcherPackageName),
                    iconId,
                    /* theme = */ null,
                )
            return drawable
        } catch (exception: Resources.NotFoundException) {
            Log.w(
                TAG,
                "Unable to find drawable resource from package $launcherPackageName with resource ID $iconId",
            )
            return null
        }
    }

    companion object {
        const val TAG = "DefaultShapeGridManager"
        const val SHAPE_OPTIONS: String = "shape_options"
        const val GRID_OPTIONS: String = "list_options"
        const val SET_GRID: String = "default_grid"
        const val SET_SHAPE: String = "shape"
        const val COL_SHAPE_KEY: String = "shape_key"
        const val COL_GRID_KEY: String = "name"
        const val COL_GRID_NAME: String = "grid_name"
        const val COL_GRID_TITLE: String = "grid_title"
        const val COL_SHAPE_TITLE: String = "shape_title"
        const val COL_ROWS: String = "rows"
        const val COL_COLS: String = "cols"
        const val COL_IS_DEFAULT: String = "is_default"
        const val COL_PATH: String = "path"
        const val KEY_GRID_ICON_ID: String = "grid_icon_id"
    }
}
