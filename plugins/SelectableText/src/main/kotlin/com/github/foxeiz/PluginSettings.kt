package com.github.foxeiz

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.discord.views.CheckedSetting
import com.lytefast.flexinput.R

enum class SettingKeyInfo(
    val key: String,
    val title: String,
    val description: String,
    val resName: String
) {
    EMBED_TITLE(
        "embed_title",
        "Embed Title",
        "Enable selectable text in embed titles",
        "chat_list_item_embed_title"
    ),
    EMBED_AUTHOR(
        "embed_author",
        "Embed Author",
        "Enable selectable text for embed authors",
        "chat_list_item_embed_author_text"
    ),
    EMBED_DESCRIPTION(
        "embed_description",
        "Embed Description",
        "Enable selectable text in embed descriptions",
        "chat_list_item_embed_description"
    ),
    EMBED_FOOTER(
        "embed_footer",
        "Embed Footer",
        "Enable selectable text in embed footers",
        "chat_list_item_embed_footer_text"
    ),
    EMBED_FIELDS(
        "embed_fields",
        "Embed Fields",
        "Enable selectable text in embed field names and values",
        ""
    )
}

class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Selectable Text")
        createHeader(context!!, "Embed Settings")
        SettingKeyInfo.entries.forEach { info ->
            createPatcherEntry(context!!, info)
        }
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
        ).let { addView(it) }
    }

    fun createHeader(ctx: Context, title: String): TextView {
        return TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = title
            this@PluginSettings.addView(this)
        }
    }

    fun createToggleButton(
        ctx: Context,
        key: String,
        title: String = "",
        description: String = "",
        rootLayout: ViewGroup? = null
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
