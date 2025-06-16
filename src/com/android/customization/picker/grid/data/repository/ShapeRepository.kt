/*
 * Copyright (C) 2025 The Android Open Source Project
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
 *
 */

package com.android.customization.picker.grid.data.repository

import android.content.Context
import android.content.res.Resources
import com.android.customization.model.ResourceConstants
import com.android.customization.model.grid.ShapeGridManager
import com.android.customization.model.grid.ShapeOptionModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class ShapeRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val shapeGridManager: ShapeGridManager,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {

    val defaultShapePath =
        context.resources.getString(
            Resources.getSystem()
                .getIdentifier(
                    ResourceConstants.CONFIG_ICON_MASK,
                    "string",
                    ResourceConstants.ANDROID_PACKAGE,
                )
        )

    val shapeOptions: Flow<List<ShapeOptionModel>> = shapeGridManager.shapeOptions

    val isShapeOptionsAvailable =
        combine(shapeGridManager.isCustomizationAvailable, shapeOptions) {
            isCustomizationAvailable,
            _ ->
            // Call getShapeOptions() instead of using shapeOptions flow to avoid getting stale
            // replay value
            isCustomizationAvailable && shapeGridManager.getShapeOptions().size > 1
        }

    val selectedShapeOption: Flow<ShapeOptionModel?> =
        shapeOptions.map { shapeOptions -> shapeOptions.firstOrNull { it.isCurrent } }

    suspend fun applyShape(shapeKey: String) =
        withContext(bgDispatcher) { shapeGridManager.applyShapeOption(shapeKey) }
}
