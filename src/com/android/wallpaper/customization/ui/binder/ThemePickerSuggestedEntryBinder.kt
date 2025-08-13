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

package com.android.wallpaper.customization.ui.binder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.View
import android.widget.Toast
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.ColorUpdateBinder
import com.android.wallpaper.picker.customization.ui.binder.PackThemeSuggestedEntryBinder
import com.android.wallpaper.picker.customization.ui.view.PackThemeSuggestedChip
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Singleton
class ThemePickerSuggestedEntryBinder @Inject constructor() : PackThemeSuggestedEntryBinder {
    override fun bind(
        view: PackThemeSuggestedChip,
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
        navigateToPackThemeActivity: (Intent) -> Unit,
    ) {
        val isOnMainScreen = {
            viewModel.customizationOptionsViewModel.selectedOption.value == null
        }

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel

        val backgroundScope =
            CoroutineScope(Dispatchers.IO + Job() + CoroutineName(BACKGROUND_CONTEXT))
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.packThemeViewModel.startSuggestedThemePackActivityIntent
                        .collect { intent ->
                            if (intent != null) {
                                view.suggestedChip.setOnClickListener {
                                    backgroundScope.launch {
                                        if (isActivityAvailable(view.context, intent)) {
                                            navigateToPackThemeActivity.invoke(intent)
                                        } else {
                                            showNoPackThemeIntentErrorMessage(
                                                lifecycleOwner,
                                                view,
                                                optionsViewModel,
                                            )
                                        }
                                    }
                                }
                            } else {
                                view.suggestedChip.setOnClickListener { null }
                            }
                        }
                }
                launch {
                    optionsViewModel.packThemeViewModel.packThemeData.collect { packThemeData ->
                        if (packThemeData.suggestedChipThemePackInfo.title.isNotEmpty()) {
                            view.visibility = View.VISIBLE
                            view.hideSuggestedChip = false
                        } else {
                            view.visibility = View.GONE
                            view.hideSuggestedChip = true
                        }
                        view.suggestedChipText.text = packThemeData.suggestedChipThemePackInfo.title
                    }
                }
            }
        }

        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(view.suggestedChip.background), color)
            },
            color = colorUpdateViewModel.colorSecondaryContainer,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )
        ColorUpdateBinder.bind(
            setColor = { color ->
                DrawableCompat.setTint(DrawableCompat.wrap(view.cancelButton.background), color)
                DrawableCompat.setTint(DrawableCompat.wrap(view.icon.background), color)
                view.suggestedChipText.setTextColor(color)
            },
            color = colorUpdateViewModel.colorOnSecondaryContainer,
            shouldAnimate = isOnMainScreen,
            lifecycleOwner = lifecycleOwner,
        )
    }

    private suspend fun isActivityAvailable(context: Context, intent: Intent): Boolean {
        val packageManager: PackageManager = context.packageManager
        val activities =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return activities.isNotEmpty()
    }

    private fun showNoPackThemeIntentErrorMessage(
        lifecycleOwner: LifecycleOwner,
        view: View,
        optionsViewModel: ThemePickerCustomizationOptionsViewModel,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            Toast.makeText(
                    view.context,
                    optionsViewModel.packThemeViewModel.noAppErrorMessage,
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }

    companion object {
        private const val BACKGROUND_CONTEXT = "backgroundContext"
    }
}
