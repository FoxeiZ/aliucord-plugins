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
        "",
        "chat_list_item_embed_title"
    ),
    EMBED_AUTHOR(
        "embed_author",
        "Embed Author",
        "",
        "chat_list_item_embed_author_text"
    ),
    EMBED_DESCRIPTION(
        "embed_description",
        "Embed Description",
        "",
        "chat_list_item_embed_description"
    ),
    EMBED_FOOTER(
        "embed_footer",
        "Embed Footer",
        "",
        "chat_list_item_embed_footer_text"
    ),
    EMBED_FIELDS(
        "embed_fields",
        "Embed Fields",
        "Includes both field name and value, since they use the same layout.",
        ""
    ),
    RICH_PRESENCE_STATE(
        "rich_presence_state",
        "Rich Presence State",
        "",
        "rich_presence_state"
    ),
    RICH_PRESENCE_TITLE(
        "rich_presence_title",
        "Rich Presence Title",
        "",
        "rich_presence_title"
    ),
    RICH_PRESENCE_DETAILS(
        "rich_presence_details",
        "Rich Presence Details",
        "",
        "rich_presence_details"
    ),
    RICH_PRESENCE_TIME(
        "rich_presence_time",
        "Rich Presence Time",
        "Very unstable, since time is updated every second, causing the selection to reset.",
        "rich_presence_time"
    ),
    USER_NAME(
        "user_name",
        "User Name",
        "",
        "user_name"
    ),
    USER_STATUS(
        "user_status",
        "User Status",
        "",
        "user_status"
    ),
    USER_BIO(
        "user_bio",
        "User Bio",
        "",
        "user_bio"
    )
}

enum class SettingGroup(val title: String, val settings: List<SettingKeyInfo>) {
    EMBEDS(
        "Embed Settings", listOf(
            SettingKeyInfo.EMBED_TITLE,
            SettingKeyInfo.EMBED_AUTHOR,
            SettingKeyInfo.EMBED_DESCRIPTION,
            SettingKeyInfo.EMBED_FOOTER,
            SettingKeyInfo.EMBED_FIELDS
        )
    ),
    RICH_PRESENCE(
        "Rich Presence Settings", listOf(
            SettingKeyInfo.RICH_PRESENCE_STATE,
            SettingKeyInfo.RICH_PRESENCE_TITLE,
            SettingKeyInfo.RICH_PRESENCE_DETAILS,
            SettingKeyInfo.RICH_PRESENCE_TIME
        )
    ),
    USER(
        "User Settings", listOf(
            SettingKeyInfo.USER_NAME,
            SettingKeyInfo.USER_STATUS,
            SettingKeyInfo.USER_BIO
        )
    )
}

class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {

    override fun onViewBound(view: View) {
        super.onViewBound(view)

        setActionBarTitle("Selectable Text")
        SettingGroup.entries.forEach { group ->
            if (group.settings.isNotEmpty()) {
                createHeader(context!!, group.title)
                group.settings.forEach { info ->
                    createPatcherEntry(context!!, info)
                }
            }
        }
    }

    fun createPatcherEntry(
        ctx: Context,
        settingKey: SettingKeyInfo,
        rootLayout: ViewGroup? = null
    ) {
        createToggleButton(
            ctx,
            settingKey.key,
            settingKey.title,
            settingKey.description
        ).let {
            (rootLayout ?: this.linearLayout).addView(it)
        }
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
