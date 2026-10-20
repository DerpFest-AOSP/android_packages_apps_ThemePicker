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

package com.android.customization.picker.icon.ui.binder

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.DoubleRowListItemSpacing
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.ui.util.IconStyleViewUtil
import com.android.customization.picker.icon.ui.viewmodel.AppIconPickerViewModel.Tab
import com.android.customization.picker.icon.ui.viewmodel.ShapeIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.binder.FloatingSheetHeightAnimationBinder
import com.android.wallpaper.customization.ui.binder.SwitchColorBinder
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.category.ui.binder.SectionsBinder.removeItemDecorations
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.view.FloatingToolbar
import com.android.wallpaper.picker.customization.ui.view.adapter.FloatingToolbarTabAdapter
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter2
import com.google.android.material.materialswitch.MaterialSwitch
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

object AppIconFloatingSheetBinder {

    fun bind(
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
        iconStyleViewUtil: IconStyleViewUtil,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val viewModel = optionsViewModel.appIconPickerViewModel
        val isFloatingSheetActive = { optionsViewModel.selectedOption.value == APP_ICONS }

        val isExtendibleThemeManager = BaseFlags.get(view.context).isExtendibleThemeManager()
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
        val shapeContent = view.requireViewById<View>(R.id.app_shape_content)
        val labelContent = view.requireViewById<View>(R.id.app_icon_label_container)

        val shapeOptionListAdapter =
            createShapeOptionItemAdapter(
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val shapeOptionList =
            view.requireViewById<RecyclerView>(R.id.shape_options).also {
                it.initOptionList(view.context, shapeOptionListAdapter)
            }

        val styleOptionListAdapter =
            createStyleOptionItemAdapter(
                iconStyleViewUtil = iconStyleViewUtil,
                colorUpdateViewModel = colorUpdateViewModel,
                shouldAnimateColor = isFloatingSheetActive,
                lifecycleOwner = lifecycleOwner,
                backgroundDispatcher = backgroundDispatcher,
            )
        val styleOptionList =
            view.requireViewById<RecyclerView>(R.id.icon_style_options).also {
                it.initOptionList(
                    view.context,
                    styleOptionListAdapter,
                    GridLayoutManager(view.context, 1, RecyclerView.HORIZONTAL, false),
                )
            }

        val styleOption = styleContent.findViewById<View>(R.id.icon_style_option_placeholder)
        val styleIcon = styleOption.findViewById<View>(R.id.option_icon)
        val styleOptionsPerRow: MutableStateFlow<Int?> = MutableStateFlow(null)

        val themedIconsSwitch = view.requireViewById<MaterialSwitch>(R.id.themed_icon_toggle)
        val themedIconEntry = view.requireViewById<ViewGroup>(R.id.themed_icon_toggle_entry)
        val themedIconTitle = view.requireViewById<TextView>(R.id.themed_icon_toggle_title)
        val themedIconBetaLabel = view.requireViewById<TextView>(R.id.themed_icon_beta_title)
        val showAppLabelsTitle = view.requireViewById<TextView>(R.id.show_app_labels_title)
        val showAppLabelsSwitch = view.requireViewById<MaterialSwitch>(R.id.show_app_labels_switch)
        val showAppDrawerLabelsTitle =
            view.requireViewById<TextView>(R.id.show_app_drawer_labels_title)
        val showAppDrawerLabelsSwitch =
            view.requireViewById<MaterialSwitch>(R.id.show_app_drawer_labels_switch)
        ColorUpdateBinder.bind(
            setColor = { color -> showAppLabelsTitle.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color -> showAppDrawerLabelsTitle.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = isFloatingSheetActive,
            lifecycleOwner = lifecycleOwner,
        )
        val globalIconShapeSwitch =
            view.requireViewById<MaterialSwitch>(R.id.global_icon_shape_toggle)
        val globalIconShapeEntry =
            view.requireViewById<ViewGroup>(R.id.global_icon_shape_toggle_entry)
        val globalIconShapeTitle =
            view.requireViewById<TextView>(R.id.global_icon_shape_toggle_title)

        data class FloatingSheetHeightsViewModel(
            val styleContentHeight: Int? = null,
            val shapeContentHeight: Int? = null,
            val labelContentHeight: Int? = null,
        )
        val floatingSheetHeights: MutableStateFlow<FloatingSheetHeightsViewModel> =
            MutableStateFlow(FloatingSheetHeightsViewModel())

        if (isExtendibleThemeManager) {
            val listEdgeSpace =
                view.resources.getDimensionPixelSize(
                    R.dimen.floating_sheet_content_horizontal_padding
                )
            val listItemSpace =
                view.resources.getDimensionPixelSize(
                    R.dimen.floating_sheet_list_item_horizontal_space
                )
            styleContent.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (styleContent.width != 0 && styleOption.width != 0) {
                            styleOptionsPerRow.value =
                                (styleContent.width - 2 * listEdgeSpace + listItemSpace) /
                                    (styleOption.width + listItemSpace)
                            styleContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )

            shapeContent.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (
                            shapeContent.height != 0 &&
                                floatingSheetHeights.value.shapeContentHeight != shapeContent.height
                        ) {
                            floatingSheetHeights.value =
                                floatingSheetHeights.value.copy(
                                    shapeContentHeight = shapeContent.height
                                )
                            shapeContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )

            labelContent.viewTreeObserver.addOnGlobalLayoutListener(
                object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (
                            labelContent.height != 0 &&
                                floatingSheetHeights.value.labelContentHeight != labelContent.height
                        ) {
                            floatingSheetHeights.value =
                                floatingSheetHeights.value.copy(
                                    labelContentHeight = labelContent.height
                                )
                            labelContent.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                }
            )
        }

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
                    viewModel.isShapeOptionsAvailable.collect { isAvailable ->
                        globalIconShapeEntry.isVisible = isAvailable
                        globalIconShapeSwitch.isEnabled = isAvailable
                    }
                }

                launch {
                    var switchBinding: SwitchColorBinder.Binding? = null
                    var titleBinding: ColorUpdateBinder.Binding? = null
                    viewModel.previewingGlobalIconShapeEnabled.collect {
                        globalIconShapeSwitch.isChecked = it
                        titleBinding?.destroy()
                        titleBinding =
                            bindTitleColor(
                                globalIconShapeTitle,
                                colorUpdateViewModel,
                                isFloatingSheetActive,
                                lifecycleOwner,
                            )
                        switchBinding?.destroy()
                        switchBinding =
                            SwitchColorBinder.bind(
                                switch = globalIconShapeSwitch,
                                isChecked = it,
                                colorUpdateViewModel = colorUpdateViewModel,
                                shouldAnimateColor = isFloatingSheetActive,
                                lifecycleOwner = lifecycleOwner,
                            )
                    }
                }

                launch {
                    viewModel.toggleGlobalIconShape.collect { toggle ->
                        globalIconShapeSwitch.setOnCheckedChangeListener { _, _ ->
                            launch { toggle.invoke() }
                        }
                    }
                }

                if (isExtendibleThemeManager) {
                    themedIconEntry.isVisible = false

                    var styleOptionIndexToFocus = 0
                    launch {
                        viewModel.styleOptions.collect { options ->
                            styleOptionListAdapter.setItems(options) {
                                (styleOptionList.layoutManager as LinearLayoutManager)
                                    .scrollToPosition(styleOptionIndexToFocus)
                            }
                        }
                    }

                    launch {
                        viewModel.previewingIconStyleOptionIndex.collect { index ->
                            if (index < 0) return@collect
                            styleOptionIndexToFocus = index
                            styleOptionList.post {
                                val layoutManager =
                                    styleOptionList.layoutManager as? LinearLayoutManager
                                        ?: return@post
                                val itemView = layoutManager.findViewByPosition(index)

                                if (itemView != null) {
                                    val parentCenter = styleOptionList.width / 2
                                    val itemCenter = itemView.left + itemView.width / 2
                                    val scrollBy = itemCenter - parentCenter
                                    styleOptionList.smoothScrollBy(scrollBy, 0)
                                } else if (index < styleOptionListAdapter.itemCount) {
                                    layoutManager.scrollToPosition(index)
                                }
                            }
                        }
                    }

                    launch {
                        val itemVerticalSpace =
                            view.resources.getDimensionPixelSize(
                                R.dimen.floating_sheet_list_item_vertical_space
                            )
                        combine(viewModel.styleOptions, styleOptionsPerRow.filterNotNull(), ::Pair)
                            .collect { (options, optionsPerRow) ->
                                val rowCount = if (options.size > optionsPerRow) 2 else 1
                                styleOptionList.setStyleOptionRowCount(rowCount)
                                val styleContentHeight =
                                    if (rowCount == 1) {
                                        styleOption.height
                                    } else {
                                        // GridLayoutManager gives both rows the same height, so
                                        // both need room for the space DoubleRowListItemSpacing
                                        // adds below the first row.
                                        2 * (styleOption.height + itemVerticalSpace)
                                    }
                                // Keep the height of the style floating sheet fixed so text renders
                                // correctly after changing tabs.
                                if (styleContent.layoutParams.height != styleContentHeight) {
                                    styleContent.layoutParams =
                                        styleContent.layoutParams.apply {
                                            height = styleContentHeight
                                        }
                                }
                                floatingSheetHeights.value =
                                    floatingSheetHeights.value.copy(
                                        styleContentHeight = styleContentHeight
                                    )
                                if (rowCount == 1) {
                                    iconStyleViewUtil.bindListDivider(
                                        options,
                                        styleOptionList,
                                        styleIcon.height,
                                    )
                                }
                            }
                    }

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
                        val verticalPadding =
                            view.resources.getDimensionPixelSize(
                                R.dimen.floating_sheet_content_vertical_padding
                            )
                        var currentTab: Tab? = null
                        combine(floatingSheetHeights, viewModel.selectedTab, ::Pair).collect {
                            (heights, selectedTab) ->
                            val (styleContentHeight, shapeContentHeight, labelContentHeight) =
                                heights
                            styleContentHeight ?: return@collect
                            shapeContentHeight ?: return@collect
                            labelContentHeight ?: return@collect
                            selectedTab ?: return@collect

                            styleContent.isVisible = (currentTab == Tab.STYLE)
                            shapeContent.isVisible = (currentTab == Tab.SHAPE)

                            val fromHeight = floatingSheetContainer.height
                            val toHeight =
                                when (selectedTab) {
                                    Tab.STYLE -> styleContentHeight
                                    Tab.SHAPE -> shapeContentHeight
                                    Tab.NAMES -> labelContentHeight
                                } + 2 * verticalPadding
                            val currentContent: View? =
                                when (currentTab) {
                                    Tab.STYLE -> styleContent
                                    Tab.SHAPE -> shapeContent
                                    Tab.NAMES -> labelContent
                                    else -> null
                                }
                            val selectedContent: View =
                                when (selectedTab) {
                                    Tab.STYLE -> styleContent
                                    Tab.SHAPE -> shapeContent
                                    Tab.NAMES -> labelContent
                                }
                            val fromContent =
                                if (currentTab != selectedTab) currentContent else null
                            val toContent = if (currentTab != selectedTab) selectedContent else null
                            FloatingSheetHeightAnimationBinder.bind(
                                floatingSheetContainer,
                                fromHeight,
                                toHeight,
                                fromContent,
                                toContent,
                            )
                            currentTab = selectedTab
                        }
                    }

                    launch {
                        var switchBinding: SwitchColorBinder.Binding? = null
                        viewModel.previewingShouldShowAppLabels.collect {
                            showAppLabelsSwitch.isChecked = it
                            switchBinding?.destroy()
                            switchBinding =
                                SwitchColorBinder.bind(
                                    switch = showAppLabelsSwitch,
                                    isChecked = it,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isFloatingSheetActive,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }

                    launch {
                        viewModel.toggleShouldShowAppLabels.collect {
                            showAppLabelsSwitch.setOnCheckedChangeListener { _, _ -> it.invoke() }
                        }
                    }

                    launch {
                        var switchBinding: SwitchColorBinder.Binding? = null
                        viewModel.previewingShouldShowAppDrawerLabels.collect {
                            showAppDrawerLabelsSwitch.isChecked = it
                            switchBinding?.destroy()
                            switchBinding =
                                SwitchColorBinder.bind(
                                    switch = showAppDrawerLabelsSwitch,
                                    isChecked = it,
                                    colorUpdateViewModel = colorUpdateViewModel,
                                    shouldAnimateColor = isFloatingSheetActive,
                                    lifecycleOwner = lifecycleOwner,
                                )
                        }
                    }

                    launch {
                        viewModel.toggleShouldShowAppDrawerLabels.collect {
                            showAppDrawerLabelsSwitch.setOnCheckedChangeListener { _, _ ->
                                it.invoke()
                            }
                        }
                    }
                } else {
                    styleContent.isVisible = false

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
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): ColorUpdateBinder.Binding =
        ColorUpdateBinder.bind(
            setColor = { color -> title.setTextColor(color) },
            color = colorUpdateViewModel.colorOnSurface,
            shouldAnimate = shouldAnimateColor,
            lifecycleOwner = lifecycleOwner,
        )

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

    private fun createStyleOptionItemAdapter(
        iconStyleViewUtil: IconStyleViewUtil,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter2<IconStyleModel> {
        return OptionItemAdapter2(
            layoutResourceId = R.layout.icon_style_option2,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            bindPayload = { view: View, iconStyleModel: IconStyleModel ->
                return@OptionItemAdapter2 iconStyleViewUtil.bindIconOptionView(
                    view,
                    iconStyleModel,
                    colorUpdateViewModel,
                    shouldAnimateColor,
                    lifecycleOwner,
                )
            },
            colorUpdateViewModel = WeakReference(colorUpdateViewModel),
            shouldAnimateColor = shouldAnimateColor,
        )
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

    private fun RecyclerView.initOptionList(
        context: Context,
        adapter: OptionItemAdapter2<*>,
        layoutManager: LinearLayoutManager =
            LinearLayoutManager(context, RecyclerView.HORIZONTAL, false),
    ) {
        apply {
            this.layoutManager = layoutManager
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

    private fun RecyclerView.setStyleOptionRowCount(rowCount: Int) {
        val gridLayoutManager = layoutManager as GridLayoutManager
        if (gridLayoutManager.spanCount == rowCount) return
        gridLayoutManager.spanCount = rowCount
        removeItemDecorations()
        val edgeItemSpacePx =
            resources.getDimensionPixelSize(R.dimen.floating_sheet_content_horizontal_padding)
        val itemHorizontalSpacePx =
            resources.getDimensionPixelSize(R.dimen.floating_sheet_list_item_horizontal_space)
        addItemDecoration(
            if (rowCount == 1) {
                SingleRowListItemSpacing(
                    edgeItemSpacePx = edgeItemSpacePx,
                    itemHorizontalSpacePx = itemHorizontalSpacePx,
                )
            } else {
                // DoubleRowListItemSpacing adds the space on both sides of a column, so halve it
                // to keep the same gap between columns as between items in a single row.
                DoubleRowListItemSpacing(
                    edgeItemSpacePx = edgeItemSpacePx,
                    itemHorizontalSpacePx = itemHorizontalSpacePx / 2,
                    itemVerticalSpacePx =
                        resources.getDimensionPixelSize(
                            R.dimen.floating_sheet_list_item_vertical_space
                        ),
                )
            }
        )
    }
}
