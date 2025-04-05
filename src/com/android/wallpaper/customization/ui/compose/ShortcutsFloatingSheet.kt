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

package com.android.wallpaper.customization.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon as ComposeIcon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.compose.theme.PlatformTheme
import com.android.compose.ui.graphics.painter.rememberDrawablePainter
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.KeyguardQuickAffordancePickerViewModel2
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2

@Composable
fun ShortcutsFloatingSheet(
    viewModel: KeyguardQuickAffordancePickerViewModel2,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.quickAffordances.collectAsStateWithLifecycle(emptyList())
    val colorScheme = MaterialTheme.colorScheme
    val tabs by viewModel.tabs.collectAsStateWithLifecycle(emptyList())

    PlatformTheme {
        Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal =
                                dimensionResource(R.dimen.floating_sheet_horizontal_padding)
                        )
                        .clip(shape = RoundedCornerShape(28.dp))
                        .background(colorScheme.inverseOnSurface)
                        .size(225.dp),
            ) {
                items(items) { item -> ShortcutItem(slotIcon = item) }
            }
            ShortcutTabRow(tabs)
        }
    }
}

@Composable
private fun ShortcutTabRow(tabs: List<FloatingToolbarTabViewModel>, modifier: Modifier = Modifier) {
    if (tabs.size < 2) {
        return
    }
    val leftShortcut = tabs[0]
    val rightShortcut = tabs[1]
    var selectedTab by remember { mutableStateOf(leftShortcut) }
    Row(
        modifier =
            modifier
                .padding(5.dp)
                .clip(shape = RoundedCornerShape(26.dp))
                .background(colorScheme.inverseOnSurface),
        horizontalArrangement = Arrangement.Center,
    ) {
        TabButton(
            shortcutTab = leftShortcut,
            isTabSelected = selectedTab == leftShortcut,
            colorScheme = colorScheme,
            onTabClick = { selectedTab = leftShortcut },
        )
        TabButton(
            shortcutTab = rightShortcut,
            isTabSelected = selectedTab == rightShortcut,
            colorScheme = colorScheme,
            onTabClick = { selectedTab = rightShortcut },
        )
    }
}

@Composable
private fun TabButton(
    modifier: Modifier = Modifier,
    shortcutTab: FloatingToolbarTabViewModel,
    isTabSelected: Boolean,
    colorScheme: ColorScheme,
    onTabClick: () -> Unit,
) {
    Button(
        onClick = onTabClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors =
            ButtonColors(
                containerColor = if (isTabSelected) colorScheme.onSurface else Color.Transparent,
                contentColor = colorScheme.primary,
                disabledContainerColor = colorScheme.onSurface,
                disabledContentColor = colorScheme.onSurface,
            ),
    ) {
        Text(
            text = shortcutTab.text,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier,
        )
    }
}

@Composable
private fun ShortcutItem(modifier: Modifier = Modifier, slotIcon: OptionItemViewModel2<Icon>) {
    // TODO (b/404820955): Get the isSelected state from the view model
    var isSelected by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(65.dp)) {
        FilledTonalButton(
            onClick = { isSelected = !isSelected },
            shape = if (isSelected) RoundedCornerShape(20.dp) else CircleShape,
            modifier = Modifier.size(56.dp),
        ) {
            when (val curIcon = slotIcon.payload) {
                is Icon.Resource -> {
                    val drawable = painterResource(curIcon.res)
                    ComposeIcon(
                        painter = drawable,
                        contentDescription = slotIcon.text.asComposeString(),
                    )
                }
                is Icon.Loaded -> {
                    ComposeIcon(
                        painter = rememberDrawablePainter(curIcon.drawable),
                        contentDescription = slotIcon.text.asComposeString(),
                    )
                }
                else -> {}
            }
        }
        Text(
            text = slotIcon.text.asComposeString(),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(55.dp).wrapContentHeight(),
        )
    }
}
