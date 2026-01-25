/*
 * SPDX-FileCopyrightText: DerpFest AOSP
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.customization.model.udfps

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.view.LayoutInflater
import com.android.customization.picker.udfps.UdfpsIconFragment
import com.android.customization.picker.udfps.UdfpsIconSectionView
import com.android.themepicker.R
import com.android.wallpaper.model.CustomizationSectionController
import com.android.wallpaper.model.CustomizationSectionController.CustomizationSectionNavigationController

class UdfpsIconSectionController(
    private val sectionNavigationController: CustomizationSectionNavigationController
) : CustomizationSectionController<UdfpsIconSectionView> {

    override fun isAvailable(context: Context): Boolean {
        val enabled = try {
            context.resources.getBoolean(R.bool.config_show_udfps_icon_customization)
        } catch (e: Exception) {
            false
        }
        return enabled && isUdfpsAvailable(context) && isUdfpsIconPackageInstalled(context)
    }

    override fun createView(context: Context): UdfpsIconSectionView {
        val sectionView = LayoutInflater.from(context).inflate(
            R.layout.udfps_icon_section_view,
            /* root= */ null
        ) as UdfpsIconSectionView
        sectionView.setOnClickListener {
            launchUdfpsIconFragment()
        }
        return sectionView
    }

    private fun launchUdfpsIconFragment() {
        val fragment = UdfpsIconFragment.newInstance()
        sectionNavigationController.navigateTo(fragment)
    }

    private fun isUdfpsAvailable(context: Context): Boolean {
        return try {
            val array = context.resources.getIntArray(
                com.android.internal.R.array.config_udfps_sensor_props
            )
            if (array.isNotEmpty()) {
                true
            } else {
                val hasFingerprint = context.packageManager
                    .hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                if (hasFingerprint) {
                    try {
                        val fingerprintManager =
                            context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
                        val udfpsProps =
                            fingerprintManager.getSensorPropertiesInternal().filter { it.isAnyUdfpsType() }
                        udfpsProps.isNotEmpty()
                    } catch (e: Exception) {
                        false
                    }
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun isUdfpsIconPackageInstalled(context: Context): Boolean {
        val iconPackage = "org.derpfest.udfps.icons"
        return isPackageInstalled(context, iconPackage)
    }

    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0 /* flags */) != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
