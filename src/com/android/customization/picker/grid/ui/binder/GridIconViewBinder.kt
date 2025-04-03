package com.android.customization.picker.grid.ui.binder

import android.widget.ImageView
import com.android.customization.picker.grid.ui.viewmodel.GridIconViewModel
import com.android.customization.widget.GridTileDrawable

object GridIconViewBinder {
    fun bind(view: ImageView, viewModel: GridIconViewModel) {
        view.setImageDrawable(
            viewModel.drawable
                ?: GridTileDrawable(viewModel.columns, viewModel.rows, viewModel.path)
        )
    }
}
