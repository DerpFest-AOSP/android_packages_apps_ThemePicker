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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import androidx.core.view.isVisible

object FloatingSheetHeightAnimationBinder {
    private const val ANIMATION_DURATION = 200L

    fun bind(
        floatingSheetContainer: View,
        fromHeight: Int,
        toHeight: Int,
        fromContent: View? = null,
        toContent: View? = null,
    ) {
        ValueAnimator.ofInt(fromHeight, toHeight)
            .apply {
                addUpdateListener { valueAnimator ->
                    val value = valueAnimator.animatedValue as Int
                    floatingSheetContainer.layoutParams =
                        floatingSheetContainer.layoutParams.apply { height = value }
                    if (fromContent != null && toContent != null) {
                        fromContent.alpha = getAlpha(fromHeight, toHeight, value)
                    }
                }
                duration = ANIMATION_DURATION
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            fromContent?.isVisible = false
                            fromContent?.alpha = 1f
                            toContent?.isVisible = true
                            toContent?.alpha = 1f
                        }
                    }
                )
            }
            .start()
    }

    // Alpha is 1 when current height is from height, and 0 when current height is to height.
    private fun getAlpha(fromHeight: Int, toHeight: Int, currentHeight: Int): Float =
        (1 - (currentHeight - fromHeight).toFloat() / (toHeight - fromHeight).toFloat())
}
