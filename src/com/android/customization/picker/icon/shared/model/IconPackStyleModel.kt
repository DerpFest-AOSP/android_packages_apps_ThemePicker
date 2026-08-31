/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.customization.picker.icon.shared.model

import android.graphics.drawable.Drawable
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text

/** Style option for a Play Store icon pack in the Icons Style carousel. */
class IconPackStyleModel(
    iconStyle: IconPackStyle,
    name: Text,
    val packIcon: Drawable?,
) : IconStyleModel(iconStyle = iconStyle, name = name)
