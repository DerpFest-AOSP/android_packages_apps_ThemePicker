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

class FakeSysUiStatsLogger(val action: Int) : SysUiStatsLogger {
    var colorPackageHash: Int? = null
    var fontPackageHash: Int? = null
    var shapePackageHash: Int? = null
    var clockPackageHash: Int? = null
    var launcherGrid: Int? = null
    var wallpaperCategoryHash: Int? = null
    var wallpaperIdHash: Int? = null
    var colorPreference: Int? = null
    @LocationPreference var locationPreference: Int? = null
    @DatePreference var datePreference: Int? = null
    @LaunchedPreference var launchedPreference: Int? = null
    @EffectStatus var effectPreference: Int? = null
    var effectIdHash: Int? = null
    var lockWallpaperCategoryHash: Int? = null
    var lockWallpaperIdHash: Int? = null
    var firstLaunchDateSinceSetup: Int? = null
    var firstWallpaperApplyDateSinceSetup: Int? = null
    var appLaunchCount: Int? = null
    var colorVariant: Int? = null
    var timeElapsedMillis: Long? = null
    var effectResultCode: Int? = null
    var appSessionId: Int? = null
    @SetWallpaperEntryPoint var setWallpaperEntryPoint: Int? = null
    @WallpaperDestination var wallpaperDestination: Int? = null
    @ColorSource var colorSource: Int? = null
    var seedColor: Int? = null
    @ClockSize var clockSize: Int? = null
    var toggleOn: Boolean? = null
    var shortcut: String? = null
    var shortcutSlotId: String? = null
    var lockEffectIdHash: Int? = null
    var clockSeedColor: Int? = null
    @CustomizationPickerScreen var customizationPickerScreen: Int? = null
    @AppIconStyle var appIconStyle: Int? = null
    var useClockCustomization: Boolean? = null

    // --- Property to track if log() was called ---
    var logCalled: Boolean = false
        private set // Allow reading but prevent external modification

    // --- Method Implementations ---

    override fun setColorPackageHash(colorPackageHash: Int): SysUiStatsLogger = apply {
        this.colorPackageHash = colorPackageHash
    }

    override fun setFontPackageHash(fontPackageHash: Int): SysUiStatsLogger = apply {
        this.fontPackageHash = fontPackageHash
    }

    override fun setShapePackageHash(shapePackageHash: Int): SysUiStatsLogger = apply {
        this.shapePackageHash = shapePackageHash
    }

    override fun setClockPackageHash(clockPackageHash: Int): SysUiStatsLogger = apply {
        this.clockPackageHash = clockPackageHash
    }

    override fun setLauncherGrid(launcherGrid: Int): SysUiStatsLogger = apply {
        this.launcherGrid = launcherGrid
    }

    override fun setWallpaperCategoryHash(wallpaperCategoryHash: Int): SysUiStatsLogger = apply {
        this.wallpaperCategoryHash = wallpaperCategoryHash
    }

    override fun setWallpaperIdHash(wallpaperIdHash: Int): SysUiStatsLogger = apply {
        this.wallpaperIdHash = wallpaperIdHash
    }

    override fun setColorPreference(colorPreference: Int): SysUiStatsLogger = apply {
        this.colorPreference = colorPreference
    }

    override fun setLocationPreference(
        @LocationPreference locationPreference: Int
    ): SysUiStatsLogger = apply { this.locationPreference = locationPreference }

    override fun setDatePreference(@DatePreference datePreference: Int): SysUiStatsLogger = apply {
        this.datePreference = datePreference
    }

    override fun setLaunchedPreference(
        @LaunchedPreference launchedPreference: Int
    ): SysUiStatsLogger = apply { this.launchedPreference = launchedPreference }

    override fun setEffectPreference(@EffectStatus effectPreference: Int): SysUiStatsLogger =
        apply {
            this.effectPreference = effectPreference
        }

