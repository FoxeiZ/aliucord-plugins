package com.github.foxeiz

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.aliucord.Logger
import com.aliucord.api.PatcherAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.patcher.after
import com.discord.models.user.User
import com.discord.utilities.streams.StreamContext
import com.discord.widgets.channels.WidgetChannelTopic
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemEmbed
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.user.presence.ModelRichPresence
import com.discord.widgets.user.presence.ViewHolderUserRichPresence
import com.discord.widgets.user.profile.UserProfileHeaderView
import com.discord.widgets.user.profile.UserProfileHeaderViewModel
import com.discord.widgets.user.usersheet.WidgetUserSheet
import com.discord.widgets.user.usersheet.WidgetUserSheetViewModel

private val logger by lazy { Logger("SelectableText") }

fun embedDescription(patcher: PatcherAPI, settings: SettingsAPI) {
    patcher.after<WidgetChatListAdapterItemEmbed>(
        "onConfigure", Int::class.javaPrimitiveType!!, ChatListEntry::class.java
    ) { param ->
        try {
            val adapterItem = param.thisObject as WidgetChatListAdapterItemEmbed
            val itemView = adapterItem.itemView

            SettingGroup.EMBEDS.settings.forEach { info ->
                if (settings.getBool(info.key, false)) {
                    if (info.resId == null && info.resIds != null) {
                        info.resIds.forEach { resId ->
                            applySelection(itemView, resId, recursive = true)
                        }
                    } else {
                        applySelection(itemView, info.resId ?: return@forEach, recursive = true)
                    }
                }
            }

        } catch (e: Throwable) {
            logger.error("Error making embed parts selectable", e)
        }
    }
}

fun richPresence(patcher: PatcherAPI, settings: SettingsAPI) {
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
            SettingGroup.RICH_PRESENCE.settings.forEach { info ->
                if (settings.getBool(info.key, false)) {
                    applySelection(viewHolder.root, info.resId ?: return@forEach)
                }
            }

        } catch (e: Throwable) {
            logger.error("Error making Rich Presence selectable", e)
        }
    }
}

fun userProfile(patcher: PatcherAPI, settings: SettingsAPI) {
    fun applyForHeader(info: SettingKeyInfo, methodName: String) {
        if (settings.getBool(info.key, false)) {
            patcher.after<UserProfileHeaderView>(
                methodName, UserProfileHeaderViewModel.ViewState.Loaded::class.java
            ) { param ->
                try {
                    val view = param.thisObject as UserProfileHeaderView
                    applySelection(view, info.resId ?: return@after)

                } catch (e: Throwable) {
                    logger.error("Error making user profile selectable", e)
                }
            }
        }
    }
    applyForHeader(SettingKeyInfo.USER_NAME, "configureSecondaryName")
    applyForHeader(SettingKeyInfo.USER_NICK_NAME, "configurePrimaryName")
    applyForHeader(SettingKeyInfo.USER_CUSTOM_STATUS, "updateViewState")

    if (settings.getBool(SettingKeyInfo.USER_BIO.key, false)) {
        patcher.after<WidgetUserSheet>(
            "configureAboutMe", WidgetUserSheetViewModel.ViewState.Loaded::class.java
        ) { param ->
            try {
                val widget = param.thisObject as WidgetUserSheet
                applySelection(widget.mView, SettingKeyInfo.USER_BIO.resId ?: return@after)

            } catch (e: Throwable) {
                logger.error("Error making user bio selectable", e)
            }
        }
    }
}

fun serverChannel(patcher: PatcherAPI, settings: SettingsAPI) {
    if (settings.getBool(SettingKeyInfo.CHANNEL_TOPIC.key, false)) {
        patcher.after<WidgetChannelTopic>(
            "configureChannelTopicTitle", WidgetChannelTopic.RenderedTopic::class.java
        ) { param ->
            try {
                val widget = param.thisObject as WidgetChannelTopic
                applySelection(widget.mView, SettingKeyInfo.CHANNEL_TOPIC.resId ?: return@after)

            } catch (e: Throwable) {
                logger.error("Error making channel topic selectable", e)
            }
        }
    }
}

private fun applySelection(view: View, resId: Int, recursive: Boolean = false) {
    if (resId == 0) {
        logger.warn("Resource ID is 0, skipping. view: ${view.javaClass.simpleName}, root: ${view.rootView.javaClass.simpleName}")
        return
    }
    val viewsToApply =
        if (recursive) findAllViewsById(view, resId) else listOfNotNull(findViewById(view, resId))
    viewsToApply.forEach { v ->
        if (v is TextView) {
            v.setTextIsSelectable(true)
            v.movementMethod = LinkMovementMethod.getInstance()
            v.setOnLongClickListener(null)  // remove default action, maybe this will be a setting later?
        }
    }
}

private fun findViewById(view: View, resId: Int): View? {
    if (view.id == resId) {
        return view
    }
    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            val found = findViewById(view.getChildAt(i), resId)
            if (found != null) {
                return found
            }
        }
    }
    return null
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
