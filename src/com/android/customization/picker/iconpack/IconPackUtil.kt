package com.android.customization.picker.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable

data class IconPackInfo(
    val packageName: String,
    val name: String,
    val icon: Drawable?,
)

object IconPackUtil {

    private val ICON_PACK_INTENTS = arrayOf(
        "com.novalauncher.THEME",
        "org.adw.launcher.icons.ACTION_PICK_ICON",
        "com.dlto.atom.launcher.THEME",
    )

    private const val META_DATA_THEMED = "org.derpfest.iconpack.THEMED"

    /** Bundled PUI pack. It stays immediately after the built-in Minimal style. */
    private const val PUI_ICON_PACK_PACKAGE = "com.android.theme.icon_pack.pui.apps"

    /**
     * Whether the pack is drawn in the themed icon palette, so it has to be applied together with
     * themed icons to match the apps and the search bar it does not cover.
     */
    fun isThemedIconPack(context: Context, packageName: String): Boolean {
        if (packageName.isEmpty()) return false
        return try {
            context.packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getBoolean(META_DATA_THEMED) == true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun getInstalledIconPacks(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val packs = mutableListOf<IconPackInfo>()

        packs.add(
            IconPackInfo(
                packageName = "",
                name = "System Icons",
                icon = context.packageManager.getApplicationIcon(context.packageName),
            )
        )

        val seenPackages = mutableSetOf<String>()

        for (action in ICON_PACK_INTENTS) {
            val intent = Intent(action)
            val activities: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)
            for (activity in activities) {
                val pkg = activity.activityInfo.packageName
                if (seenPackages.add(pkg)) {
                    val appInfo = activity.activityInfo.applicationInfo
                    packs.add(
                        IconPackInfo(
                            packageName = pkg,
                            name = appInfo.loadLabel(pm).toString(),
                            icon = appInfo.loadIcon(pm),
                        ),
                    )
                }
            }
        }

        val apexIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("com.anddoes.launcher.THEME")
        }
        val apexActivities: List<ResolveInfo> = pm.queryIntentActivities(apexIntent, 0)
        for (activity in apexActivities) {
            val pkg = activity.activityInfo.packageName
            if (seenPackages.add(pkg)) {
                val appInfo = activity.activityInfo.applicationInfo
                packs.add(
                    IconPackInfo(
                        packageName = pkg,
                        name = appInfo.loadLabel(pm).toString(),
                        icon = appInfo.loadIcon(pm),
                    ),
                )
            }
        }

        // Style list is Default, Minimal, then these packs. PUI is always first;
        // packs the user installed follow, in name order.
        return packs.sortedWith(
            compareBy<IconPackInfo> { if (it.packageName == PUI_ICON_PACK_PACKAGE) 0 else 1 }
                .thenBy { it.name.lowercase() }
        )
    }
}
