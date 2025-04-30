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

import android.app.WallpaperManager
import android.content.Intent
import android.graphics.Color
import android.stats.style.StyleEnums
import android.stats.style.StyleEnums.SCREEN_CLOCK
import android.stats.style.StyleEnums.SCREEN_COLORS
import android.stats.style.StyleEnums.SCREEN_ICONS
import android.stats.style.StyleEnums.SCREEN_LAYOUT
import android.stats.style.StyleEnums.SCREEN_SHORTCUTS
import android.stats.style.StyleEnums.SNAPSHOT
import androidx.test.filters.SmallTest
import com.android.customization.model.color.ColorCustomizationManager
import com.android.customization.model.grid.GridOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.GRID
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.SHORTCUTS
import com.android.wallpaper.module.WallpaperPreferences
import com.android.wallpaper.util.LaunchSourceUtils
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ThemesUserEventLoggerImplTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var wallpaperPreferences: WallpaperPreferences
    @Inject lateinit var colorCustomizationManager: ColorCustomizationManager
    @Inject lateinit var appSessionId: AppSessionId
    @Inject lateinit var sysUiStatsLoggerFactory: SysUiStatsLoggerFactory

    private lateinit var underTest: ThemesUserEventLoggerImpl

    // Helper to access the last created fake logger instance
    private val fakeStatsLogger: FakeSysUiStatsLogger
        get() =
            (sysUiStatsLoggerFactory as FakeSysUiStatsLoggerFactory).sysUiStatsLogger
                as FakeSysUiStatsLogger

    @Before
    fun setUp() {
        hiltRule.inject()

        wallpaperPreferences.setHomeWallpaperCollectionId(TEST_WALLPAPER_COLLECTION_ID)
        wallpaperPreferences.setHomeWallpaperRemoteId(TEST_WALLPAPER_REMOTE_ID)
        wallpaperPreferences.setHomeWallpaperEffects(TEST_WALLPAPER_EFFECT)
        wallpaperPreferences.setLockWallpaperCollectionId(TEST_LOCK_WALLPAPER_COLLECTION_ID)
        wallpaperPreferences.setLockWallpaperRemoteId(TEST_LOCK_WALLPAPER_REMOTE_ID)
        wallpaperPreferences.setLockWallpaperEffects(TEST_LOCK_WALLPAPER_EFFECT)

        underTest =
            ThemesUserEventLoggerImpl(
                wallpaperPreferences,
                colorCustomizationManager,
                appSessionId,
                sysUiStatsLoggerFactory,
            )
    }

    @Test
    fun logSnapshot_logsCorrectData() {
        underTest.logSnapshot()

        assertThat(fakeStatsLogger.action).isEqualTo(SNAPSHOT)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.wallpaperCategoryHash)
            .isEqualTo(TEST_WALLPAPER_COLLECTION_ID.hashCode())
        assertThat(fakeStatsLogger.wallpaperIdHash).isEqualTo(TEST_WALLPAPER_REMOTE_ID.hashCode())
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.lockWallpaperCategoryHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_COLLECTION_ID.hashCode())
        assertThat(fakeStatsLogger.lockWallpaperIdHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_REMOTE_ID.hashCode())
        assertThat(fakeStatsLogger.lockEffectIdHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.colorSource).isEqualTo(0)
        assertThat(fakeStatsLogger.colorVariant).isEqualTo(0)
        assertThat(fakeStatsLogger.seedColor).isEqualTo(0)
    }

    @Test
    fun logAppLaunched_usesNewSessionIdAndCorrectSource() {
        val intentLauncher =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_LAUNCHER,
                )
        val intentSettings =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_SETTINGS,
                )
        val intentSuw =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_SUW,
                )
        val intentTips =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_TIPS,
                )
        val intentDeepLink =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_DEEP_LINK,
                )
        val intentKeyguard =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_KEYGUARD,
                )
        val intentSettingsSearch =
            Intent()
                .putExtra(
                    LaunchSourceUtils.WALLPAPER_LAUNCH_SOURCE,
                    LaunchSourceUtils.LAUNCH_SOURCE_SETTINGS_SEARCH,
                )
        val intentCrop = Intent(WallpaperManager.ACTION_CROP_AND_SET_WALLPAPER)
        val intentIcon = Intent().addCategory(Intent.CATEGORY_LAUNCHER)
        val intentOther = Intent("some.other.ACTION") // Unspecified

        executeAndAssertAppLaunched(intentLauncher, StyleEnums.LAUNCHED_LAUNCHER)
        executeAndAssertAppLaunched(intentSettings, StyleEnums.LAUNCHED_SETTINGS)
        executeAndAssertAppLaunched(intentSuw, StyleEnums.LAUNCHED_SUW)
        executeAndAssertAppLaunched(intentTips, StyleEnums.LAUNCHED_TIPS)
        executeAndAssertAppLaunched(intentDeepLink, StyleEnums.LAUNCHED_DEEP_LINK)
        executeAndAssertAppLaunched(intentKeyguard, StyleEnums.LAUNCHED_KEYGUARD)
        executeAndAssertAppLaunched(intentSettingsSearch, StyleEnums.LAUNCHED_SETTINGS_SEARCH)
        executeAndAssertAppLaunched(intentCrop, StyleEnums.LAUNCHED_CROP_AND_SET_ACTION)
        executeAndAssertAppLaunched(intentIcon, StyleEnums.LAUNCHED_LAUNCH_ICON)
        executeAndAssertAppLaunched(intentOther, StyleEnums.LAUNCHED_PREFERENCE_UNSPECIFIED)
    }

    private fun executeAndAssertAppLaunched(intent: Intent, expectedSource: Int) {
        underTest.logAppLaunched(intent)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.APP_LAUNCHED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.launchedPreference).isEqualTo(expectedSource)
    }

    @Test
    fun logWallpaperApplied_homeOnly() {
        underTest.logWallpaperApplied(
            TEST_WALLPAPER_COLLECTION_ID,
            TEST_WALLPAPER_REMOTE_ID,
            TEST_WALLPAPER_EFFECT,
            StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
            StyleEnums.WALLPAPER_DESTINATION_HOME_SCREEN,
        )

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId)
            .isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID) // Uses existing ID
        assertThat(fakeStatsLogger.wallpaperCategoryHash)
            .isEqualTo(TEST_WALLPAPER_COLLECTION_ID.hashCode())
        assertThat(fakeStatsLogger.wallpaperIdHash).isEqualTo(TEST_WALLPAPER_REMOTE_ID.hashCode())
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.lockWallpaperCategoryHash).isEqualTo(0) // Not Lock
        assertThat(fakeStatsLogger.lockWallpaperIdHash).isEqualTo(0) // Not Lock
        assertThat(fakeStatsLogger.lockEffectIdHash).isEqualTo(0) // Not Lock
        assertThat(fakeStatsLogger.setWallpaperEntryPoint)
            .isEqualTo(StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW)
        assertThat(fakeStatsLogger.wallpaperDestination)
            .isEqualTo(StyleEnums.WALLPAPER_DESTINATION_HOME_SCREEN)
    }

    @Test
    fun logWallpaperApplied_lockOnly() {
        underTest.logWallpaperApplied(
            TEST_LOCK_WALLPAPER_COLLECTION_ID,
            TEST_LOCK_WALLPAPER_REMOTE_ID,
            TEST_LOCK_WALLPAPER_EFFECT,
            StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
            StyleEnums.WALLPAPER_DESTINATION_LOCK_SCREEN,
        )

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.wallpaperCategoryHash).isEqualTo(0) // Not Home
        assertThat(fakeStatsLogger.wallpaperIdHash).isEqualTo(0) // Not Home
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(0) // Not Home
        assertThat(fakeStatsLogger.lockWallpaperCategoryHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_COLLECTION_ID.hashCode())
        assertThat(fakeStatsLogger.lockWallpaperIdHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_REMOTE_ID.hashCode())
        assertThat(fakeStatsLogger.lockEffectIdHash)
            .isEqualTo(TEST_LOCK_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.setWallpaperEntryPoint)
            .isEqualTo(StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW)
        assertThat(fakeStatsLogger.wallpaperDestination)
            .isEqualTo(StyleEnums.WALLPAPER_DESTINATION_LOCK_SCREEN)
    }

    @Test
    fun logWallpaperApplied_homeAndLock() {
        underTest.logWallpaperApplied(
            TEST_WALLPAPER_COLLECTION_ID,
            TEST_WALLPAPER_REMOTE_ID,
            TEST_WALLPAPER_EFFECT,
            StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW,
            StyleEnums.WALLPAPER_DESTINATION_HOME_AND_LOCK_SCREEN,
        )

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.wallpaperCategoryHash)
            .isEqualTo(TEST_WALLPAPER_COLLECTION_ID.hashCode()) // Home
        assertThat(fakeStatsLogger.wallpaperIdHash)
            .isEqualTo(TEST_WALLPAPER_REMOTE_ID.hashCode()) // Home
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode()) // Home
        assertThat(fakeStatsLogger.lockWallpaperCategoryHash)
            .isEqualTo(TEST_WALLPAPER_COLLECTION_ID.hashCode()) // Lock
        assertThat(fakeStatsLogger.lockWallpaperIdHash)
            .isEqualTo(TEST_WALLPAPER_REMOTE_ID.hashCode()) // Lock
        assertThat(fakeStatsLogger.lockEffectIdHash)
            .isEqualTo(TEST_WALLPAPER_EFFECT.hashCode()) // Lock
        assertThat(fakeStatsLogger.setWallpaperEntryPoint)
            .isEqualTo(StyleEnums.SET_WALLPAPER_ENTRY_POINT_WALLPAPER_PREVIEW)
        assertThat(fakeStatsLogger.wallpaperDestination)
            .isEqualTo(StyleEnums.WALLPAPER_DESTINATION_HOME_AND_LOCK_SCREEN)
    }

    @Test
    fun logEffectApply() {
        underTest.logEffectApply(
            TEST_WALLPAPER_EFFECT,
            StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
            1234L,
            0,
        )

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_EFFECT_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.effectPreference).isEqualTo(StyleEnums.EFFECT_APPLIED_ON_SUCCESS)
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.timeElapsedMillis).isEqualTo(1234L)
        assertThat(fakeStatsLogger.effectResultCode).isEqualTo(0)
    }

    @Test
    fun logEffectProbe() {
        underTest.logEffectProbe(TEST_WALLPAPER_EFFECT, StyleEnums.EFFECT_APPLIED_STARTED)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_EFFECT_PROBE)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.effectPreference).isEqualTo(StyleEnums.EFFECT_APPLIED_STARTED)
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.timeElapsedMillis).isNull() // Not logged
        assertThat(fakeStatsLogger.effectResultCode).isNull() // Not logged
    }

    @Test
    fun logEffectForegroundDownload() {
        underTest.logEffectForegroundDownload(
            TEST_WALLPAPER_EFFECT,
            StyleEnums.EFFECT_APPLIED_ON_SUCCESS,
            5678L,
        )

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_EFFECT_FG_DOWNLOAD)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.effectPreference).isEqualTo(StyleEnums.EFFECT_APPLIED_ON_SUCCESS)
        assertThat(fakeStatsLogger.effectIdHash).isEqualTo(TEST_WALLPAPER_EFFECT.hashCode())
        assertThat(fakeStatsLogger.timeElapsedMillis).isEqualTo(5678L)
        assertThat(fakeStatsLogger.effectResultCode).isNull() // Not logged
    }

    @Test
    fun logResetApplied() {
        underTest.logResetApplied()

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.RESET_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
    }

    @Test
    fun logWallpaperExploreButtonClicked() {
        underTest.logWallpaperExploreButtonClicked()

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.WALLPAPER_EXPLORE)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
    }

    @Test
    fun logThemeColorApplied() {
        underTest.logThemeColorApplied(StyleEnums.COLOR_SOURCE_HOME_SCREEN_WALLPAPER, 5, 0xABCDEF)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.THEME_COLOR_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.colorSource)
            .isEqualTo(StyleEnums.COLOR_SOURCE_HOME_SCREEN_WALLPAPER)
        assertThat(fakeStatsLogger.colorVariant).isEqualTo(5)
        assertThat(fakeStatsLogger.seedColor).isEqualTo(0xABCDEF)
    }

    @Test
    fun logGridApplied() {
        val gridOption =
            GridOption("TestGrid", "TestGrid", true, 6, 4, android.net.Uri.EMPTY, 1, "", 0)

        underTest.logGridApplied(gridOption)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.GRID_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.launcherGrid).isEqualTo(406) // 4 * 100 + 6
    }

    @Test
    fun logClockApplied() {
        underTest.logClockApplied(TEST_CLOCK_ID)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.CLOCK_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.clockPackageHash).isEqualTo(TEST_CLOCK_ID.hashCode())
    }

    @Test
    fun logClockColorApplied() {
        underTest.logClockColorApplied(TEST_SEED_COLOR)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.CLOCK_COLOR_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.clockSeedColor).isEqualTo(TEST_SEED_COLOR)
    }

    @Test
    fun logClockSizeApplied() {
        underTest.logClockSizeApplied(StyleEnums.CLOCK_SIZE_SMALL)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.CLOCK_SIZE_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.clockSize).isEqualTo(StyleEnums.CLOCK_SIZE_SMALL)
    }

    @Test
    fun logThemedIconApplied_true() {
        underTest.logThemedIconApplied(true)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.THEMED_ICON_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isTrue()
    }

    @Test
    fun logThemedIconApplied_false() {
        underTest.logThemedIconApplied(false)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.THEMED_ICON_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isFalse()
    }

    @Test
    fun logLockScreenNotificationApplied_true() {
        underTest.logLockScreenNotificationApplied(true)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.LOCK_SCREEN_NOTIFICATION_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isTrue()
    }

    @Test
    fun logLockScreenNotificationApplied_false() {
        underTest.logLockScreenNotificationApplied(false)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.LOCK_SCREEN_NOTIFICATION_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isFalse()
    }

    @Test
    fun logShortcutApplied() {
        underTest.logShortcutApplied(TEST_SHORTCUT, TEST_SHORTCUT_SLOT)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.SHORTCUT_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.shortcut).isEqualTo(TEST_SHORTCUT)
        assertThat(fakeStatsLogger.shortcutSlotId).isEqualTo(TEST_SHORTCUT_SLOT)
    }

    @Test
    fun logDarkThemeApplied_true() {
        underTest.logDarkThemeApplied(true)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.DARK_THEME_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isTrue()
    }

    @Test
    fun logDarkThemeApplied_false() {
        underTest.logDarkThemeApplied(false)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.DARK_THEME_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.toggleOn).isFalse()
    }

    @Test
    fun logShapeApplied() {
        underTest.logShapeApplied(TEST_SHAPE_ID)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.SHAPE_APPLIED)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.shapePackageHash).isEqualTo(TEST_SHAPE_ID.hashCode())
    }

    @Test
    fun logEnterScreen() {
        underTest.logEnterScreen(SCREEN_CLOCK)

        assertThat(fakeStatsLogger.action).isEqualTo(StyleEnums.ENTER_SCREEN)
        assertThat(fakeStatsLogger.logCalled).isTrue()
        assertThat(fakeStatsLogger.appSessionId).isEqualTo(FakeAppSessionId.TEST_APP_SESSION_ID)
        assertThat(fakeStatsLogger.customizationPickerScreen).isEqualTo(SCREEN_CLOCK)
    }

    @Test
    fun transformCustomizationOptionToScreenForLogging() {
        assertThat(underTest.transformCustomizationOptionToScreenForLogging(COLORS))
            .isEqualTo(SCREEN_COLORS)
        assertThat(underTest.transformCustomizationOptionToScreenForLogging(APP_ICONS))
            .isEqualTo(SCREEN_ICONS)
        assertThat(underTest.transformCustomizationOptionToScreenForLogging(GRID))
            .isEqualTo(SCREEN_LAYOUT)
        assertThat(underTest.transformCustomizationOptionToScreenForLogging(CLOCK))
            .isEqualTo(SCREEN_CLOCK)
        assertThat(underTest.transformCustomizationOptionToScreenForLogging(SHORTCUTS))
            .isEqualTo(SCREEN_SHORTCUTS)
    }

    companion object {
        private const val TEST_WALLPAPER_COLLECTION_ID: String = "test_wallpaper_collections_id"
        private const val TEST_WALLPAPER_REMOTE_ID: String = "test_wallpaper_remote_id"
        private const val TEST_WALLPAPER_EFFECT: String = "test_wallpaper_effect"
        private const val TEST_LOCK_WALLPAPER_COLLECTION_ID: String =
            "test_lock_wallpaper_collections_id"
        private const val TEST_LOCK_WALLPAPER_REMOTE_ID: String = "test_lock_wallpaper_remote_id"
        private const val TEST_LOCK_WALLPAPER_EFFECT: String = "test_lock_wallpaper_effect"
        private const val TEST_CLOCK_ID: String = "text_clock_id"
        private const val TEST_SEED_COLOR: Int = Color.BLUE
        private const val TEST_SHORTCUT: String = "test_shortcut"
        private const val TEST_SHORTCUT_SLOT: String = "test_shortcut_slot"
        private const val TEST_SHAPE_ID: String = "test_shape_id"
    }
}
