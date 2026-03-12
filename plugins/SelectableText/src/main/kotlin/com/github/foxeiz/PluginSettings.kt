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
    val resId: Int? = null,
    val resIds: List<Int>? = null,
) {
    EMBED_TITLE(
        "embed_title",
        "Embed Title",
        "",
        Utils.getResId("chat_list_item_embed_title", "id")
    ),
    EMBED_AUTHOR(
        "embed_author",
        "Embed Author",
        "",
        Utils.getResId("chat_list_item_embed_author_text", "id")
    ),
    EMBED_DESCRIPTION(
        "embed_description",
        "Embed Description",
        "",
        Utils.getResId("chat_list_item_embed_description", "id")
    ),
    EMBED_FOOTER(
        "embed_footer",
        "Embed Footer",
        "",
        Utils.getResId("chat_list_item_embed_footer_text", "id")
    ),
    EMBED_FIELDS(
        "embed_fields",
        "Embed Fields",
        "Includes both field name and value, since they use the same layout.",
        resIds = listOf(
            Utils.getResId("chat_list_item_embed_field_name", "id"),
            Utils.getResId("chat_list_item_embed_field_value", "id")
        )
    ),
    RICH_PRESENCE_STATE(
        "rich_presence_state",
        "Rich Presence State",
        "",
        Utils.getResId("rich_presence_state", "id")
    ),
    RICH_PRESENCE_TITLE(
        "rich_presence_title",
        "Rich Presence Title",
        "",
        Utils.getResId("rich_presence_title", "id")
    ),
    RICH_PRESENCE_DETAILS(
        "rich_presence_details",
        "Rich Presence Details",
        "",
        Utils.getResId("rich_presence_details", "id")
    ),
    RICH_PRESENCE_TIME(
        "rich_presence_time",
        "Rich Presence Time",
        "Very unstable, since time is updated every second, causing the selection to reset.",
        Utils.getResId("rich_presence_time", "id")
    ),
    USER_NICK_NAME(
        "user_nick_name",
        "User Nickname",
        "",
        Utils.getResId("username_text", "id")
    ),
    USER_NAME(
        "user_name",
        "User Name",
        "The actual username, not the nickname.",
        Utils.getResId("user_profile_header_secondary_name", "id")
    ),
    USER_CUSTOM_STATUS(
        "user_custom_status",
        "User Custom Status",
        "",
        Utils.getResId("user_profile_header_custom_status", "id")
    ),
    USER_BIO(
        "user_bio",
        "User Bio",
        "",
        Utils.getResId("about_me_text", "id")
    ),
    CHANNEL_TOPIC(
        "channel_topic",
        "Channel Topic",
        "",
        Utils.getResId("channel_topic_title", "id")
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
            SettingKeyInfo.USER_NICK_NAME,
            SettingKeyInfo.USER_NAME,
            SettingKeyInfo.USER_CUSTOM_STATUS,
            SettingKeyInfo.USER_BIO
        )
    ),
    CHANNEL(
        "Channel Settings", listOf(
            SettingKeyInfo.CHANNEL_TOPIC
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
