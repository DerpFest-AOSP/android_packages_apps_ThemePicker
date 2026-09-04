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

package com.android.customization.picker.icon.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.AdaptiveIconDrawable
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.icon.shared.model.IconPackStyleModel
import com.android.customization.picker.icon.shared.model.IconStyle
import com.android.customization.picker.icon.shared.model.IconStyleModel
import com.android.customization.picker.icon.shared.model.ThemePickerIconStyle
import com.android.customization.picker.icon.ui.binder.ShapeIconViewBinder
import com.android.customization.picker.icon.ui.view.ShapeTileDrawable
import com.android.customization.picker.icon.ui.viewmodel.ShapeIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel2
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import kotlinx.coroutines.DisposableHandle

@ActivityScoped
class ThemePickerIconStyleViewUtil
@Inject
constructor(@ApplicationContext private val context: Context) : IconStyleViewUtil {
    override fun getOnClick(iconStyle: IconStyle): (() -> Unit)? {
        return null
    }

    override fun bindIconOptionView(
        view: View,
        iconStyleModel: IconStyleModel,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        val icon = getIcon(iconStyleModel)
        val foregroundView = view.requireViewById<ImageView>(R.id.app_icon)
        IconViewBinder.bind(foregroundView, icon)
        return bindIconColors(
            iconStyleModel,
            icon,
            colorUpdateViewModel,
            shouldAnimateColor,
            lifecycleOwner,
        )
    }

    override fun bindShapeIconPreview(
        view: View,
        iconStyleModel: IconStyleModel?,
        shapeIcon: ShapeIconViewModel?,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        val optionIcon = view.requireViewById<ImageView>(R.id.option_entry_icon)
        val icon = iconStyleModel?.let { getIcon(it, shapeIcon?.path) }
        icon?.let { IconViewBinder.bind(optionIcon, icon) }
        return if (iconStyleModel != null && icon != null) {
            bindIconColors(
                iconStyleModel,
                icon,
                colorUpdateViewModel,
                shouldAnimateColor,
                lifecycleOwner,
            )
        } else null
    }

    override fun bindIconColors(
        iconStyleModel: IconStyleModel,
        icon: Icon,
        colorUpdateViewModel: ColorUpdateViewModel,
        shouldAnimateColor: () -> Boolean,
        lifecycleOwner: LifecycleOwner,
    ): DisposableHandle? {
        var disposableHandle: DisposableHandle? = null
        // If the icon is a themed icon, bind its foreground and background color
        if (iconStyleModel.iconStyle == ThemePickerIconStyle.MONOCHROME) {
            ((icon as? Icon.Loaded)?.drawable as? ShapeTileDrawable)?.let {
                disposableHandle =
                    ShapeIconViewBinder.bindPreviewIconColor(
                        shapeTileDrawable = it,
                        colorUpdateViewModel = colorUpdateViewModel,
                        shouldAnimateColor = shouldAnimateColor,
                        lifecycleOwner = lifecycleOwner,
                    )
            }
        }
        return disposableHandle
    }

    override fun getIcon(iconStyleModel: IconStyleModel?, shapePath: String?): Icon {
        if (iconStyleModel is IconPackStyleModel && iconStyleModel.packIcon != null) {
            return Icon.Loaded(drawable = iconStyleModel.packIcon, contentDescription = null)
        }
        return Icon.Loaded(
            drawable =
                ShapeTileDrawable(
                    context = context,
                    path = shapePath,
                    // A fresh instance per option, they set their own bounds on it.
                    icon = previewIconPackage?.let(::loadAdaptiveIcon),
                    isThemed = iconStyleModel?.iconStyle == ThemePickerIconStyle.MONOCHROME,
                ),
            contentDescription = null,
        )
    }

    /**
     * App whose icon is drawn as an example on the options that don't bring their own icon.
     * [R.string.camera_package] is not installed on every device, and an option without an adaptive
     * icon renders as an empty tile, so fall back to any camera app that is installed. The
     * monochrome option only draws the monochrome layer, so prefer an icon that has one.
     */
    private val previewIconPackage: String? by lazy {
        val icons =
            previewIconCandidates().mapNotNull { pkg -> loadAdaptiveIcon(pkg)?.let { pkg to it } }
        (icons.firstOrNull { (_, icon) -> icon.monochrome != null } ?: icons.firstOrNull())?.first
    }

    private fun previewIconCandidates(): Set<String> = buildSet {
        add(context.resources.getString(R.string.camera_package))
        context.packageManager
            .queryIntentActivities(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), 0)
            .mapTo(this) { it.activityInfo.packageName }
        add(SETTINGS_PACKAGE)
    }

    private fun loadAdaptiveIcon(packageName: String) =
        ShapeIconViewBinder.loadAppIcon(context, packageName) as? AdaptiveIconDrawable

    override fun bindListDivider(
        options: List<OptionItemViewModel2<IconStyleModel>>,
        list: RecyclerView,
        optionIconHeightPx: Int?,
    ) {}

    companion object {
        private const val SETTINGS_PACKAGE = "com.android.settings"
    }
}
