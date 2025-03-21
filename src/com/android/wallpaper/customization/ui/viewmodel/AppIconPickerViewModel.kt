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
 */

package com.android.wallpaper.customization.ui.viewmodel

import com.android.customization.model.grid.ShapeOptionModel
import com.android.customization.picker.grid.domain.interactor.AppIconInteractor
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class AppIconPickerViewModel
@AssistedInject
constructor(interactor: AppIconInteractor, @Assisted private val viewModelScope: CoroutineScope) {
    //// Shape

    // The currently-set system shape option
    val selectedShapeKey =
        interactor.selectedShapeOption
            .filterNotNull()
            .map { it.key }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingShapeKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system shape option
    val previewingShapeKey =
        combine(overridingShapeKey, selectedShapeKey) { overridingShapeOptionKey, selectedShapeKey
            ->
            overridingShapeOptionKey ?: selectedShapeKey
        }

    val shapeOptions: Flow<List<OptionItemViewModel2<ShapeIconViewModel>>> =
        interactor.shapeOptions
            .filterNotNull()
            .map { shapeOptions -> shapeOptions.map { toShapeOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    //// Themed icons enabled
    val isThemedIconAvailable =
        interactor.isThemedIconAvailable.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )

    private val overridingIsThemedIconEnabled = MutableStateFlow<Boolean?>(null)
    val isThemedIconEnabled =
        interactor.isThemedIconEnabled.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )
    val previewingIsThemeIconEnabled =
        combine(overridingIsThemedIconEnabled, isThemedIconEnabled) {
            overridingIsThemeIconEnabled,
            isThemeIconEnabled ->
            overridingIsThemeIconEnabled ?: isThemeIconEnabled
        }
    val toggleThemedIcon: Flow<suspend () -> Unit> =
        previewingIsThemeIconEnabled.map {
            {
                val newValue = !it
                overridingIsThemedIconEnabled.value = newValue
            }
        }

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(
            overridingShapeKey,
            selectedShapeKey,
            overridingIsThemedIconEnabled,
            isThemedIconEnabled,
        ) {
            overridingShapeKey,
            selectedShapeKey,
            overridingIsThemedIconEnabled,
            currentIsThemedIconEnabled ->
            val shapeNeedsUpdate =
                overridingShapeKey != null && overridingShapeKey != selectedShapeKey
            val themedIconNeedsUpdate =
                overridingIsThemedIconEnabled != null &&
                    overridingIsThemedIconEnabled != currentIsThemedIconEnabled
            if (shapeNeedsUpdate || themedIconNeedsUpdate) {
                {
                    if (shapeNeedsUpdate) {
                        overridingShapeKey?.let { interactor.applyShape(it) }
                    }
                    if (themedIconNeedsUpdate) {
                        coroutineScope {
                            launch {
                                overridingIsThemedIconEnabled?.let {
                                    interactor.applyThemedIconEnabled(it)
                                }
                            }
                            isThemedIconEnabled.drop(1).take(1).collect {
                                return@collect
                            }
                        }
                    }
                }
            } else {
                null
            }
        }

    fun resetPreview() {
        overridingShapeKey.value = null
        overridingIsThemedIconEnabled.value = null
    }

    private fun toShapeOptionItemViewModel(
        option: ShapeOptionModel
    ): OptionItemViewModel2<ShapeIconViewModel> {
        val isSelected =
            previewingShapeKey
                .map { it == option.key }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )

        return OptionItemViewModel2(
            key = MutableStateFlow(option.key),
            payload = ShapeIconViewModel(option.key, option.path),
            text = Text.Loaded(option.title),
            isSelected = isSelected,
            onClicked =
                isSelected.map {
                    if (!it) {
                        { overridingShapeKey.value = option.key }
                    } else {
                        null
                    }
                },
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): AppIconPickerViewModel
    }
}
