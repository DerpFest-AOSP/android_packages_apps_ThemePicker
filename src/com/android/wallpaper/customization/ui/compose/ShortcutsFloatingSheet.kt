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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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

// TODO: b/404820955 - Plug correct colors inherited from the device into all composables
// TODO(b/409112907) Evaluate Compose performance before enabling flag
@Composable
fun ShortcutsFloatingSheet(
    viewModel: KeyguardQuickAffordancePickerViewModel2,
    modifier: Modifier = Modifier,
) {
    val items by viewModel.quickAffordances.collectAsStateWithLifecycle(emptyList())
    val tabs by viewModel.tabs.collectAsStateWithLifecycle(emptyList())

    PlatformTheme {
        Column(
            modifier = modifier.padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalArrangement = Arrangement.Center,
                modifier =
                    Modifier.fillMaxWidth()
                        .padding(
                            horizontal =
                                dimensionResource(R.dimen.floating_sheet_horizontal_padding)
                        )
                        .clip(shape = RoundedCornerShape(28.dp))
                        .background(Color.DarkGray)
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
    Row(
        modifier =
            modifier.padding(6.dp).clip(shape = RoundedCornerShape(56.dp)).background(Color.Gray),
        horizontalArrangement = Arrangement.Center,
    ) {
        TabButton(shortcutTab = leftShortcut)
        TabButton(shortcutTab = rightShortcut)
    }
}

@Composable
private fun TabButton(modifier: Modifier = Modifier, shortcutTab: FloatingToolbarTabViewModel) {
    val buttonClickableOrNull = shortcutTab.onClick?.takeIf { !shortcutTab.isSelected }
    Button(
        enabled = buttonClickableOrNull != null,
        onClick = { shortcutTab.onClick?.invoke() },
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(56.dp),
        colors =
            ButtonColors(
                containerColor =
                    if (shortcutTab.isSelected) colorScheme.background else Color.Transparent,
                contentColor = colorScheme.primary,
                disabledContainerColor = colorScheme.inverseOnSurface,
                disabledContentColor = colorScheme.inverseOnSurface,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AnimatedVisibility(shortcutTab.isSelected) {
                when (shortcutTab.icon) {
                    is Icon.Resource -> {
                        val drawable = painterResource((shortcutTab.icon as Icon.Resource).res)
                        Image(
                            painter = drawable,
                            contentDescription = shortcutTab.text,
                            modifier = Modifier,
                        )
                    }

                    is Icon.Loaded -> {
                        Image(
                            painter =
                                rememberDrawablePainter((shortcutTab.icon as Icon.Loaded).drawable),
                            contentDescription = shortcutTab.text,
                            modifier = Modifier,
                        )
                    }
                }
            }
            Text(
                text = shortcutTab.text,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ShortcutItem(modifier: Modifier = Modifier, slotIcon: OptionItemViewModel2<Icon>) {
    val iconState by slotIcon.onClicked.collectAsStateWithLifecycle({})
    val isIconSelected by slotIcon.isSelected.collectAsStateWithLifecycle(false)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(65.dp).padding(5.dp),
    ) {
        val selectedDP = if (isIconSelected) 20.dp else 56.dp
        val selectedColor = if (isIconSelected) Color.LightGray else Color.Gray
        val cornerShapeRadius =
            animateDpAsState(
                targetValue = selectedDP,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
            )
        val iconColor = animateColorAsState(selectedColor)
        FilledTonalButton(
            enabled = slotIcon.isEnabled,
            onClick = { iconState?.invoke() },
            shape = RoundedCornerShape(cornerShapeRadius.value),
            modifier = Modifier.size(56.dp),
            contentPadding = PaddingValues(16.dp),
            colors =
                ButtonColors(
                    containerColor = iconColor.value,
                    contentColor = iconColor.value,
                    disabledContainerColor = iconColor.value,
                    disabledContentColor = iconColor.value,
                ),
        ) {
            when (val curIcon = slotIcon.payload) {
                is Icon.Resource -> {
                    val drawable = painterResource(curIcon.res)
                    Image(
                        painter = drawable,
                        contentDescription = slotIcon.text.asComposeString(),
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                is Icon.Loaded -> {
                    Image(
                        painter = rememberDrawablePainter(curIcon.drawable),
                        contentDescription = slotIcon.text.asComposeString(),
                        modifier = Modifier.fillMaxSize(),
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
            color = Color.White,
        )
    }
}
