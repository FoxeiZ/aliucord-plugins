package com.github.foxeiz

import android.content.Context
import android.view.View
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

enum class SettingKeyInfo(val key: String, val title: String, val description: String) {
    EMBED_DESCRIPTION(
        "embed_description",
        "Embed description",
        "Enable selectable text in embed descriptions"
    )
}

class PluginSettings(private val settings: SettingsAPI) :
    SettingsPage() {

    override fun onViewBound(view: View) {
        super.onViewBound(view)
        val padding = DimenUtils.defaultPadding

        setActionBarTitle("Selectable Text")
        // embed settings
        createHeader(context!!, "Embed")
        createPatcherEntry(
            context!!,
            SettingKeyInfo.EMBED_DESCRIPTION
        )
    }

    fun createPatcherEntry(
        ctx: Context,
        settingKey: SettingKeyInfo
    ) {
        createToggleButton(
            ctx,
            settingKey.key,
            settingKey.title,
            settingKey.description
        )
    }

    fun createHeader(ctx: Context, title: String): TextView {
        return TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = title
            addView(this)
        }
    }

    fun createToggleButton(
        ctx: Context,
        key: String,
        title: String = "",
        description: String = "",
        rootLayout: View? = null
    ): CheckedSetting {
        return Utils.createCheckedSetting(
            ctx, CheckedSetting.ViewType.SWITCH,
            title, description
        ).apply {
            isChecked = settings.getBool(key, false)
            setOnCheckedListener {
                settings.setBool(key, it)
                Utils.promptRestart()
            }

        }
    }
}
