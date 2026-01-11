package com.github.foxeiz

import android.content.Context
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.before
import com.aliucord.utils.ReflectUtils
import com.discord.api.message.MessageFlags
import com.discord.api.message.MessageTypes
import com.discord.models.message.Message
import com.discord.stores.StoreMessages
import com.discord.stores.StoreStream
import com.discord.utilities.message.LocalMessageCreatorsKt
import com.discord.utilities.time.ClockFactory
import com.discord.widgets.chat.MessageContent
import com.discord.widgets.chat.MessageManager
import com.discord.widgets.chat.input.ChatInputViewModel
import com.github.foxeiz.settings.CatboxSettings
import com.github.foxeiz.settings.LitterboxSettings
import com.github.foxeiz.settings.PomfSettings
import com.lytefast.flexinput.model.Attachment
import com.uploader.FileHostingService
import com.uploader.services.Catbox
import com.uploader.services.Litterbox
import com.uploader.services.Pomf
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("unused")
@AliucordPlugin
class BoxUpload : Plugin() {

    init {
        settingsTab = SettingsTab(PluginSettings::class.java).withArgs(settings, commands)
    }

    private object LargeFileHandoff {
        val uploadQueue = ConcurrentLinkedQueue<UploadPayload>()
    }

    private data class UploadPayload(
        val files: List<Attachment<*>>,
        val originalText: String,
        val willBeEmpty: Boolean
    )

    private lateinit var uploadProcessor: UploadProcessor

    private fun createUploadProvider(): FileHostingService {
        return when (settings.getString(PluginSettings.UPLOAD_SERVICE_KEY, "catbox-anon")) {
            PluginSettings.UploadProvider.CATBOX_USER -> Catbox(
                settings.getString(CatboxSettings.USER_HASH_KEY, null)
            )

            PluginSettings.UploadProvider.CATBOX_ANON -> Catbox(null)
            PluginSettings.UploadProvider.LITTERBOX -> Litterbox(
                settings.getInt(LitterboxSettings.TIME_KEY, 1)
            )

            PluginSettings.UploadProvider.POMF -> Pomf(
                settings.getObject(
                    PomfSettings.POMF_CONFIG_KEY, Pomf.ServerConfig.default()
                )
            )

            else -> Catbox(null)
        }
    }

    override fun start(context: Context) {
        uploadProcessor = UploadProcessor(context, ::createUploadProvider, logger)
        val commandRegistry = CommandRegistry(commands)
        commandRegistry.registerDebugCommands()

        setupPatchers(context)
    }

    private fun setupPatchers(ctx: Context) {
        setupChatInputPatcher(ctx)
    }

