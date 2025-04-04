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

package com.android.wallpaper.customization.ui.viewmodel

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.test.filters.SmallTest
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.customization.picker.grid.domain.interactor.GridInteractor2
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class GridPickerViewModelTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var testScope: TestScope
    @Inject lateinit var gridOptionsManager: FakeShapeGridManager
    @Inject lateinit var interactor: GridInteractor2
    @Inject @ApplicationContext lateinit var appContext: Context

    private lateinit var underTest: GridPickerViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        underTest = GridPickerViewModel(appContext, interactor, testScope.backgroundScope)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectedGridOption() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedGridOption)

            assertGridItem(
                optionItem = selectedGridOption(),
                key = "normal",
                payload = gridOptionsManager.gridOptionDrawable0,
                text = Text.Loaded("5x5"),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
        }

    @Test
    fun selectedGridOption_shouldUpdate_afterOnApply() =
        testScope.runTest {
            val selectedGridOption = collectLastValue(underTest.selectedGridOption)
            val optionItems = collectLastValue(underTest.gridOptions)
            val onApply = collectLastValue(underTest.onApply)
            val onPracticalOptionClick =
                optionItems()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onPracticalOptionClick)

            onPracticalOptionClick()?.invoke()
            onApply()?.invoke()

            assertGridItem(
                optionItem = selectedGridOption(),
                key = "practical",
                payload = gridOptionsManager.gridOptionDrawable1,
                text = Text.Loaded("4x5"),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
        }

    @Test
    fun optionItems() =
        testScope.runTest {
            val optionItems = collectLastValue(underTest.gridOptions)

            assertGridItem(
                optionItem = optionItems()?.get(0),
                key = "normal",
                payload = gridOptionsManager.gridOptionDrawable0,
                text = Text.Loaded("5x5"),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
            assertGridItem(
                optionItem = optionItems()?.get(1),
                key = "practical",
                payload = gridOptionsManager.gridOptionDrawable1,
                text = Text.Loaded("4x5"),
                isTextUserVisible = true,
                isSelected = false,
                isEnabled = true,
            )
        }

    @Test
    fun optionItems_whenClickOnPracticalOption() =
        testScope.runTest {
            val optionItems = collectLastValue(underTest.gridOptions)
            val onPracticalOptionClick =
                optionItems()?.get(1)?.onClicked?.let { collectLastValue(it) }
            checkNotNull(onPracticalOptionClick)

            onPracticalOptionClick()?.invoke()

            assertGridItem(
                optionItem = optionItems()?.get(0),
                key = "normal",
                payload = gridOptionsManager.gridOptionDrawable0,
                text = Text.Loaded("5x5"),
                isTextUserVisible = true,
                isSelected = false,
                isEnabled = true,
            )
            assertGridItem(
                optionItem = optionItems()?.get(1),
                key = "practical",
                payload = gridOptionsManager.gridOptionDrawable1,
                text = Text.Loaded("4x5"),
                isTextUserVisible = true,
                isSelected = true,
                isEnabled = true,
            )
        }

    private fun TestScope.assertGridItem(
        optionItem: OptionItemViewModel2<Drawable>?,
        key: String,
        payload: Drawable?,
        text: Text,
        isTextUserVisible: Boolean,
        isSelected: Boolean,
        isEnabled: Boolean,
    ) {
        checkNotNull(optionItem)
        assertThat(collectLastValue(optionItem.key)()).isEqualTo(key)
        assertThat(optionItem.text).isEqualTo(text)
        assertThat(optionItem.payload).isEqualTo(payload)
        assertThat(optionItem.isTextUserVisible).isEqualTo(isTextUserVisible)
        assertThat(collectLastValue(optionItem.isSelected)()).isEqualTo(isSelected)
        assertThat(optionItem.isEnabled).isEqualTo(isEnabled)
    }
}
