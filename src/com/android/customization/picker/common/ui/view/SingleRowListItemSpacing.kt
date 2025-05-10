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
package com.android.customization.picker.common.ui.view

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Item spacing used by the horizontal RecyclerView with only 1 row.
 *
 * @param dividerIndex Index of the item that a divider will be drawn on the right.
 */
class SingleRowListItemSpacing(
    private val edgeItemSpacePx: Int,
    private val itemHorizontalSpacePx: Int,
    private val dividerIndex: Int = -1,
    private val dividerDrawable: Drawable? = null,
) : RecyclerView.ItemDecoration() {

    private val dividerWidth = dividerDrawable?.intrinsicWidth ?: 0
    private val dividerHeight = dividerDrawable?.intrinsicHeight ?: 0

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val itemIndex = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0
        val isRtl = parent.layoutManager?.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val isFirstItem = itemIndex == 0
        val startSpace = if (isFirstItem) edgeItemSpacePx else 0

        val isLastItem = itemIndex == itemCount - 1
        val isDivider = itemIndex == dividerIndex && dividerDrawable != null && dividerHeight > 0
        val itemSpaceWithDividerConsideration =
            if (isDivider) itemHorizontalSpacePx * 2 + dividerWidth else itemHorizontalSpacePx
        val endSpace = if (isLastItem) edgeItemSpacePx else itemSpaceWithDividerConsideration

        if (isRtl) {
            outRect.right = startSpace
            outRect.left = endSpace
        } else {
            outRect.left = startSpace
            outRect.right = endSpace
        }
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        if (dividerDrawable == null || dividerHeight <= 0) return

        val parentPaddedHeight = parent.height - parent.paddingTop - parent.paddingBottom
        val actualDrawableHeight = minOf(dividerHeight, parentPaddedHeight)
        val dividerTop = parent.paddingTop + (parentPaddedHeight - actualDrawableHeight) / 2
        val dividerBottom = dividerTop + actualDrawableHeight

        val isRtl = parent.layoutManager?.layoutDirection == View.LAYOUT_DIRECTION_RTL

        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val position = parent.getChildAdapterPosition(child)

            if (position == RecyclerView.NO_POSITION) continue

            if (position == dividerIndex) {
                val params = child.layoutParams as RecyclerView.LayoutParams
                val dividerLeft: Int
                val dividerRight: Int
                if (isRtl) {
                    dividerRight = child.left - params.leftMargin - itemHorizontalSpacePx
                    dividerLeft = dividerRight - dividerWidth
                } else {
                    dividerLeft = child.right + params.rightMargin + itemHorizontalSpacePx
                    dividerRight = dividerLeft + dividerWidth
                }
                dividerDrawable.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom)
                dividerDrawable.draw(c)
                // Break as we only want to draw one such divider.
                break
            }
        }
    }
}