    fun updateProgressMessage(storeMessages: StoreMessages, message: Message, text: String) {
        Utils.mainThread.post {
            try {
                ReflectUtils.setField(Message::class.java, message, "content", text)
                StoreMessages.`access$handleLocalMessageCreate`(storeMessages, message)
            } catch (e: Exception) {
                logger.error("Failed to update progress", e)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setupChatInputPatcher(ctx: Context) {
        patcher.before<ChatInputViewModel>(
            "sendMessage",
            Context::class.java,
            MessageManager::class.java,
            MessageContent::class.java,
            List::class.java,
            Boolean::class.javaPrimitiveType!!,
            Function1::class.java
        ) { param ->
            try {
                if (settings.getBool(PluginSettings.DISABLED_KEY, false)) {
                    return@before
                }

                val attachments = (param.args[3] as List<Attachment<*>>).toMutableList()
                val largeFiles = if (settings.getBool(PluginSettings.ALWAYS_UPLOAD_KEY, false)) {
                    attachments.toList()
                } else {
                    attachments.filter { uploadProcessor.isLargeAttachment(it) }
                }

                if (largeFiles.isEmpty()) return@before

                // stop the original method
                param.result = null

                val context = param.args[0] as Context
                val messageManager = param.args[1] as MessageManager
                val originalContent = param.args[2] as MessageContent
                val compressedImages = param.args[4] as Boolean
                val onValidation = param.args[5]
                val viewModel = param.thisObject

                handleLargeFileUpload(
                    context,
                    viewModel,
                    messageManager,
                    originalContent,
                    attachments,
                    largeFiles,
                    compressedImages,
                    onValidation
                )

            } catch (t: Throwable) {
                logger.error("Error in sendMessage hook", t)
            }
        }
    }

    private fun handleLargeFileUpload(
        context: Context,
        viewModel: Any,
        messageManager: MessageManager,
        originalContent: MessageContent,
        allAttachments: MutableList<Attachment<*>>,
        largeFiles: List<Attachment<*>>,
        compressedImages: Boolean,
        onValidation: Any
    ) {
        val storeMessages = StoreStream.getMessages()
        val thinkingMsg = createThinkingMessage(largeFiles.size)

        StoreMessages.`access$handleLocalMessageCreate`(storeMessages, thinkingMsg)

        Utils.threadPool.execute {
            try {
                val uploadedUrls = uploadProcessor.processAttachments(
                    attachments = largeFiles,
                    check = { _, att -> uploadProcessor.isSupportedFile(att) },
                    onProgress = { filename, current, total ->
                        val progressText = "Uploading `$filename`... ($current/$total)"
                        updateLocalMessage(storeMessages, thinkingMsg.id, progressText)
                    }
                )

                Utils.mainThread.post {
                    storeMessages.deleteMessage(thinkingMsg)

                    if (uploadedUrls.isEmpty()) {
                        Utils.showToast("Upload failed or cancelled", false)
                        return@post
                    }

                    // prepare new content
                    val newText = if (originalContent.textContent.isEmpty()) {
                        uploadedUrls.joinToString("\n")
                    } else {
                        "${originalContent.textContent}\n${uploadedUrls.joinToString("\n")}"
                    }

                    val newContent = originalContent.copy(newText, originalContent.mentionedUsers)
                    allAttachments.removeAll(largeFiles)

                    recallSendMessage(
                        viewModel,
                        context,
                        messageManager,
                        newContent,
                        allAttachments,
                        compressedImages,
                        onValidation
                    )
                }
            } catch (t: Throwable) {
                Utils.mainThread.post {
                    storeMessages.deleteMessage(thinkingMsg)
                    logger.error("Upload error", t)
                    Utils.showToast("Error: ${t.message}", false)
                }
            }
        }
    }

    private fun createThinkingMessage(fileCount: Int): Message {
        val clock = ClockFactory.get()
        val channelId = StoreStream.getChannelsSelected().id
        val clydeUser = Utils.buildClyde("BoxUpload", null)

        val msg = LocalMessageCreatorsKt.createLocalMessage(
            "Uploading $fileCount files...",
            channelId,
            clydeUser,
            null,  // timestamp
            false, // isPending
            false, // isEditing
            null,  // attachments
            null,  // embeds
            clock,
            null,   // stickers
            null,  // messageReference
            null,  // interaction
            null,  // flags
            null,  // stickerItems
            null,  // components
            null,  // application
            null   // referencedMessage
        )

        ReflectUtils.setField(Message::class.java, msg, "flags", MessageFlags.EPHEMERAL)
        ReflectUtils.setField(Message::class.java, msg, "type", MessageTypes.LOCAL)

        return msg
    }

    private fun updateLocalMessage(storeMessages: StoreMessages, messageId: Long, text: String) {
        Utils.mainThread.post {
            val clock = ClockFactory.get()
            val channelId = StoreStream.getChannelsSelected().id
            val clydeUser = Utils.buildClyde("BoxUpload", null)

            val newMsg = LocalMessageCreatorsKt.createLocalMessage(
                text,
                channelId,
                clydeUser,
                null,
                false,
                false,
                null,
                null,
                clock,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
            )

            ReflectUtils.setField(Message::class.java, newMsg, "id", messageId)
            ReflectUtils.setField(Message::class.java, newMsg, "flags", MessageFlags.EPHEMERAL)
            ReflectUtils.setField(Message::class.java, newMsg, "type", MessageTypes.LOCAL)

            StoreMessages.`access$handleLocalMessageCreate`(storeMessages, newMsg)
        }
    }

    private fun recallSendMessage(
        viewModel: Any,
        context: Context,
        messageManager: MessageManager,
        content: MessageContent,
        attachments: List<Attachment<*>>,
        compressedImages: Boolean,
        onValidation: Any
    ) {
        try {
            val method = viewModel.javaClass.declaredMethods.firstOrNull {
                it.name == "sendMessage" && it.parameterTypes.size == 6
            }

            if (method != null) {
                method.isAccessible = true
                method.invoke(
                    viewModel,
                    context,
                    messageManager,
                    content,
                    attachments,
                    compressedImages,
                    onValidation
                )
            } else {
                logger.error("Could not find sendMessage method signature", null)
                Utils.showToast("Error: Method signature mismatch", false)
            }
        } catch (e: Exception) {
            logger.error("Failed to recall sendMessage", e)
            Utils.showToast("Error sending message", false)
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
        commands.unregisterAll()
    }
}
