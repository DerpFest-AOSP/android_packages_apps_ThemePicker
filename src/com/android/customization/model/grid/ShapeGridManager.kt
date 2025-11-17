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

import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.Flow

interface ShapeGridManager {
    /**
     * Get a list of grid options.
     *
     * @return It will return an empty list if there are no available grid options.
     */
    suspend fun getGridOptions(): List<GridOptionModel>

    /**
     * A flow representing whether shape and grid customization option provider is available.
     * Collecting from this flow also triggers a retry to get customization provider if it is not
     * available.
     */
    val isCustomizationAvailable: Flow<Boolean>

    /**
     * A flow of the current list of grid options, updated when the grid options change.
     *
     * @return It will return an empty list if there are no available grid options.
     */
    val gridOptions: Flow<List<GridOptionModel>>

    /**
     * A flow of the current list of shape options, updated when the shape options change.
     *
     * @return It will return an empty list if there are no available shape options.
     */
    val shapeOptions: Flow<List<ShapeOptionModel>>

    suspend fun getShapeOptions(): List<ShapeOptionModel>

    fun applyGridOption(gridKey: String)

    fun applyShapeOption(shapeKey: String)

    fun getGridOptionDrawable(iconId: Int): Drawable?
}
