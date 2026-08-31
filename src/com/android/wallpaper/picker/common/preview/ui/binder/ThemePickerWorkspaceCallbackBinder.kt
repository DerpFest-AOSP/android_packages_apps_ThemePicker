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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.content.Context
import android.os.Bundle
import android.os.Message
import android.os.RemoteException
import android.util.Log
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.COL_GRID_NAME
import com.android.customization.model.grid.DefaultShapeGridManager.Companion.COL_SHAPE_KEY
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.data.util.MaterialColorsGenerator
import com.android.customization.picker.icon.shared.model.IconPackStyle
import com.android.customization.picker.icon.shared.model.ThemePickerIconStyle
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_HIDE_SMART_SPACE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_INITIALLY_SELECTED_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_QUICK_AFFORDANCE_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_DEFAULT_PREVIEW
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_HIDE_SMART_SPACE
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_SLOT_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder.Companion.sendMessage
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@Singleton
class ThemePickerWorkspaceCallbackBinder
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val defaultWorkspaceCallbackBinder: DefaultWorkspaceCallbackBinder,
    private val materialColorsGenerator: MaterialColorsGenerator,
    private val baseFlags: BaseFlags,
) : WorkspaceCallbackBinder {

    private var lockScreenJob: Job? = null
    private var homeScreenJob: Job? = null

    @OptIn(FlowPreview::class)
    override fun bind(
        workspaceCallback: Message,
        viewModel: CustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        screen: Screen,
        clockViewFactory: ClockViewFactory,
        lifecycleOwner: LifecycleOwner,
    ) {
        defaultWorkspaceCallbackBinder.bind(
            workspaceCallback = workspaceCallback,
            viewModel = viewModel,
            colorUpdateViewModel = colorUpdateViewModel,
            screen = screen,
            clockViewFactory = clockViewFactory,
            lifecycleOwner = lifecycleOwner,
        )

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        when (screen) {
            Screen.LOCK_SCREEN -> {
                lockScreenJob?.cancel()
                lockScreenJob =
                    lifecycleOwner.lifecycleScope.launch {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                            launch {
                                viewModel.selectedOption.collect {
                                    when (it) {
                                        ThemePickerLockCustomizationOption.SHORTCUTS ->
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES,
                                                Bundle().apply {
                                                    putString(
                                                        KEY_INITIALLY_SELECTED_SLOT_ID,
                                                        SLOT_ID_BOTTOM_START,
                                                    )
                                                },
                                            )
                                        else ->
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_DEFAULT_PREVIEW,
                                                Bundle.EMPTY,
                                            )
                                    }
                                }
                            }

                            launch {
                                viewModel.keyguardQuickAffordancePickerViewModel2.selectedSlotId
                                    .collect {
                                        safeSendMessage(
                                            workspaceCallback,
                                            MESSAGE_ID_SLOT_SELECTED,
                                            Bundle().apply { putString(KEY_SLOT_ID, it) },
                                        )
                                    }
                            }

                            launch {
                                viewModel.keyguardQuickAffordancePickerViewModel2
                                    .previewingQuickAffordances
                                    .collect {
                                        it[SLOT_ID_BOTTOM_START]?.let {
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                                Bundle().apply {
                                                    putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_START)
                                                    putString(KEY_QUICK_AFFORDANCE_ID, it)
                                                },
                                            )
                                        }
                                        it[SLOT_ID_BOTTOM_END]?.let {
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                                Bundle().apply {
                                                    putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_END)
                                                    putString(KEY_QUICK_AFFORDANCE_ID, it)
                                                },
                                            )
                                        }
                                    }
                            }

                            launch {
                                viewModel.clockPickerViewModel.showKeyguardPreviewRendererSmartspace
                                    .collect {
                                        safeSendMessage(
                                            workspaceCallback,
                                            MESSAGE_ID_HIDE_SMART_SPACE,
                                            Bundle().apply { putBoolean(KEY_HIDE_SMART_SPACE, !it) },
                                        )
                                    }
                            }
                        }
                    }
            }
            Screen.HOME_SCREEN -> {
                homeScreenJob?.cancel()
                homeScreenJob =
                    lifecycleOwner.lifecycleScope.launch {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                            launch {
                                viewModel.appIconPickerViewModel.previewingShapeKey.collect {
                                    safeSendMessage(
                                        workspaceCallback,
                                        MESSAGE_ID_UPDATE_SHAPE,
                                        bundleOf(COL_SHAPE_KEY to it),
                                    )
                                }
                            }

                            launch {
                                viewModel.gridPickerViewModel.previewingGridKey.collect {
                                    safeSendMessage(
                                        workspaceCallback,
                                        MESSAGE_ID_UPDATE_GRID,
                                        bundleOf(COL_GRID_NAME to it),
                                    )
                                }
                            }

                            if (BaseFlags.get(context).isColorPickerUpdateEnabled()) {
                                launch {
                                    combine(
                                            viewModel.colorPickerViewModel2.selectedColorOption,
                                            // Sample to reduce workspace updates during freeform
                                            // slider updates
                                            viewModel.colorPickerViewModel2
                                                .tempOverridingColorOption
                                                .debounce(200),
                                            viewModel.colorPickerViewModel2.overridingStyle,
                                            viewModel.darkModeViewModel.overridingIsDarkMode,
                                            ::Quadruple,
                                        )
                                        .collect {
                                            (
                                                selectedColor,
                                                overridingColor,
                                                overridingStyle,
                                                overridingDarkMode) ->
                                            val bundle =
                                                Bundle().apply {
                                                    val colorNeedsUpdate = overridingColor != null
                                                    val styleNeedsUpdate = overridingStyle != null
                                                    val colorOption =
                                                        overridingColor ?: selectedColor
                                                    if (
                                                        (colorNeedsUpdate || styleNeedsUpdate) &&
                                                            colorOption != null
                                                    ) {
                                                        if (baseFlags.isThemeServiceEnabled()) {
                                                            val style =
                                                                overridingStyle ?: colorOption.style
                                                            // TODO (b/488057749): enable multi-seed
                                                            putIntArray(
                                                                KEY_SEED_COLOR_LIST,
                                                                intArrayOf(colorOption.seedColor),
                                                            )
                                                            putInt(KEY_THEME_STYLE, style)
                                                        } else {
                                                            val style =
                                                                overridingStyle ?: colorOption.style
                                                            val (ids, colors) =
                                                                materialColorsGenerator.generate(
                                                                    colorOption.seedColor,
                                                                    style,
                                                                    overridingDarkMode,
                                                                )
                                                            putIntArray(KEY_COLOR_RESOURCE_IDS, ids)
                                                            putIntArray(KEY_COLOR_VALUES, colors)
                                                        }
                                                    }

                                                    if (overridingDarkMode != null) {
                                                        putBoolean(
                                                            KEY_DARK_MODE,
                                                            overridingDarkMode,
                                                        )
                                                    }
                                                }
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_UPDATE_COLOR,
                                                bundle,
                                            )
                                        }
                                }
                            } else {
                                launch {
                                    combine(
                                            viewModel.colorPickerViewModel2.overridingColorOption,
                                            viewModel.darkModeViewModel.overridingIsDarkMode,
                                            ::Pair,
                                        )
                                        .collect { (colorOption, darkMode) ->
                                            val bundle =
                                                Bundle().apply {
                                                    if (colorOption != null) {
                                                        if (baseFlags.isThemeServiceEnabled()) {
                                                            // TODO (b/488057749): enable multi-seed
                                                            putIntArray(
                                                                KEY_SEED_COLOR_LIST,
                                                                intArrayOf(colorOption.seedColor),
                                                            )
                                                            putInt(
                                                                KEY_THEME_STYLE,
                                                                colorOption.style,
                                                            )
                                                        } else {
                                                            val (ids, colors) =
                                                                materialColorsGenerator.generate(
                                                                    colorOption.seedColor,
                                                                    colorOption.style,
                                                                    darkMode,
                                                                )
                                                            putIntArray(KEY_COLOR_RESOURCE_IDS, ids)
                                                            putIntArray(KEY_COLOR_VALUES, colors)
                                                        }
                                                    }

                                                    if (darkMode != null) {
                                                        putBoolean(KEY_DARK_MODE, darkMode)
                                                    }
                                                }
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_UPDATE_COLOR,
                                                bundle,
                                            )
                                        }
                                }
                            }

                            if (BaseFlags.get(context).isExtendibleThemeManager()) {
                                launch {
                                    var hasEmittedPackOverride = false
                                    viewModel.appIconPickerViewModel.iconStyleOverride.collect {
                                        override ->
                                        if (override != null) {
                                            hasEmittedPackOverride = true
                                            val packValue =
                                                (override as? IconPackStyle)?.packageName
                                                    ?: SYSTEM_ICONS_SENTINEL
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_UPDATE_COMMAND,
                                                Bundle().apply {
                                                    putString(
                                                        KEY_UPDATE_METHOD,
                                                        METHOD_SET_ICON_PACK,
                                                    )
                                                    putString(KEY_ICON_PACK_VALUE, packValue)
                                                },
                                            )
                                        } else if (hasEmittedPackOverride) {
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_UPDATE_COMMAND,
                                                Bundle().apply {
                                                    putString(
                                                        KEY_UPDATE_METHOD,
                                                        METHOD_SET_ICON_PACK,
                                                    )
                                                    putString(KEY_ICON_PACK_VALUE, "")
                                                },
                                            )
                                        }
                                    }
                                }
                                launch {
                                    viewModel.appIconPickerViewModel.previewingIconStyle.collect {
                                        safeSendMessage(
                                            workspaceCallback,
                                            MESSAGE_ID_UPDATE_ICON_THEMED,
                                            Bundle().apply {
                                                putBoolean(
                                                    KEY_BOOLEAN_VALUE,
                                                    it == ThemePickerIconStyle.MONOCHROME,
                                                )
                                            },
                                        )
                                    }
                                }
                            } else {
                                launch {
                                    viewModel.appIconPickerViewModel.previewingIsThemeIconEnabled
                                        .collect {
                                            safeSendMessage(
                                                workspaceCallback,
                                                MESSAGE_ID_UPDATE_ICON_THEMED,
                                                Bundle().apply { putBoolean(KEY_BOOLEAN_VALUE, it) },
                                            )
                                        }
                                }
                            }

                            launch {
                                viewModel.appIconPickerViewModel.previewingShouldShowAppLabels
                                    .collect {
                                        safeSendMessage(
                                            workspaceCallback,
                                            MESSAGE_ID_UPDATE_COMMAND,
                                            Bundle().apply {
                                                putString(
                                                    KEY_UPDATE_METHOD,
                                                    METHOD_SET_WORKSPACE_ITEMS_LABEL_HIDDEN,
                                                )
                                                putBoolean(KEY_BOOLEAN_VALUE, !it)
                                            },
                                        )
                                    }
                            }
                        }
                    }
            }
        }
    }

    override fun unbind() {
        lockScreenJob?.cancel()
        lockScreenJob = null
        homeScreenJob?.cancel()
        homeScreenJob = null
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        const val TAG = "ThemePickerWorkspaceCallbackBinder"

        @Deprecated("Use [MESSAGE_ID_UPDATE_COMMAND] instead")
        const val MESSAGE_ID_UPDATE_SHAPE = 2586
        @Deprecated("Use [MESSAGE_ID_UPDATE_COMMAND] instead")
        const val MESSAGE_ID_UPDATE_GRID = 7414
        @Deprecated("Use [MESSAGE_ID_UPDATE_COMMAND] instead")
        const val MESSAGE_ID_UPDATE_COLOR = 856
        @Deprecated("Use [MESSAGE_ID_UPDATE_COMMAND] instead")
        const val MESSAGE_ID_UPDATE_ICON_THEMED = 311
        const val KEY_COLOR_RESOURCE_IDS: String = "color_resource_ids"
        const val KEY_COLOR_VALUES: String = "color_values"
        const val KEY_DARK_MODE: String = "use_dark_mode"
        const val KEY_BOOLEAN_VALUE: String = "boolean_value"
        const val KEY_SEED_COLOR_LIST: String = "seed_color_list"
        const val KEY_THEME_STYLE: String = "theme_style"

        const val MESSAGE_ID_UPDATE_COMMAND = 512
        const val KEY_UPDATE_METHOD = "update_method"
        private const val METHOD_SET_WORKSPACE_ITEMS_LABEL_HIDDEN =
            "/set_workspace_items_label_hidden"
        private const val METHOD_SET_ICON_PACK = "/icon_pack"
        private const val KEY_ICON_PACK_VALUE = "icon_pack_value"
        private const val SYSTEM_ICONS_SENTINEL = "system_icons"

        fun safeSendMessage(workspaceCallback: Message, what: Int, data: Bundle) {
            try {
                workspaceCallback.sendMessage(what, data)
            } catch (e: RemoteException) {
                Log.w(TAG, "Failed to send message to workspace callback", e)
            }
        }
    }
}
