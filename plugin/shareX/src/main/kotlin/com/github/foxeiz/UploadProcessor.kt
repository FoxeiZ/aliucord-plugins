package com.github.foxeiz

import android.content.Context
import com.aliucord.Logger
import com.aliucord.Utils
import com.github.foxeiz.AttachmentUtils.getFileSize
import com.github.foxeiz.AttachmentUtils.toFile
import com.lytefast.flexinput.model.Attachment
import com.uploader.FileHostingService

class UploadProcessor(
    private val context: Context,
    private val uploadProvider: FileHostingService,
    private val logger: Logger
) {
    private val notificationHelper = NotificationHelper(context)

    fun processAttachments(
        attachments: List<Attachment<*>>,
        check: (Context, Attachment<*>) -> Boolean
    ): List<String> {
        logger.info("Processing ${attachments.size} large attachments")
        val uploadedUrls = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val notificationId = System.currentTimeMillis().toInt()

        attachments.forEach { attachment ->
            try {
                if (!check(context, attachment)) {
                    logger.debug("Skipping unsupported attachment: ${attachment.displayName}")
                    return@forEach
                }

                attachment.toFile(context)?.let { file ->
                    file.deleteOnExit()

                    notificationHelper.showUploadProgress(
                        notificationId,
                        attachment.displayName ?: "unknown",
                        uploadedUrls.size,
                        attachments.size
                    )

                    val url = uploadProvider.upload(file)
                    uploadedUrls.add(url)
                    logger.info("Uploaded ${attachment.displayName} successfully, url: $url")

                    if (!file.delete()) {
                        logger.warn("Failed to delete temp file: ${file.absolutePath}")
                    }
                } ?: run {
                    val errorMsg = "Failed to create temp file for ${attachment.displayName}"
                    logger.error(errorMsg, null)
                    errors.add(errorMsg)
                }
            } catch (e: Throwable) {
                val errorMsg = "Upload failed for ${attachment.displayName}: ${e.message}"
                logger.error(errorMsg, e)
                errors.add(errorMsg)
            }
        }

        handleUploadResults(errors, notificationId)
        return uploadedUrls
    }

    private fun handleUploadResults(errors: List<String>, notificationId: Int) {
        if (errors.isNotEmpty()) {
            errors.forEach { logger.error("Error uploading files: $it", null) }
            Utils.showToast("Error uploading files: ${errors.size} error. Check logs", false)
        }
        notificationHelper.dismissNotification(notificationId)
    }

    fun isLargeAttachment(attachment: Attachment<*>): Boolean {
        val fileSize = attachment.getFileSize(context)
        return fileSize > 10 * 1024 * 1024
    }

    fun isSupportedFile(attachment: Attachment<*>): Boolean {
        val fileType = context.contentResolver.getType(attachment.uri)
            ?: attachment.uri.toString().substringAfterLast('.', "").trim()
                .ifEmpty { "bin" }

        return uploadProvider.isSupportedFileExtension(fileType)
    }
}