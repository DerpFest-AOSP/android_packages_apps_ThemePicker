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

import android.stats.style.StyleEnums.CLOCK_SIZE_UNSPECIFIED
import android.stats.style.StyleEnums.COLOR_SOURCE_UNSPECIFIED
import android.stats.style.StyleEnums.DATE_PREFERENCE_UNSPECIFIED
import android.stats.style.StyleEnums.EFFECT_PREFERENCE_UNSPECIFIED
import android.stats.style.StyleEnums.LAUNCHED_PREFERENCE_UNSPECIFIED
import android.stats.style.StyleEnums.LOCATION_PREFERENCE_UNSPECIFIED
import android.stats.style.StyleEnums.SCREEN_UNSPECIFIED
import android.stats.style.StyleEnums.SET_WALLPAPER_ENTRY_POINT_UNSPECIFIED
import android.stats.style.StyleEnums.WALLPAPER_DESTINATION_UNSPECIFIED
import com.android.customization.module.logging.ThemesUserEventLogger.ClockSize
import com.android.customization.module.logging.ThemesUserEventLogger.ColorSource
import com.android.systemui.shared.system.SysUiStatsLog
import com.android.systemui.shared.system.SysUiStatsLog.STYLE_UI_CHANGED
import com.android.wallpaper.module.logging.UserEventLogger.CustomizationPickerScreen
import com.android.wallpaper.module.logging.UserEventLogger.DatePreference
import com.android.wallpaper.module.logging.UserEventLogger.EffectStatus
import com.android.wallpaper.module.logging.UserEventLogger.LaunchedPreference
import com.android.wallpaper.module.logging.UserEventLogger.LocationPreference
import com.android.wallpaper.module.logging.UserEventLogger.SetWallpaperEntryPoint
import com.android.wallpaper.module.logging.UserEventLogger.WallpaperDestination

class SysUiStatsLoggerImpl(val action: Int) : SysUiStatsLogger {

    private var colorPackageHash = 0
    private var fontPackageHash = 0
    private var shapePackageHash = 0
    private var clockPackageHash = 0
    private var launcherGrid = 0
    private var wallpaperCategoryHash = 0
    private var wallpaperIdHash = 0
    private var colorPreference = 0
    @LocationPreference private var locationPreference = LOCATION_PREFERENCE_UNSPECIFIED
    @DatePreference private var datePreference = DATE_PREFERENCE_UNSPECIFIED
    @LaunchedPreference private var launchedPreference = LAUNCHED_PREFERENCE_UNSPECIFIED
    @EffectStatus private var effectPreference = EFFECT_PREFERENCE_UNSPECIFIED
    private var effectIdHash = 0
    private var lockWallpaperCategoryHash = 0
    private var lockWallpaperIdHash = 0
    private var firstLaunchDateSinceSetup = 0
    private var firstWallpaperApplyDateSinceSetup = 0
    private var appLaunchCount = 0
    private var colorVariant = 0
    private var timeElapsedMillis = 0L
    private var effectResultCode = -1
    private var appSessionId = 0
    @SetWallpaperEntryPoint
    private var setWallpaperEntryPoint = SET_WALLPAPER_ENTRY_POINT_UNSPECIFIED
    @WallpaperDestination private var wallpaperDestination = WALLPAPER_DESTINATION_UNSPECIFIED
    @ColorSource private var colorSource = COLOR_SOURCE_UNSPECIFIED
    private var seedColor = 0
    @ClockSize private var clockSize = CLOCK_SIZE_UNSPECIFIED
    private var toggleOn = false
    private var shortcut = ""
    private var shortcutSlotId = ""
    private var lockEffectIdHash = 0
    private var clockSeedColor = 0
    @CustomizationPickerScreen private var customizationPickerScreen = SCREEN_UNSPECIFIED

    override fun setColorPackageHash(colorPackageHash: Int) = apply {
        this.colorPackageHash = colorPackageHash
    }

    override fun setFontPackageHash(fontPackageHash: Int) = apply {
        this.fontPackageHash = fontPackageHash
    }

    override fun setShapePackageHash(shapePackageHash: Int) = apply {
        this.shapePackageHash = shapePackageHash
    }

    override fun setClockPackageHash(clockPackageHash: Int) = apply {
        this.clockPackageHash = clockPackageHash
    }

    override fun setLauncherGrid(launcherGrid: Int) = apply { this.launcherGrid = launcherGrid }

    override fun setWallpaperCategoryHash(wallpaperCategoryHash: Int) = apply {
        this.wallpaperCategoryHash = wallpaperCategoryHash
    }

    override fun setWallpaperIdHash(wallpaperIdHash: Int) = apply {
        this.wallpaperIdHash = wallpaperIdHash
    }

