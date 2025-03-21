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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.grid.ui.viewmodel.ShapeIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
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

                launch {
                    viewModel.isThemedIconAvailable.collect { isAvailable ->
                        themedIconsSwitch.isEnabled = isAvailable
                    }
                }

                launch {
                    var binding: SwitchColorBinder.Binding? = null
                    viewModel.previewingIsThemeIconEnabled.collect {
                        themedIconsSwitch.isChecked = it
                        binding?.destroy()
                        binding =
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
