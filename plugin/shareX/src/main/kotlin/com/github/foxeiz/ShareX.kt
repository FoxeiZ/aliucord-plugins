package com.github.foxeiz

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.discord.api.activity.Activity
import com.discord.api.application.Application
import com.discord.api.message.MessageReference
import com.discord.api.message.activity.MessageActivity
import com.discord.api.message.allowedmentions.MessageAllowedMentions
import com.discord.models.user.User
import com.discord.stores.StoreMessages
import com.discord.utilities.captcha.CaptchaHelper
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.github.foxeiz.settings.CatboxSetting
import com.lytefast.flexinput.model.Attachment
import com.uploader.FileHostingService
import com.uploader.services.Catbox
import java.util.concurrent.Executors

@Suppress("unused")
@AliucordPlugin
class ShareX : Plugin() {

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings)
    }

    private val threadExecutor by lazy { Executors.newSingleThreadExecutor() }
    private val processQueue = mutableMapOf<String, () -> List<String>>()

    private lateinit var uploadProcessor: UploadProcessor

    private fun createUploadProvider(): FileHostingService {
        return when (settings.getString(PluginSettings.UPLOAD_SERVICE_KEY, "catbox-anon")) {
            PluginSettings.UploadProvider.CATBOX_USER.value -> Catbox(
                settings.getString(CatboxSetting.USER_HASH_KEY, null)
            )

            PluginSettings.UploadProvider.CATBOX_ANON.value -> Catbox(null)
            else -> Catbox(null)
        }
    }

    override fun start(ctx: Context) {
        val uploadProvider = createUploadProvider()
        uploadProcessor = UploadProcessor(ctx, uploadProvider, logger)

        val commandRegistry = CommandRegistry(commands)
        commandRegistry.registerDebugCommands()

        setupPatchers(ctx)
    }

    private fun setupPatchers(ctx: Context) {
        setupStoreMessagesPatcher(ctx)
        setupChatInputPatcher(ctx)
    }

    private fun setupStoreMessagesPatcher(ctx: Context) {
        patcher.before<StoreMessages>(
            "sendMessage",
            Long::class.javaPrimitiveType!!,
            User::class.java,
            String::class.java,
            List::class.java,
            List::class.java,
            List::class.java,
            MessageReference::class.java,
            MessageAllowedMentions::class.java,
            Application::class.java,
            Activity::class.java,
            MessageActivity::class.java,
            Long::class.javaObjectType,
            Long::class.javaObjectType,
            Integer::class.javaObjectType,
            CaptchaHelper.CaptchaPayload::class.java
        ) {
            if (settings.getBool(PluginSettings.DISABLED_KEY, false)) {
                return@before
            }

            @Suppress("UNCHECKED_CAST")
            val attachments = (it.args[3] as List<Attachment<*>>).toMutableList()
            val content = it.args[2] as String

            if (!content.endsWith("\uFFFF")) {
                logger.info("Skipping processing, content does not end with placeholder")
                return@before
            }

            // Remove placeholder
            it.args[2] = content.replace(
                Regex("""placeholder_\d+_\uffff$"""),
                ""
            )

            logger.info("${attachments.size}, processQueue size: ${processQueue.size}, content: $content")
            processQueue.remove(content)?.let { queue ->
                val result = queue()
                if (result.isEmpty()) {
                    Utils.showToast("No files uploaded", false)
                } else {
                    Utils.showToast("Uploaded ${result.size} files", false)
                    it.args[2] = "${it.args[2] as String}\n${result.joinToString(" ")}"
                }
                logger.info("processQueue size: ${processQueue.size}")
            }
        }
    }

    private fun setupChatInputPatcher(ctx: Context) {
        patcher.before<ChatInputViewModel>(
            "sendMessage",
            Context::class.java,
            MessageManager::class.java,
            MessageContent::class.java,
            List::class.java,
            Boolean::class.javaPrimitiveType!!,
            Function1::class.java
        ) {
            if (settings.getBool(PluginSettings.DISABLED_KEY, false)) {
                return@before
            }

            @Suppress("UNCHECKED_CAST")
            val attachments = (it.args[3] as List<Attachment<*>>).toMutableList()
            if (attachments.isEmpty()) {
                return@before
            }

            var messageContent = it.args[2] as MessageContent

            val largeAttachments = if (settings.getBool(PluginSettings.ALWAYS_UPLOAD_KEY, false)) {
                attachments.toMutableList()
            } else {
                attachments.filter { attachment ->
                    uploadProcessor.isLargeAttachment(attachment)
                }
            }

            if (largeAttachments.isEmpty()) {
                return@before
            }

            logger.info("Found ${largeAttachments.size} large attachments to upload, content: ${messageContent.textContent}")

            attachments.removeAll(largeAttachments)
            messageContent = messageContent.copy(
                messageContent.textContent + "placeholder_" + System.currentTimeMillis() + "_\uFFFF",
                messageContent.mentionedUsers
            )
            it.args[2] = messageContent
            it.args[3] = attachments.toList()

            processQueue[messageContent.textContent] = {
                uploadProcessor.processAttachments(
                    attachments = largeAttachments,
                    check = { context, attachment ->
                        uploadProcessor.isSupportedFile(attachment)
                    }
                )
            }
        }
    }

    override fun stop(ctx: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
        threadExecutor.shutdown()
    }
}