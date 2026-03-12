package com.github.foxeiz

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.api.PatcherAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.patcher.after
import com.discord.models.user.User
import com.discord.utilities.streams.StreamContext
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.user.presence.ModelRichPresence
import com.discord.widgets.user.presence.ViewHolderUserRichPresence

private val logger by lazy { Logger("SelectableText") }

fun createSelectableEmbedDescription(patcher: PatcherAPI, settings: SettingsAPI) {
    patcher.after<WidgetChatListAdapterItemEmbed>(
        "onConfigure",
        Int::class.javaPrimitiveType!!,
        ChatListEntry::class.java
    ) { param ->
        try {
            val adapterItem = param.thisObject as WidgetChatListAdapterItemEmbed
            val itemView = adapterItem.itemView

            SettingGroup.EMBEDS.settings.forEach { info ->
                if (settings.getBool(info.key, false)) {
                    if (info == SettingKeyInfo.EMBED_FIELDS) {
                        applySelection(itemView, "chat_list_item_embed_field_name")
                        applySelection(itemView, "chat_list_item_embed_field_value")
                    } else {
                        applySelection(itemView, info.resName)
                    }
                }
            }

        } catch (e: Throwable) {
            logger.error("Error making embed parts selectable", e)
        }
    }
}

fun createSelectableRichPresence(patcher: PatcherAPI, settings: SettingsAPI) {
    patcher.after<ViewHolderUserRichPresence>(
        "configureUi",
        FragmentManager::class.java,
        StreamContext::class.java,
        Boolean::class.javaPrimitiveType!!,
        User::class.java,
        Context::class.java,
        ModelRichPresence::class.java,
        Boolean::class.javaPrimitiveType!!
    ) { param ->
        try {
            val viewHolder = param.thisObject as ViewHolderUserRichPresence
            logger.error(viewHolder.root.toString(), null)

            SettingGroup.RICH_PRESENCE.settings.forEach { info ->
                if (settings.getBool(info.key, false)) {
                    applySelection(viewHolder.root, info.resName)
                }
            }

        } catch (e: Throwable) {
            logger.error("Error making Rich Presence selectable", e)
        }
    }
}

private fun applySelection(view: View, resName: String) {
    val resId = Utils.getResId(resName, "id")
    if (resId != 0) {
        findAllViewsById(view, resId).forEach { view ->
            if (view is TextView) {
                view.setTextIsSelectable(true)
                view.movementMethod = LinkMovementMethod.getInstance()
                view.setOnLongClickListener(null)  // remove default action, maybe this will be a setting later?
            }
        }
    }
}

private fun findAllViewsById(view: View, resId: Int): List<View> {
    val views = mutableListOf<View>()
    if (view.id == resId) {
        views.add(view)
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            views.addAll(findAllViewsById(view.getChildAt(i), resId))
        }
    }
    return views
}