    override fun setColorPreference(colorPreference: Int) = apply {
        this.colorPreference = colorPreference
    }

    override fun setLocationPreference(@LocationPreference locationPreference: Int) = apply {
        this.locationPreference = locationPreference
    }

    override fun setDatePreference(@DatePreference datePreference: Int) = apply {
        this.datePreference = datePreference
    }

    override fun setLaunchedPreference(@LaunchedPreference launchedPreference: Int) = apply {
        this.launchedPreference = launchedPreference
    }

    override fun setEffectPreference(@EffectStatus effectPreference: Int) = apply {
        this.effectPreference = effectPreference
    }

    override fun setEffectIdHash(effectIdHash: Int) = apply { this.effectIdHash = effectIdHash }

    override fun setLockWallpaperCategoryHash(lockWallpaperCategoryHash: Int) = apply {
        this.lockWallpaperCategoryHash = lockWallpaperCategoryHash
    }

    override fun setLockWallpaperIdHash(lockWallpaperIdHash: Int) = apply {
        this.lockWallpaperIdHash = lockWallpaperIdHash
    }

    override fun setFirstLaunchDateSinceSetup(firstLaunchDateSinceSetup: Int) = apply {
        this.firstLaunchDateSinceSetup = firstLaunchDateSinceSetup
    }

    override fun setFirstWallpaperApplyDateSinceSetup(firstWallpaperApplyDateSinceSetup: Int) =
        apply {
            this.firstWallpaperApplyDateSinceSetup = firstWallpaperApplyDateSinceSetup
        }

    override fun setAppLaunchCount(appLaunchCount: Int) = apply {
        this.appLaunchCount = appLaunchCount
    }

    override fun setColorVariant(colorVariant: Int) = apply { this.colorVariant = colorVariant }

    override fun setTimeElapsed(timeElapsedMillis: Long) = apply {
        this.timeElapsedMillis = timeElapsedMillis
    }

    override fun setEffectResultCode(effectResultCode: Int) = apply {
        this.effectResultCode = effectResultCode
    }

    override fun setAppSessionId(sessionId: Int) = apply { this.appSessionId = sessionId }

    override fun setSetWallpaperEntryPoint(@SetWallpaperEntryPoint setWallpaperEntryPoint: Int) =
        apply {
            this.setWallpaperEntryPoint = setWallpaperEntryPoint
        }

    override fun setWallpaperDestination(@WallpaperDestination wallpaperDestination: Int) = apply {
        this.wallpaperDestination = wallpaperDestination
    }

    override fun setColorSource(@ColorSource colorSource: Int) = apply {
        this.colorSource = colorSource
    }

    override fun setSeedColor(seedColor: Int) = apply { this.seedColor = seedColor }

    override fun setClockSize(@ClockSize clockSize: Int) = apply { this.clockSize = clockSize }

    override fun setToggleOn(toggleOn: Boolean) = apply { this.toggleOn = toggleOn }

    override fun setShortcut(shortcut: String) = apply { this.shortcut = shortcut }

    override fun setShortcutSlotId(shortcutSlotId: String) = apply {
        this.shortcutSlotId = shortcutSlotId
    }

    override fun setLockEffectIdHash(lockEffectIdHash: Int) = apply {
        this.lockEffectIdHash = lockEffectIdHash
    }

    override fun setClockSeedColor(clockSeedColor: Int) = apply {
        this.clockSeedColor = clockSeedColor
    }

    override fun setCustomizationPickerScreen(
        @CustomizationPickerScreen customizationPickerScreen: Int
    ) = apply { this.customizationPickerScreen = customizationPickerScreen }

    override fun log() {
        SysUiStatsLog.write(
            STYLE_UI_CHANGED,
            action,
            colorPackageHash,
            fontPackageHash,
            shapePackageHash,
            clockPackageHash,
            launcherGrid,
            wallpaperCategoryHash,
            wallpaperIdHash,
            colorPreference,
            locationPreference,
            datePreference,
            launchedPreference,
            effectPreference,
            effectIdHash,
            lockWallpaperCategoryHash,
            lockWallpaperIdHash,
            firstLaunchDateSinceSetup,
            firstWallpaperApplyDateSinceSetup,
            appLaunchCount,
            colorVariant,
            timeElapsedMillis,
            effectResultCode,
            appSessionId,
            setWallpaperEntryPoint,
            wallpaperDestination,
            colorSource,
            seedColor,
            clockSize,
            toggleOn,
            shortcut,
            shortcutSlotId,
            lockEffectIdHash,
            clockSeedColor,
            customizationPickerScreen,
        )
    }
}
