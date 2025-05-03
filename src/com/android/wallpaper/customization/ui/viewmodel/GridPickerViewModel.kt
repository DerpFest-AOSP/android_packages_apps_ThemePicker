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

package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.android.customization.model.ResourceConstants
import com.android.customization.model.grid.GridOptionModel
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.grid.domain.interactor.GridInteractor2
import com.android.customization.widget.GridTileDrawable
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class GridPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val context: Context,
    private val interactor: GridInteractor2,
    private val logger: ThemesUserEventLogger,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    val isGridCustomizationAvailable = interactor.isGridCustomizationAvailable

    // The currently-set system grid option
    val selectedGridOption =
        interactor.selectedGridOption
            .filterNotNull()
            .map { toGridOptionItemViewModel(it) }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingGridKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system grid option
    val previewingGridKey =
        combine(overridingGridKey, selectedGridOption) { overridingGridOptionKey, selectedGridOption
                ->
                overridingGridOptionKey ?: selectedGridOption.key.value
            }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val gridOptions: Flow<List<GridOptionModel>> =
        interactor.gridOptions
            .filterNotNull()
            .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), 1)
    val gridOptionListItems: Flow<List<OptionItemViewModel2<Drawable>>> =
        gridOptions
            .map { gridOptions -> gridOptions.map { toGridOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(overridingGridKey, selectedGridOption, gridOptions) {
            overridingGridKey,
            selectedGridOption,
            gridOptions ->
            if (overridingGridKey != null && overridingGridKey != selectedGridOption.key.value) {
                {
                    interactor.applyGridOption(overridingGridKey)
                    gridOptions
                        .find { it.key == overridingGridKey }
                        ?.let { logger.logGridApplied(it) }
                }
            } else {
                null
            }
        }

    fun resetPreview() {
        overridingGridKey.value = null
    }

    private fun toGridOptionItemViewModel(option: GridOptionModel): OptionItemViewModel2<Drawable> {
        // Fallback to use GridTileDrawable when no resource found for the icon ID
        val drawable =
            interactor.getGridOptionDrawable(option.iconId)
                ?: GridTileDrawable(
                    option.cols,
                    option.rows,
                    context.resources.getString(
                        Resources.getSystem()
                            .getIdentifier(
                                ResourceConstants.CONFIG_ICON_MASK,
                                "string",
                                ResourceConstants.ANDROID_PACKAGE,
                            )
                    ),
                )
        val isSelected =
            previewingGridKey
                .map { it == option.key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )
        return OptionItemViewModel2(
            key = MutableStateFlow(option.key),
            payload = drawable,
            text = Text.Loaded(option.title),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingGridKey.value = option.key }
                    } else {
                        null
                    }
                },
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): GridPickerViewModel
    }
}
