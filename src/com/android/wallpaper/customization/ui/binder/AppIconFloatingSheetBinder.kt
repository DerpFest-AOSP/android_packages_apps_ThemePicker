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

package com.android.wallpaper.customization.ui.binder

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.viewmodel.AppIconPickerViewModel
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import com.google.android.material.materialswitch.MaterialSwitch
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

object AppIconFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val viewModel = optionsViewModel.appIconPickerViewModel
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == APP_ICONS }

        val isExtendibleThemeManager = BaseFlags.get().isExtendibleThemeManager()
        val tabs = view.requireViewById<FloatingToolbar>(R.id.floating_toolbar)
        val tabAdapter: FloatingToolbarTabAdapter?
        if (isExtendibleThemeManager) {
            tabs.isVisible = true
            val tabContainer =
                tabs.findViewById<ViewGroup>(
                    com.android.wallpaper.R.id.floating_toolbar_tab_container
                )
            ColorUpdateBinder.bind(
                setColor = { color ->
                    DrawableCompat.setTint(DrawableCompat.wrap(tabContainer.background), color)
                },
                color = colorUpdateViewModel.floatingToolbarBackground,
                shouldAnimate = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
            )
            tabAdapter =
                FloatingToolbarTabAdapter(
                        colorUpdateViewModel = WeakReference(colorUpdateViewModel),
                        shouldAnimateColor = isFloatingSheetActive,
                    )
                    .also { tabs.setAdapter(it) }
        } else {
            tabAdapter = null
        }

        val floatingSheetContainer =
            view.requireViewById<ViewGroup>(R.id.floating_sheet_content_container)
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(
                    DrawableCompat.wrap(floatingSheetContainer.background),
                    color,
                )
            },
            color = colorUpdateViewModel.colorSurfaceBright,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )

        val styleContent = view.requireViewById<View>(R.id.app_icon_style_container)
        val shapeContent = view.requireViewById<View>(R.id.app_shape_container)

        val shapeOptionListAdapter =
            createShapeOptionItemAdapter(
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val shapeOptionList =
            view.requireViewById<RecyclerView>(R.id.shape_options).also {
                it.initShapeOptionList(view.context, shapeOptionListAdapter)
            }

        val themedIconsSwitch = view.requireViewById<MaterialSwitch>(R.id.themed_icon_toggle)
        val themedIconEntry = view.requireViewById<ViewGroup>(R.id.themed_icon_toggle_entry)
        val themedIconTitle = view.requireViewById<TextView>(R.id.themed_icon_toggle_title)
        val themedIconBetaLabel = view.requireViewById<TextView>(R.id.themed_icon_beta_title)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.shapeOptions.collect { options ->
                        shapeOptionListAdapter.setItems(options) {
                            val indexToFocus =
                                options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (shapeOptionList.layoutManager as LinearLayoutManager).scrollToPosition(
                                indexToFocus
                            )
                        }
                    }
                }

                if (isExtendibleThemeManager) {
                    themedIconEntry.isVisible = false

                    launch {
                        viewModel.tabs.collect {
                            if (it.size > 1) {
                                tabAdapter?.submitList(it)
                            } else {
                                tabs.isVisible = false
                            }
                        }
                    }

                    launch {
                        viewModel.selectedTab.collect {
                            // TODO (b/397782741): add animation when switching tabs
                            styleContent.isVisible = (it == AppIconPickerViewModel.Tab.STYLE)
                            shapeContent.isVisible = (it == AppIconPickerViewModel.Tab.SHAPE)
                        }
                    }
                } else {
                    launch {
                        viewModel.isShapeOptionsAvailable.collect { shapeAvailable ->
                            shapeContent.isVisible = shapeAvailable
                        }
                    }

                    launch {
                        viewModel.isThemedIconAvailable.collect { isAvailable ->
                            themedIconEntry.isVisible = isAvailable
                            themedIconsSwitch.isEnabled = isAvailable
                        }
                    }

                    launch {
                        var switchBinding: SwitchColorBinder.Binding? = null
                        var titleBinding: ColorUpdateBinder.Binding? = null
                        viewModel.previewingIsThemeIconEnabled.collect {
                            themedIconsSwitch.isChecked = it
                            titleBinding?.destroy()
                            titleBinding =
                                bindTitleColor(
                                    themedIconTitle,
                                    themedIconBetaLabel,
                                    colorUpdateViewModel,
                                    isFloatingSheetActive,
                                    lifecycleOwner,
                                )
                            switchBinding?.destroy()
                            switchBinding =
                                SwitchColorBinder.bind(
                                    switch = themedIconsSwitch,
                                    isChecked = it,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isFloatingSheetActive,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }

                    launch {
                        viewModel.toggleThemedIcon.collect {
                            themedIconsSwitch.setOnCheckedChangeListener { _, _ ->
                                launch { it.invoke() }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindTitleColor(
        title: TextView,
        betaLabel: TextView,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): ColorUpdateBinder.Binding {
        val titleBinding =
            ColorUpdateBinder.bind(
                setColor = { color -> title.setTextColor(color) },
                color = colorUpdateViewModel.colorOnSurface,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val labelBinding =
            ColorUpdateBinder.bind(
                setColor = { color -> betaLabel.setTextColor(color) },
                color = colorUpdateViewModel.colorOnPrimaryContainer,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        val labelBackgroundBinding =
            ColorUpdateBinder.bind(
                setColor = { color ->
                    betaLabel.background.setTintList(ColorStateList.valueOf(color))
                },
                color = colorUpdateViewModel.colorPrimaryContainer,
                shouldAnimate = shouldAnimateColor,
                lifecycleOwner = lifecycleOwner,
            )
        return object : ColorUpdateBinder.Binding {
            override fun destroy() {
                titleBinding.destroy()
                labelBinding.destroy()
                labelBackgroundBinding.destroy()
            }
        }
    }

    private fun createShapeOptionItemAdapter(
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter2<ShapeIconViewModel> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.shape_option2,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            bindPayload = { view: View, shapeIcon: ShapeIconViewModel ->
                val imageView = view.findViewById(R.id.foreground) as? ImageView
                imageView?.let { ShapeIconViewBinder.bind(imageView, shapeIcon) }
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    private fun RecyclerView.initShapeOptionList(
        context: Context,
        adapter: OptionItemAdapter2<ShapeIconViewModel>,
    ) {
        apply {
            this.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            addItemDecoration(
                SingleRowListItemSpacing(
                    edgeItemSpacePx =
                        context.resources.getDimensionPixelSize(
                            R.dimen.floating_sheet_content_horizontal_padding
                        ),
                    itemHorizontalSpacePx =
                        context.resources.getDimensionPixelSize(
                            R.dimen.floating_sheet_list_item_horizontal_space
                        ),
                )
            )
            this.adapter = adapter
        }
    }
}
