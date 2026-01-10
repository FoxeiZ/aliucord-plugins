package com.uploader.services

import com.aliucord.Http
import com.uploader.FileHostingService
import java.io.File

internal data class PomfFile(
    val hash: String,
    val filename: String,
    val url: String,
    val size: Long,
    val dupe: Boolean,
)

internal data class PomfResponse(
    val success: Boolean,
    val files: List<PomfFile>
)

class Pomf(
    private val config: ServerConfig
) : FileHostingService() {

    data class ServerConfig(
        val serverType: ServerType,
        val url: String,
        val maxUploadSize: Int? = null,
        val maxSizeUnit: String? = null,
        val expireTime: String? = null,
        val expireTimeUnit: String? = null,
    ) {
        companion object {
            fun default(): ServerConfig {
                return ServerConfig(
                    serverType = ServerType.UGUU,
                    url = "https://uguu.se/",
                    maxUploadSize = 128,
                    expireTime = "3",
                    expireTimeUnit = "hours"
                )
            }
        }
    }

    enum class ServerType(val key: String) {
        UNKNOWN("UNKNOWN"),
        UGUU("UGUU"),
        POMF("POMF")
    }

    private fun makePostRequest(
        parameters: Map<String, Any> = emptyMap()
    ): String {
        val url = config.url + if (config.serverType == ServerType.UGUU) {
            "/upload"
        } else {
            "/upload.php"
        }
        val allParams = mutableMapOf<String, Any>().apply {
            putAll(parameters)
        }

        val response = Http.Request(url, "POST")
            .setHeader("User-Agent", "PomfClient/1.0")
            .executeWithMultipartForm(allParams)
        if (response.statusCode != 200) {
            throw Exception("Failed to make request: ${response.statusCode} ${response.statusMessage}")
        }
        return response.json(PomfResponse::class.java).files.first().url  // kinda dirty, oh well
    }

    override fun getServiceName(): String = "Pomf/Uguu"

    override fun getMaxFileSize(): Float? = config.maxUploadSize?.toFloat()

    override fun isSupportedFileExtension(extension: String): Boolean {
        val unsupportedExtensions = setOf(
            "exe",
            "scr",
            "com",
            "vbs",
            "bat",
            "cmd",
            "htm",
            "html",
            "jar",
            "msi",
            "apk",
            "phtml",
            "svg"
        )
        return !unsupportedExtensions.contains(extension.lowercase())
    }

    override fun upload(file: File): String {
        require(file.name.trim().isNotEmpty()) { "'name' must not be blank" }
        return makePostRequest(mapOf("files[]" to file))
    }

    override fun upload(url: String): String {
        throw UnsupportedOperationException("Pomf/Uguu does not support URL uploads")
    }

    override fun delete(files: Set<String>) {
        throw UnsupportedOperationException("Pomf/Uguu does not support file deletion")
    }
}