    override fun setEffectIdHash(effectIdHash: Int): SysUiStatsLogger = apply {
        this.effectIdHash = effectIdHash
    }

    override fun setLockWallpaperCategoryHash(lockWallpaperCategoryHash: Int): SysUiStatsLogger =
        apply {
            this.lockWallpaperCategoryHash = lockWallpaperCategoryHash
        }

    override fun setLockWallpaperIdHash(lockWallpaperIdHash: Int): SysUiStatsLogger = apply {
        this.lockWallpaperIdHash = lockWallpaperIdHash
    }

    override fun setFirstLaunchDateSinceSetup(firstLaunchDateSinceSetup: Int): SysUiStatsLogger =
        apply {
            this.firstLaunchDateSinceSetup = firstLaunchDateSinceSetup
        }

    override fun setFirstWallpaperApplyDateSinceSetup(
        firstWallpaperApplyDateSinceSetup: Int
    ): SysUiStatsLogger = apply {
        this.firstWallpaperApplyDateSinceSetup = firstWallpaperApplyDateSinceSetup
    }

    override fun setAppLaunchCount(appLaunchCount: Int): SysUiStatsLogger = apply {
        this.appLaunchCount = appLaunchCount
    }

    override fun setColorVariant(colorVariant: Int): SysUiStatsLogger = apply {
        this.colorVariant = colorVariant
    }

    override fun setTimeElapsed(timeElapsedMillis: Long): SysUiStatsLogger = apply {
        this.timeElapsedMillis = timeElapsedMillis
    }

    override fun setEffectResultCode(effectResultCode: Int): SysUiStatsLogger = apply {
        this.effectResultCode = effectResultCode
    }

    override fun setAppSessionId(sessionId: Int): SysUiStatsLogger = apply {
        this.appSessionId = sessionId
    }

    override fun setSetWallpaperEntryPoint(
        @SetWallpaperEntryPoint setWallpaperEntryPoint: Int
    ): SysUiStatsLogger = apply { this.setWallpaperEntryPoint = setWallpaperEntryPoint }

    override fun setWallpaperDestination(
        @WallpaperDestination wallpaperDestination: Int
    ): SysUiStatsLogger = apply { this.wallpaperDestination = wallpaperDestination }

    override fun setColorSource(@ColorSource colorSource: Int): SysUiStatsLogger = apply {
        this.colorSource = colorSource
    }

    override fun setSeedColor(seedColor: Int): SysUiStatsLogger = apply {
        this.seedColor = seedColor
    }

    override fun setClockSize(@ClockSize clockSize: Int): SysUiStatsLogger = apply {
        this.clockSize = clockSize
    }

    override fun setToggleOn(toggleOn: Boolean): SysUiStatsLogger = apply {
        this.toggleOn = toggleOn
    }

    override fun setShortcut(shortcut: String): SysUiStatsLogger = apply {
        this.shortcut = shortcut
    }

    override fun setShortcutSlotId(shortcutSlotId: String): SysUiStatsLogger = apply {
        this.shortcutSlotId = shortcutSlotId
    }

    override fun setLockEffectIdHash(lockEffectIdHash: Int): SysUiStatsLogger = apply {
        this.lockEffectIdHash = lockEffectIdHash
    }

    override fun setClockSeedColor(clockSeedColor: Int): SysUiStatsLogger = apply {
        this.clockSeedColor = clockSeedColor
    }

    override fun setCustomizationPickerScreen(
        @CustomizationPickerScreen customizationPickerScreen: Int
    ): SysUiStatsLogger = apply { this.customizationPickerScreen = customizationPickerScreen }

    override fun setAppIconStyle(@AppIconStyle appIconStyle: Int): SysUiStatsLogger = apply {
        this.appIconStyle = appIconStyle
    }

    override fun setUseClockCustomization(useClockCustomization: Boolean): SysUiStatsLogger =
        apply {
            this.useClockCustomization = useClockCustomization
        }

    override fun log() {
        this.logCalled = true
    }
}
