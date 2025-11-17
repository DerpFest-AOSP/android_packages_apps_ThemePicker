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

package com.android.customization.picker.grid.data.repository

import android.content.Context
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import com.android.customization.model.grid.FakeShapeGridManager
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
class ShapeRepositoryTest {

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var shapeManager: FakeShapeGridManager
    @Inject lateinit var testScope: TestScope
    @BackgroundDispatcher @Inject lateinit var bgScope: CoroutineScope
    @BackgroundDispatcher @Inject lateinit var bgDispatcher: CoroutineDispatcher

    private lateinit var context: Context

    private lateinit var underTest: ShapeRepository

    @Before
    fun setUp() {
        hiltRule.inject()
        context = InstrumentationRegistry.getInstrumentation().targetContext
        underTest =
            ShapeRepository(
                context = context,
                shapeGridManager = shapeManager,
                bgDispatcher = bgDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun shapeOptions_default() =
        testScope.runTest {
            val shapeOptions = collectLastValue(underTest.shapeOptions)

            assertThat(shapeOptions()).isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST)
        }

    @Test
    fun selectedShapeOption_default() =
        testScope.runTest {
            val selectedShapeOption = collectLastValue(underTest.selectedShapeOption)

            assertThat(selectedShapeOption())
                .isEqualTo(FakeShapeGridManager.DEFAULT_SHAPE_OPTION_LIST[0])
        }
}
