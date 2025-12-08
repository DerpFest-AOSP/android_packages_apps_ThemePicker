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
package com.android.customization.picker.color.data.util

import android.app.WallpaperColors
import android.content.Context
import android.content.res.Configuration
import android.content.theming.ThemeStyle
import android.provider.Settings
import android.util.Log
import com.android.customization.model.ResourceConstants
import com.android.systemui.monet.ColorScheme
import com.android.systemui.monet.DynamicColors
import com.android.systemui.shared.settings.data.repository.SecureSettingsRepository
import com.google.ux.material.libmonet.dynamiccolor.DynamicColor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONException
import org.json.JSONObject

/**
 * Extract material next colors from wallpaper colors. Based on Nexus Launcher's
 * MaterialColorsGenerator, nexuslauncher/widget/MaterialColorsGenerator.java
 */
@Singleton
class MaterialColorsGenerator
@Inject
constructor(
    @ApplicationContext private val applicationContext: Context,
    private val secureSettingsRepository: SecureSettingsRepository,
) {
    private fun addDynamicColors(
        lightColorScheme: ColorScheme,
        darkColorScheme: ColorScheme,
        colors: MutableList<android.util.Pair<String, DynamicColor>>,
        isFixed: Boolean,
        isDarkMode: Boolean,
    ): Map<Int, Int> = buildMap {
        for (p in colors) {
            val color = p.second as DynamicColor
            val name = p.first
            if (isFixed) {
                put(
                    applicationContext.resources.getIdentifier(
                        "android:color/system_$name",
                        null,
                        null,
                    ),
                    // -0x1000000 is equivalent to 0xff000000 which doesn't fit in a Kotlin Int
                    -0x1000000 or color.getArgb(lightColorScheme.materialScheme),
                )
            } else {
                put(
                    applicationContext.resources.getIdentifier(
                        "android:color/system_$name",
                        null,
                        null,
                    ),
                    -0x1000000 or
                        color.getArgb(
                            (if (isDarkMode) darkColorScheme else lightColorScheme).materialScheme
                        ),
                )
                put(
                    applicationContext.resources.getIdentifier(
                        "android:color/system_${name}_dark",
                        null,
                        null,
                    ),
                    // -0x1000000 is equivalent to 0xff000000 which doesn't fit in a Kotlin Int
                    -0x1000000 or color.getArgb(darkColorScheme.materialScheme),
                )
                put(
                    applicationContext.resources.getIdentifier(
                        "android:color/system_${name}_light",
                        null,
                        null,
                    ),
                    -0x1000000 or color.getArgb(lightColorScheme.materialScheme),
                )
            }
        }
        remove(0)
        remove(-1)
    }

    /**
     * Generates the mapping from system color resources to values from wallpaper colors.
     *
     * @return a list of color resource IDs and a corresponding list of their color values
     */
    suspend fun generate(colors: WallpaperColors): Pair<IntArray, IntArray> {
        val isDarkMode =
            (applicationContext.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return generate(
            ColorScheme(colors, false, fetchThemeStyleFromSetting()),
            ColorScheme(colors, true, fetchThemeStyleFromSetting()),
            isDarkMode,
        )
    }

    /**
     * Generates the mapping from system color resources to values from color seed and style.
     *
     * @return a list of color resource IDs and a corresponding list of their color values
     */
    fun generate(
        colorSeed: Int,
        @ThemeStyle.Type style: Int,
        useDarkMode: Boolean?,
    ): Pair<IntArray, IntArray> {
        val isDarkMode =
            useDarkMode
                ?: ((applicationContext.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
        return generate(
            ColorScheme(colorSeed, false, style),
            ColorScheme(colorSeed, true, style),
            isDarkMode,
        )
    }

    private fun generate(
        lightColorScheme: ColorScheme,
        darkColorScheme: ColorScheme,
        isDarkMode: Boolean,
    ): Pair<IntArray, IntArray> {
        val colorMap: MutableMap<Int, Int> = mutableMapOf()

        colorMap.apply {
            putAll(
                addDynamicColors(
                    lightColorScheme,
                    darkColorScheme,
                    DynamicColors.getAllNeutralPalette(),
                    false,
                    isDarkMode,
                )
            )
            putAll(
                addDynamicColors(
                    lightColorScheme,
                    darkColorScheme,
                    DynamicColors.getAllAccentPalette(),
                    false,
                    isDarkMode,
                )
            )
            putAll(
                addDynamicColors(
                    lightColorScheme,
                    darkColorScheme,
                    DynamicColors.getAllDynamicColorsMapped(),
                    false,
                    isDarkMode,
                )
            )
            putAll(
                addDynamicColors(
                    lightColorScheme,
                    darkColorScheme,
                    DynamicColors.getFixedColorsMapped(),
                    true,
                    isDarkMode,
                )
            )
            putAll(
                addDynamicColors(
                    lightColorScheme,
                    darkColorScheme,
                    DynamicColors.getCustomColorsMapped(),
                    false,
                    isDarkMode,
                )
            )
        }

        return Pair(colorMap.keys.toIntArray(), colorMap.values.toIntArray())
    }

    @ThemeStyle.Type
    private suspend fun fetchThemeStyleFromSetting(): Int {
        val overlayPackageJson =
            secureSettingsRepository.getString(Settings.Secure.THEME_CUSTOMIZATION_OVERLAY_PACKAGES)
        return if (!overlayPackageJson.isNullOrEmpty()) {
            try {
                val jsonObject = JSONObject(overlayPackageJson)
                ThemeStyle.valueOf(
                    jsonObject.getString(ResourceConstants.OVERLAY_CATEGORY_THEME_STYLE)
                )
            } catch (e: (JSONException)) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                ThemeStyle.TONAL_SPOT
            } catch (e: IllegalArgumentException) {
                Log.i(TAG, "Failed to parse THEME_CUSTOMIZATION_OVERLAY_PACKAGES.", e)
                ThemeStyle.TONAL_SPOT
            }
        } else {
            ThemeStyle.TONAL_SPOT
        }
    }

    companion object {
        private const val TAG = "MaterialColorsGenerator"
    }
}
