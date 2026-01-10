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

    override fun start(ctx: Context) {
        requiresRestart()
        if (settings.getBool(SettingKeyInfo.EMBED_DESCRIPTION.key, false)) {
            createSelectableEmbedDescription(patcher)
        }
    }

    override fun stop(ctx: Context) {
        patcher.unpatchAll()
    }

}