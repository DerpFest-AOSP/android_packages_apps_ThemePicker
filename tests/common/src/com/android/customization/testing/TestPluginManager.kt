package com.android.customization.testing

import com.android.systemui.plugins.Plugin
import com.android.systemui.plugins.PluginListener
import com.android.systemui.plugins.PluginManager

class TestPluginManager : PluginManager {
    override val config = PluginManager.Config()

    override fun <T : Plugin> addPluginListener(
        listener: PluginListener<T>,
        cls: Class<T>,
        allowMultiple: Boolean,
    ) {}

    override fun removePluginListener(listener: PluginListener<*>) {}

    override fun <T> dependsOn(p: Plugin, cls: Class<T>): Boolean = false
}
