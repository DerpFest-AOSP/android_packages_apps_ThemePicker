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

package com.android.wallpaper.customization.ui.viewmodel

import android.content.ComponentName
import android.content.Intent
import com.android.customization.picker.pack.data.PackThemeData
import com.android.wallpaper.picker.domain.interactor.PackThemeInteractor
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ViewModelScoped
class PackThemeViewModel @Inject constructor(private val interactor: PackThemeInteractor) {
    val startThemePackActivityIntent: Flow<Intent?> =
        interactor.packThemeData.map { data ->
            if (
                data.launchingPackageName.isNotEmpty() &&
                    data.launchingDetailActivityClass.isNotEmpty()
            ) {
                val componentName =
                    ComponentName(data.launchingPackageName, data.launchingDetailActivityClass)
                Intent().apply { component = componentName }
            } else {
                null
            }
        }
    val startSuggestedThemePackActivityIntent: Flow<Intent?> =
        interactor.packThemeData.map { data ->
            if (
                data.launchingPackageName.isNotEmpty() &&
                    data.launchingDetailActivityClass.isNotEmpty()
            ) {
                val componentName =
                    ComponentName(data.launchingPackageName, data.launchingDetailActivityClass)
                Intent().apply {
                    component = componentName
                    putExtra(THEME_ID, data.suggestedChipThemePackInfo.themeId)
                }
            } else {
                null
            }
        }
    val packThemeData: Flow<PackThemeData> = interactor.packThemeData

    private companion object {
        const val THEME_ID = "themeId"
    }
}
