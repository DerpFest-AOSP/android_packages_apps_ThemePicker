/*
 * Copyright (C) 2023 The LibreMobileOS Foundation
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

package com.android.customization.picker.udfps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.android.themepicker.R
import com.android.wallpaper.picker.AppbarFragment

class UdfpsAnimationFragment : AppbarFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): UdfpsAnimationFragment {
            return UdfpsAnimationFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val composeView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                UdfpsAnimationScreen()
            }
        }

        val rootView = inflater.inflate(R.layout.fragment_udfps_animation, container, false)
        val contentContainer: ViewGroup = rootView.findViewById(R.id.content_container)
        contentContainer.addView(composeView)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                topMargin = insets.top
                bottomMargin = insets.bottom
            }
            WindowInsetsCompat.CONSUMED
        }
        setUpToolbar(rootView)

        return rootView
    }

    override fun getDefaultTitle(): CharSequence {
        return getString(R.string.udfps_animation_title)
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(
            requireContext(),
            com.android.wallpaper.R.color.system_on_surface
        )
    }
}
