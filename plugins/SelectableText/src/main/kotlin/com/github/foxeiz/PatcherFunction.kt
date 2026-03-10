package com.github.foxeiz

import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.PatcherAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.patcher.after
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.discord.widgets.chat.list.entries.ChatListEntry

fun createSelectableEmbedDescription(patcher: PatcherAPI, settings: SettingsAPI) {
    patcher.after<WidgetChatListAdapterItemEmbed>(
        "onConfigure",
        Int::class.javaPrimitiveType!!,
        ChatListEntry::class.java
    ) { param ->
        try {
            val adapterItem = param.thisObject as WidgetChatListAdapterItemEmbed
            val itemView = adapterItem.itemView

            fun applySelection(resName: String) {
                val resId = Utils.getResId(resName, "id")
                if (resId != 0) {
                    findAllViewsById(itemView, resId).forEach { view ->
                        if (view is TextView) {
                            view.setTextIsSelectable(true)
                            view.movementMethod = LinkMovementMethod.getInstance()
                            view.setOnLongClickListener(null)  // remove default action, maybe this will be a setting later?
                        }
                    }
                }
            }

            SettingKeyInfo.entries.forEach { info ->
                if (settings.getBool(info.key, false)) {
                    if (info == SettingKeyInfo.EMBED_FIELDS) {
                        applySelection("chat_list_item_embed_field_name")
                        applySelection("chat_list_item_embed_field_value")
                    } else {
                        applySelection(info.resName)
                    }
                }
            }

        } catch (e: Throwable) {
            patcher.logger.error("Error making embed parts selectable", e)
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
