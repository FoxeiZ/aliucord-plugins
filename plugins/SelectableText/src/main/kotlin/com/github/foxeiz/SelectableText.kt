package com.github.foxeiz

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin

@Suppress("unused")
@AliucordPlugin
class SelectableText : Plugin() {

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }

    override fun start(context: Context) {
        embedDescription(patcher, settings)
        richPresence(patcher, settings)
        userProfile(patcher, settings)
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
