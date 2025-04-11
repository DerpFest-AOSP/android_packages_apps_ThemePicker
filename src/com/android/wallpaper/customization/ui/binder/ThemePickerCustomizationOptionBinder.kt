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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView
import com.android.customization.picker.clock.ui.view.ClockConstraintLayoutHostView.Companion.addClockViews
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder2
import com.android.customization.picker.color.ui.view.ColorOptionIconView2
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.settings.ui.binder.ColorContrastSectionViewBinder2
import com.android.systemui.plugins.clocks.ClockAxisStyle
import com.android.systemui.plugins.clocks.ClockPreviewConfig
import com.android.systemui.shared.Flags
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.compose.ShortcutsFloatingSheet
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsData
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    override fun bind(
        customizationOptionsData: CustomizationOptionsData,
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToMoreLockScreenSettingsActivity: () -> Unit,
        navigateToColorContrastSettingsActivity: () -> Unit,
        navigateToLockScreenNotificationsSettingsActivity: () -> Unit,
        navigateToPackThemeActivity: (Intent) -> Unit,
    ) {
        defaultCustomizationOptionsBinder.bind(
            customizationOptionsData,
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
            navigateToMoreLockScreenSettingsActivity,
            navigateToColorContrastSettingsActivity,
            navigateToLockScreenNotificationsSettingsActivity,
            navigateToPackThemeActivity,
        )

        customizationOptionsData as ThemePickerCustomizationOptionsData

        val isComposeRefactorEnabled = BaseFlags.get().isComposeRefactorEnabled()

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel

        val isOnMainScreen = { optionsViewModel.selectedOption.value == null }

        val allCustomizationOptionEntries =
            lockScreenCustomizationOptionEntries + homeScreenCustomizationOptionEntries
        allCustomizationOptionEntries.forEach { (_, view) ->
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(view.background), color)
                },
                color = colorUpdateViewModel.colorSurfaceBright,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view
                        .findViewById<ViewGroup>(R.id.option_entry_icon_container)
                        ?.background
                        ?.let { DrawableCompat.setTint(DrawableCompat.wrap(it), color) }
                },
                color = colorUpdateViewModel.colorSurfaceContainerHigh,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_title)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    view.findViewById<TextView>(R.id.option_entry_description)?.setTextColor(color)
                },
                color = colorUpdateViewModel.colorOnSurfaceVariant,
                shouldAnimate = isOnMainScreen,
                lifecycleOwner = lifecycleOwner,
            )
        }

        val optionClock: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.CLOCK }
                .second
        val optionClockIcon: ImageView = optionClock.requireViewById(R.id.option_entry_icon)

        val isKeyguardQuickAffordanceEnabled =
            BaseFlags.get().isKeyguardQuickAffordanceEnabled(view.context)
        var optionShortcut: View? = null
        var optionShortcutDescription: TextView? = null
        var optionShortcutIcon1: ImageView? = null
        var optionShortcutIcon2: ImageView? = null
        if (isKeyguardQuickAffordanceEnabled) {
            optionShortcut =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerLockCustomizationOption.SHORTCUTS }
                    .second
            optionShortcutDescription =
                optionShortcut.requireViewById(R.id.option_entry_description)
            optionShortcutIcon1 = optionShortcut.requireViewById(R.id.option_entry_icon_1)
            optionShortcutIcon2 = optionShortcut.requireViewById(R.id.option_entry_icon_2)
        }

        val optionLockScreenNotificationsSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.LOCK_SCREEN_NOTIFICATIONS }
                .second
        optionLockScreenNotificationsSettings.setOnClickListener {
            navigateToLockScreenNotificationsSettingsActivity.invoke()
        }

        val optionMoreLockScreenSettings: View =
            lockScreenCustomizationOptionEntries
                .first { it.first == ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS }
                .second
        optionMoreLockScreenSettings.setOnClickListener {
            navigateToMoreLockScreenSettingsActivity.invoke()
        }

        var optionPackThemeIconHome: ImageView? = null
        var optionPackThemeIconLock: ImageView? = null
        var optionPackThemeHome: View? = null
        var optionPackThemeLock: View? = null
        if (BaseFlags.get().isPackThemeEnabled()) {
            optionPackThemeHome =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconHome = optionPackThemeHome.requireViewById(R.id.option_entry_icon)

            optionPackThemeLock =
                lockScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.PACK_THEME }
                    .second
            optionPackThemeIconLock = optionPackThemeLock.requireViewById(R.id.option_entry_icon)
        }

        val optionColors: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLORS }
                .second
        val optionColorsIcon: ColorOptionIconView2 =
            optionColors.requireViewById(R.id.option_entry_icon)

        val optionAppIcons: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.APP_ICONS }
                .second
        val optionAppIconsDescription: TextView =
            optionAppIcons.requireViewById(R.id.option_entry_description)
        val optionAppIconsIcon: ImageView = optionAppIcons.requireViewById(R.id.option_entry_icon)

        var optionGrid: View? = null
        var optionGridDescription: TextView? = null
        var optionGridIcon: ImageView? = null
        if (customizationOptionsData.isGridCustomizationAvailable) {
            optionGrid =
                homeScreenCustomizationOptionEntries
                    .first { it.first == ThemePickerHomeCustomizationOption.GRID }
                    .second
            optionGridDescription = optionGrid.requireViewById(R.id.option_entry_description)
            optionGridIcon = optionGrid.requireViewById(R.id.option_entry_icon)
        }

        val optionColorContrast: View =
            homeScreenCustomizationOptionEntries
                .first { it.first == ThemePickerHomeCustomizationOption.COLOR_CONTRAST }
                .second
        optionColorContrast.setOnClickListener { navigateToColorContrastSettingsActivity.invoke() }

        ColorUpdateBinder.bind(
            setColor = { color ->
                optionClockIcon.setColorFilter(color)
                if (isKeyguardQuickAffordanceEnabled) {
                    optionShortcutIcon1?.setColorFilter(color)
                    optionShortcutIcon2?.setColorFilter(color)
                }
                if (customizationOptionsData.isGridCustomizationAvailable) {
                    optionGridIcon?.setColorFilter(color)
                }
                if (BaseFlags.get().isPackThemeEnabled()) {
                    optionPackThemeIconHome?.setColorFilter(color)
                    optionPackThemeIconLock?.setColorFilter(color)
                }
            },
            color = colorUpdateViewModel.colorOnSurfaceVariant,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.onCustomizeClockClicked.collect {
                        optionClock.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.clockPickerViewModel.selectedClock.collect {
                        optionClockIcon.setImageDrawable(it.thumbnail)
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.onCustomizeShortcutClicked.collect {
                            optionShortcut?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }
                }

                if (isKeyguardQuickAffordanceEnabled) {
                    launch {
                        optionsViewModel.keyguardQuickAffordancePickerViewModel2.summary.collect {
                            summary ->
                            optionShortcutDescription?.let {
                                TextViewBinder.bind(view = it, viewModel = summary.description)
                            }
                            summary.icon1?.let { icon ->
                                optionShortcutIcon1?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon1?.isVisible = summary.icon1 != null

                            summary.icon2?.let { icon ->
                                optionShortcutIcon2?.let {
                                    IconViewBinder.bind(view = it, viewModel = icon)
                                }
                            }
                            optionShortcutIcon2?.isVisible = summary.icon2 != null
                        }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeColorsClicked.collect {
                        optionColors.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeIconsClicked.collect {
                        optionAppIcons.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    val appIconPickerViewModel = optionsViewModel.appIconPickerViewModel
                    combine(
                            appIconPickerViewModel.selectedShape,
                            appIconPickerViewModel.isThemedIconEnabled,
                            ::Pair,
                        )
                        .collect { (selectedShape, isThemedIconEnabled) ->
                            // TODO(b/402161932): create and display app icon preview
                            optionAppIcons
                                .requireViewById<View>(R.id.option_entry_icon_container)
                                .visibility = View.INVISIBLE
                            // TODO(b/402161932): show selected shape text when b/406486710 is fixed
                            // TODO(b/402161932): show selected theme text after content is decided
                            optionAppIconsDescription.visibility = View.GONE
                        }
                }

                if (customizationOptionsData.isGridCustomizationAvailable) {
                    launch {
                        optionsViewModel.onCustomizeShapeGridClicked.collect {
                            optionGrid?.setOnClickListener { _ -> it?.invoke() }
                        }
                    }

                    launch {
                        optionsViewModel.gridPickerViewModel.selectedGridOption.collect { gridOption
                            ->
                            optionGridDescription?.let { TextViewBinder.bind(it, gridOption.text) }
                            gridOption.payload?.let { optionGridIcon?.setImageDrawable(it) }
                        }
                    }
                }

                launch {
                    var binding: ColorContrastSectionViewBinder2.Binding? = null
                    optionsViewModel.colorContrastSectionViewModel.contrast.collectLatest { contrast
                        ->
                        binding?.destroy()
                        binding =
                            ColorContrastSectionViewBinder2.bind(
                                view = optionColorContrast,
                                contrast = contrast,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isOnMainScreen,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    var binding: ColorOptionIconBinder2.Binding? = null
                    optionsViewModel.colorPickerViewModel2.selectedColorOption.collect { colorOption
                        ->
                        (colorOption as? ColorOptionImpl)?.let {
                            binding?.destroy()
                            binding =
                                ColorOptionIconBinder2.bind(
                                    view = optionColorsIcon,
                                    viewModel =
                                        ColorOptionIconViewModel.fromColorOption(colorOption),
                                    darkTheme = view.resources.configuration.isNightModeActive,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isOnMainScreen,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }
                }

                if (BaseFlags.get().isPackThemeEnabled()) {
                    launch {
                        optionsViewModel.packThemeViewModel.packThemeData.collect { packThemeData ->
                            val homeTitle =
                                optionPackThemeHome?.findViewById<TextView>(R.id.option_entry_title)
                            val lockTitle =
                                optionPackThemeLock?.findViewById<TextView>(R.id.option_entry_title)
                            val homeDescription =
                                optionPackThemeHome?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            val lockDescription =
                                optionPackThemeHome?.findViewById<TextView>(
                                    R.id.option_entry_description
                                )
                            if (packThemeData.currentThemePackInfo.title.isNotEmpty()) {
                                homeTitle?.text = packThemeData.currentThemePackInfo.title
                                lockTitle?.text = packThemeData.currentThemePackInfo.title
                            }
                            if (packThemeData.currentThemePackInfo.description.isNotEmpty()) {
                                homeDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                                lockDescription?.text =
                                    packThemeData.currentThemePackInfo.description
                            }
                        }
                    }
                    launch {
                        optionsViewModel.packThemeViewModel.startThemePackActivityIntent.collect {
                            intent ->
                            if (intent != null) {
                                optionPackThemeHome?.setOnClickListener {
                                    navigateToPackThemeActivity.invoke(intent)
                                }
                                optionPackThemeLock?.setOnClickListener {
                                    navigateToPackThemeActivity.invoke(intent)
                                }
                            } else {
                                optionPackThemeHome?.setOnClickListener(null)
                                optionPackThemeLock?.setOnClickListener(null)
                            }
                        }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }
        if (isComposeRefactorEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    // TODO(b/409112907) Evaluate Compose performance before enabling flag
                    (it as ComposeView).setContent {
                        ShortcutsFloatingSheet(
                            optionsViewModel.keyguardQuickAffordancePickerViewModel2
                        )
                    }
                }
        } else {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
                ?.let {
                    ShortcutFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        if (!isComposeRefactorEnabled) {
            customizationOptionFloatingSheetViewMap
                ?.get(ThemePickerHomeCustomizationOption.COLORS)
                ?.let {
                    ColorsFloatingSheetBinder.bind(
                        it,
                        optionsViewModel,
                        colorUpdateViewModel,
                        lifecycleOwner,
                    )
                }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.APP_ICONS)
            ?.let {
                AppIconFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                    Dispatchers.IO,
                )
            }

        customizationOptionFloatingSheetViewMap?.get(ThemePickerHomeCustomizationOption.GRID)?.let {
            GridFloatingSheetBinder.bind(
                it,
                optionsViewModel,
                colorUpdateViewModel,
                lifecycleOwner,
                Dispatchers.IO,
            )
        }
    }

    override fun bindClockPreview(
        context: Context,
        clockHostView: View,
        clockFaceClickDelegateView: View,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        clockHostView as ClockConstraintLayoutHostView
        val clockPickerViewModel =
            (viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel)
                .clockPickerViewModel

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockSize,
                        ) { clock, size ->
                            clock to size
                        }
                        .collect { (clock, size) ->
                            clockHostView.removeAllViews()
                            // For new customization picker, we should get views from clocklayout
                            if (Flags.newCustomizationPickerUi()) {
                                clockViewFactory.getController(clock.clockId)?.let { clockController
                                    ->
                                    val udfpsTop =
                                        clockPickerViewModel.getUdfpsLocation()?.let {
                                            it.centerY - it.radius
                                        }
                                    val previewConfig =
                                        ClockPreviewConfig(
                                            context = context,
                                            isShadeLayoutWide =
                                                clockPickerViewModel.getIsShadeLayoutWide(),
                                            isSceneContainerFlagEnabled = false,
                                            udfpsTop = udfpsTop,
                                        )
                                    addClockViews(clockController, clockHostView, size)
                                    val cs = ConstraintSet()
                                    clockController.largeClock.layout.applyPreviewConstraints(
                                        previewConfig,
                                        cs,
                                    )
                                    clockController.smallClock.layout.applyPreviewConstraints(
                                        previewConfig,
                                        cs,
                                    )
                                    cs.applyTo(clockHostView)
                                }
                            } else {
                                val clockView =
                                    when (size) {
                                        ClockSize.DYNAMIC ->
                                            clockViewFactory.getLargeView(clock.clockId)
                                        ClockSize.SMALL ->
                                            clockViewFactory.getSmallView(clock.clockId)
                                    }
                                // The clock view might still be attached to an existing parent.
                                // Detach
                                // before adding to another parent.
                                (clockView.parent as? ViewGroup)?.removeView(clockView)
                                clockHostView.addView(clockView)
                            }
                        }
                }

                launch {
                    combine(
                            clockPickerViewModel.previewingSeedColor,
                            clockPickerViewModel.previewingClock,
                            clockPickerViewModel.previewingClockPresetIndexedStyle,
                            colorUpdateViewModel.systemColorsUpdated,
                            ::Quadruple,
                        )
                        .collect { quadruple ->
                            val (color, clock, clockPresetIndexedStyle, _) = quadruple
                            clockViewFactory.updateColor(clock.clockId, color)
                            clockViewFactory.updateFontAxes(
                                clock.clockId,
                                clockPresetIndexedStyle?.style ?: ClockAxisStyle(),
                            )
                        }
                }

                launch {
                    combine(
                            viewModel.customizationOptionsViewModel.selectedOption,
                            clockPickerViewModel.onClockFaceClicked,
                            ::Pair,
                        )
                        .collect { (selectedOption, onClockFaceClicked) ->
                            clockFaceClickDelegateView.isVisible =
                                selectedOption == ThemePickerLockCustomizationOption.CLOCK
                            clockFaceClickDelegateView.setOnClickListener {
                                onClockFaceClicked.invoke()
                            }
                        }
                }
            }
        }
    }

    override fun bindDiscardChangesDialog(
        customizationOptionsViewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
        activity: Activity,
    ) {
        defaultCustomizationOptionsBinder.bindDiscardChangesDialog(
            customizationOptionsViewModel,
            lifecycleOwner,
            activity,
        )
    }

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
