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

import android.content.Context
import com.android.customization.model.grid.ShapeOptionModel
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.customization.picker.icon.domain.interactor.AppIconInteractor
import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

class AppIconPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext private val applicationContext: Context,
    interactor: AppIconInteractor,
    private val logger: ThemesUserEventLogger,
    @Assisted private val viewModelScope: CoroutineScope,
) {
    //// Shape

    // The currently-set system shape option
    val selectedShape =
        interactor.selectedShapeOption
            .filterNotNull()
            .map { toShapeOptionItemViewModel(it) }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)
    private val overridingShapeKey = MutableStateFlow<String?>(null)
    // If the overriding key is null, use the currently-set system shape option
    val previewingShapeKey =
        combine(overridingShapeKey, selectedShape) { overridingShapeOptionKey, selectedShape ->
            overridingShapeOptionKey ?: selectedShape.key.value
        }

    val shapeOptions: Flow<List<OptionItemViewModel2<ShapeIconViewModel>>> =
        interactor.shapeOptions
            .filterNotNull()
            .map { shapeOptions -> shapeOptions.map { toShapeOptionItemViewModel(it) } }
            .shareIn(scope = viewModelScope, started = SharingStarted.Lazily, replay = 1)

    val isShapeOptionsAvailable: Flow<Boolean> =
        interactor.isShapeOptionsAvailable.shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1,
        )

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

    //// Style
    private val selectedIconStyle = interactor.selectedIconStyle
    private val overridingIconStyle: MutableStateFlow<IconStyle?> = MutableStateFlow(null)
    val previewingIconStyle =
        combine(selectedIconStyle, overridingIconStyle) { selected, overriding ->
            overriding ?: selected
        }
    private val iconStyles = interactor.iconStyles
    val styleOptions: Flow<List<OptionItemViewModel2<IconStyle>>> =
        iconStyles.map {
            List(size = it.size, init = { index -> toStyleOptionItemViewModel(it[index]) })
        }
    val isIconStyleAvailable = iconStyles.map { it.size > 1 }

    enum class Tab {
        STYLE,
        SHAPE,
    }

    private val _selectedTab = MutableStateFlow<Tab?>(null)
    val selectedTab =
        combine(isThemedIconAvailable, isShapeOptionsAvailable, _selectedTab) {
            isThemedIconAvailable,
            isShapeOptionsAvailable,
            selectedTab ->
            selectedTab
                ?: if (isThemedIconAvailable) {
                    Tab.STYLE
                } else if (isShapeOptionsAvailable) {
                    Tab.SHAPE
                } else {
                    null
                }
        }

    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        combine(isIconStyleAvailable, isShapeOptionsAvailable, selectedTab) {
            isIconStyleAvailable,
            isShapeOptionsAvailable,
            selectedTab ->
            buildList {
                if (isIconStyleAvailable) {
                    val isSelected = (selectedTab == Tab.STYLE)
                    add(
                        FloatingToolbarTabViewModel(
                            icon =
                                Icon.Resource(
                                    res = R.drawable.ic_style,
                                    contentDescription = Text.Resource(R.string.app_icons_style),
                                ),
                            text =
                                Text.Resource(R.string.app_icons_style)
                                    .asString(applicationContext),
                            isSelected = isSelected,
                            onClick =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedTab.value = Tab.STYLE }
                                },
                        )
                    )
                }
                if (isShapeOptionsAvailable) {
                    val isSelected = (selectedTab == Tab.SHAPE)
                    add(
                        FloatingToolbarTabViewModel(
                            icon =
                                Icon.Resource(
                                    res = R.drawable.ic_shapes,
                                    contentDescription = Text.Resource(R.string.app_icons_shape),
                                ),
                            text =
                                Text.Resource(R.string.app_icons_shape)
                                    .asString(applicationContext),
                            isSelected = isSelected,
                            onClick =
                                if (isSelected) {
                                    null
                                } else {
                                    { _selectedTab.value = Tab.SHAPE }
                                },
                        )
                    )
                }
            }
        }

    val summary: Flow<AppIconPickerSummaryViewModel> =
        combine(selectedShape, isThemedIconEnabled, isShapeOptionsAvailable) {
            selectedShape,
            isThemedIconEnabled,
            isShapeOptionsAvailable ->
            val selectedShapeString =
                if (isShapeOptionsAvailable) selectedShape.text.asString(applicationContext) else ""
            val appIconThemeString =
                if (isThemedIconEnabled) {
                    applicationContext.getString(R.string.app_icons_theme_themed)
                } else {
                    applicationContext.getString(R.string.app_icons_theme_default)
                }
            AppIconPickerSummaryViewModel(
                description =
                    Text.Loaded(
                        if (selectedShapeString.isEmpty()) {
                            appIconThemeString.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault())
                                else it.toString()
                            }
                        } else {
                            applicationContext.getString(
                                R.string.app_icons_description,
                                selectedShapeString,
                                appIconThemeString,
                            )
                        }
                    ),
                iconShape = selectedShape.payload,
                isThemed = isThemedIconEnabled,
            )
        }

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(
            overridingShapeKey,
            selectedShape,
            overridingIsThemedIconEnabled,
            isThemedIconEnabled,
        ) {
            overridingShapeKey,
            selectedShape,
            overridingIsThemedIconEnabled,
            currentIsThemedIconEnabled ->
            val shapeNeedsUpdate =
                overridingShapeKey != null && overridingShapeKey != selectedShape.key.value
            val themedIconNeedsUpdate =
                overridingIsThemedIconEnabled != null &&
                    overridingIsThemedIconEnabled != currentIsThemedIconEnabled
            if (shapeNeedsUpdate || themedIconNeedsUpdate) {
                {
                    if (shapeNeedsUpdate) {
                        overridingShapeKey?.let {
                            interactor.applyShape(it)
                            logger.logShapeApplied(it)
                        }
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
                            overridingIsThemedIconEnabled?.let { logger.logThemedIconApplied(it) }
                        }
                    }
                }
            } else {
                null
            }
        }

    val onApply2: Flow<(suspend () -> Unit)?> =
        combine(overridingShapeKey, selectedShape, overridingIconStyle, selectedIconStyle) {
            overridingShapeKey,
            selectedShape,
            overridingIconStyle,
            currentIconStyle ->
            val shapeNeedsUpdate =
                overridingShapeKey != null && overridingShapeKey != selectedShape.key.value
            val styleNeedsUpdate =
                overridingIconStyle != null && overridingIconStyle != currentIconStyle
            if (shapeNeedsUpdate || styleNeedsUpdate) {
                {
                    if (shapeNeedsUpdate) {
                        overridingShapeKey?.let {
                            interactor.applyShape(it)
                            logger.logShapeApplied(it)
                        }
                    }
                    if (styleNeedsUpdate) {
                        coroutineScope {
                            launch {
                                overridingIconStyle?.let {
                                    interactor.applyThemedIconEnabled(it.getIsThemedIcon())
                                }
                            }
                            selectedIconStyle.drop(1).take(1).collect {
                                return@collect
                            }
                            overridingIconStyle?.let {
                                logger.logThemedIconApplied(it.getIsThemedIcon())
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

    fun resetPreview2() {
        overridingShapeKey.value = null
        overridingIconStyle.value = null
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

    private fun toStyleOptionItemViewModel(iconStyle: IconStyle): OptionItemViewModel2<IconStyle> {
        val isSelected =
            previewingIconStyle
                .map { it == iconStyle }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Lazily,
                    initialValue = false,
                )
        val text = Text.Resource(iconStyle.nameResId)
        return OptionItemViewModel2(
            key = MutableStateFlow(text.asString(applicationContext)),
            payload = iconStyle,
            text = text,
            isSelected = isSelected,
            onClicked =
                if (iconStyle.getIsExternalLink()) {
                    // A button is not selectable.
                    flowOf(null)
                } else {
                    isSelected.map {
                        if (!it) {
                            { overridingIconStyle.value = iconStyle }
                        } else {
                            null
                        }
                    }
                },
            skipOnClickBinding = iconStyle.getIsExternalLink(),
        )
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): AppIconPickerViewModel
    }
}
