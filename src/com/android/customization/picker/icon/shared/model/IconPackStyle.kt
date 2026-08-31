/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.customization.picker.icon.shared.model

import android.stats.style.StyleEnums.APP_ICON_STYLE_UNSPECIFIED
import com.android.customization.module.logging.ThemesUserEventLogger.AppIconStyle

/** Icon style backed by an installed Play Store icon pack. */
data class IconPackStyle(val packageName: String) : IconStyle {
    override val nameResId: Int = 0
    @AppIconStyle override val loggingId: Int = APP_ICON_STYLE_UNSPECIFIED
}
