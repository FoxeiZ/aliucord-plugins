package com.github.foxeiz

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.lytefast.flexinput.model.Attachment
import com.uploader.FileHostingService
import com.uploader.services.Catbox

@AliucordPlugin
class ShareX() : Plugin() {

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }

    private fun createUploadProvider(): FileHostingService {
        return when (settings.getString("uploadService", "catbox")) {
            PluginSettings.UploadProvider.CATBOX_USER.value -> Catbox(
                settings.getString(
                    "userHash",
                    null
                )
            )

            PluginSettings.UploadProvider.CATBOX_ANON.value -> Catbox(null)
            else -> Catbox(null)
        }
    }

    val uploadProvider: FileHostingService by lazy {
        createUploadProvider()
    }

    override fun start(ctx: Context) {
        patcher.before<ChatInputViewModel>(
            "sendMessage",
            Context::class.java,
            MessageManager::class.java,
            MessageContent::class.java,
            List::class.java,
            Boolean::class.javaPrimitiveType!!,
            Function1::class.java
        ) {
            it.args[0] as Context
            val content = it.args[2] as MessageContent
            content.textContent
            val attachments = (it.args[3] as List<Attachment<*>>).toMutableList()

            if (attachments.isEmpty()  // no attachments
                || settings.getBool("disabled", false)  // plugin is disabled
            ) {
                return@before
            }
        }
    }

    override fun stop(p0: Context?) {
        TODO("Not yet implemented")
    }
}