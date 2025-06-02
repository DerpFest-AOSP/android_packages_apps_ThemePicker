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

package com.android.customization.picker.icon.data.repository

import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.customization.picker.icon.shared.model.ThemePickerIconStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

@Singleton
class FakeIconStyleRepository @Inject constructor() : IconStyleRepository {
    private val _isThemedIconAvailable = MutableStateFlow(true)
    override val isThemedIconAvailable = _isThemedIconAvailable.asStateFlow()

    private val _isThemedIconActivated = MutableStateFlow(false)
    override val isThemedIconActivated = _isThemedIconActivated.asStateFlow()

    override val iconStyles: Flow<List<IconStyle>> =
        isThemedIconAvailable.map { isThemedIconAvailable ->
            var styles = ThemePickerIconStyle.entries.toList()
            if (!isThemedIconAvailable) styles = styles.filter { !it.getIsThemedIcon() }
            styles
        }

    override val selectedIconStyle =
        isThemedIconActivated.map {
            when (it) {
                true -> ThemePickerIconStyle.MONOCHROME
                false -> ThemePickerIconStyle.DEFAULT
            }
        }

    override suspend fun setThemedIconEnabled(enabled: Boolean) {
        _isThemedIconActivated.value = enabled
    }

    fun setIsThemedIconAvailable(isAvailable: Boolean) {
        _isThemedIconAvailable.value = isAvailable
    }
}
