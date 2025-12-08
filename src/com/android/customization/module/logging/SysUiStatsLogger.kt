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
package com.android.customization.module.logging

import com.android.customization.module.logging.ThemesUserEventLogger.AppIconStyle
import com.android.customization.module.logging.ThemesUserEventLogger.ClockSize
import com.android.customization.module.logging.ThemesUserEventLogger.ColorSource
import com.android.wallpaper.module.logging.UserEventLogger.CustomizationPickerScreen
import com.android.wallpaper.module.logging.UserEventLogger.DatePreference
import com.android.wallpaper.module.logging.UserEventLogger.EffectStatus
import com.android.wallpaper.module.logging.UserEventLogger.LaunchedPreference
import com.android.wallpaper.module.logging.UserEventLogger.LocationPreference
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.module.logging.UserEventLogger.WallpaperDestination

/**
 * Interface defining the contract for logging Style UI changes via SysUiStatsLog. This follows a
 * builder pattern.
 */
interface SysUiStatsLogger {

    fun setColorPackageHash(colorPackageHash: Int): SysUiStatsLogger

    fun setFontPackageHash(fontPackageHash: Int): SysUiStatsLogger

    fun setShapePackageHash(shapePackageHash: Int): SysUiStatsLogger

    fun setClockPackageHash(clockPackageHash: Int): SysUiStatsLogger

    fun setLauncherGrid(launcherGrid: Int): SysUiStatsLogger

    fun setWallpaperCategoryHash(wallpaperCategoryHash: Int): SysUiStatsLogger

    fun setWallpaperIdHash(wallpaperIdHash: Int): SysUiStatsLogger

    fun setColorPreference(colorPreference: Int): SysUiStatsLogger

    fun setLocationPreference(@LocationPreference locationPreference: Int): SysUiStatsLogger

    fun setDatePreference(@DatePreference datePreference: Int): SysUiStatsLogger

    fun setLaunchedPreference(@LaunchedPreference launchedPreference: Int): SysUiStatsLogger

    fun setEffectPreference(@EffectStatus effectPreference: Int): SysUiStatsLogger

    fun setEffectIdHash(effectIdHash: Int): SysUiStatsLogger

    fun setLockWallpaperCategoryHash(lockWallpaperCategoryHash: Int): SysUiStatsLogger

    fun setLockWallpaperIdHash(lockWallpaperIdHash: Int): SysUiStatsLogger

    fun setFirstLaunchDateSinceSetup(firstLaunchDateSinceSetup: Int): SysUiStatsLogger

    fun setFirstWallpaperApplyDateSinceSetup(
        firstWallpaperApplyDateSinceSetup: Int
    ): SysUiStatsLogger

    fun setAppLaunchCount(appLaunchCount: Int): SysUiStatsLogger

    fun setColorVariant(colorVariant: Int): SysUiStatsLogger

    fun setTimeElapsed(timeElapsedMillis: Long): SysUiStatsLogger

    fun setEffectResultCode(effectResultCode: Int): SysUiStatsLogger

    fun setAppSessionId(sessionId: Int): SysUiStatsLogger

    fun setSetWallpaperEntryPoint(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int
    ): SysUiStatsLogger

    fun setWallpaperDestination(@WallpaperDestination wallpaperDestination: Int): SysUiStatsLogger

    fun setColorSource(@ColorSource colorSource: Int): SysUiStatsLogger

    fun setSeedColor(seedColor: Int): SysUiStatsLogger

    fun setClockSize(@ClockSize clockSize: Int): SysUiStatsLogger

    fun setToggleOn(toggleOn: Boolean): SysUiStatsLogger

    fun setShortcut(shortcut: String): SysUiStatsLogger

    fun setShortcutSlotId(shortcutSlotId: String): SysUiStatsLogger

    fun setLockEffectIdHash(lockEffectIdHash: Int): SysUiStatsLogger

    fun setClockSeedColor(clockSeedColor: Int): SysUiStatsLogger

    fun setCustomizationPickerScreen(
        @CustomizationPickerScreen customizationPickerScreen: Int
    ): SysUiStatsLogger

    fun setAppIconStyle(@AppIconStyle appIconStyle: Int): SysUiStatsLogger

    fun setUseClockCustomization(useClockCustomization: Boolean): SysUiStatsLogger

    fun log()
}
