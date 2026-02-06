package com.github.foxeiz

import android.os.Bundle
import android.view.View
import com.aliucord.Utils
import com.aliucord.api.PatcherAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.widgets.BottomSheet
import com.discord.views.CheckedSetting

class PluginSettings(private val settings: SettingsAPI, private val patcher: PatcherAPI) :
    BottomSheet() {
    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)

        context?.let { ctx ->
            addView(
                Utils.createCheckedSetting(
                    ctx,
                    CheckedSetting.ViewType.SWITCH,
                    "Compatibility mode",
                    "If the plugin does not work, turn this on"
                ).apply {
                    isChecked = settings.getBool("compatibilityMode", false)
                    setOnCheckedListener {
                        settings.setBool("compatibilityMode", it)
                        patcher.unpatchAll()
                    }
                }
            )
        }
    }
}
