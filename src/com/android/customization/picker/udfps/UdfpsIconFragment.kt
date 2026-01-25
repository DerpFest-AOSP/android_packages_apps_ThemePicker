/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
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

class UdfpsIconFragment : AppbarFragment() {

    companion object {
        @JvmStatic
        fun newInstance(): UdfpsIconFragment {
            return UdfpsIconFragment()
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
                UdfpsIconScreen()
            }
        }

        val rootView = inflater.inflate(R.layout.fragment_udfps_icon, container, false)
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
        return getString(R.string.udfps_icon_title)
    }

    override fun getToolbarTextColor(): Int {
        return ContextCompat.getColor(
            requireContext(),
            com.android.wallpaper.R.color.system_on_surface
        )
    }
}
