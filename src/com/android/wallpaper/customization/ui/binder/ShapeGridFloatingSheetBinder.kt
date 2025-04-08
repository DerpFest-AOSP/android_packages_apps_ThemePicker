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

import android.content.Context
import android.graphics.drawable.Drawable
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
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.GRID
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

// TODO (b/402161932): rename shape grid classes and clean up view model and below
object ShapeGridFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val viewModel = optionsViewModel.fridPickerViewModel
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == GRID }

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

        val gridOptionListAdapter =
            createGridOptionItemAdapter(
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val gridOptionList =
            view.requireViewById<RecyclerView>(R.id.grid_options).also {
                it.initGridOptionList(view.context, gridOptionListAdapter)
            }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.gridOptions.collect { options ->
                        gridOptionListAdapter.setItems(options) {
                            val indexToFocus =
                                options.indexOfFirst { it.isSelected.value }.coerceAtLeast(0)
                            (gridOptionList.layoutManager as LinearLayoutManager).scrollToPosition(
                                indexToFocus
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createGridOptionItemAdapter(
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter2<Drawable> =
        OptionItemAdapter2(
            layoutResourceId = R.layout.grid_option2,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            bindPayload = { view: View, gridIcon: Drawable ->
                val imageView = view.findViewById(R.id.foreground) as? ImageView
                imageView?.setImageDrawable(gridIcon)
                return@OptionItemAdapter2 null
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )

    private fun RecyclerView.initGridOptionList(
        context: Context,
        adapter: OptionItemAdapter2<Drawable>,
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
                            R.dimen.floating_sheet_grid_list_item_horizontal_space
                        ),
                )
            )
            this.adapter = adapter
        }
    }
}
