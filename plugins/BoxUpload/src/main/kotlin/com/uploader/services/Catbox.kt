package com.uploader.services

import com.aliucord.Http
import com.uploader.FileHostingService
import com.uploader.exceptions.NoSuchCatboxAlbumException
import com.uploader.exceptions.NoSuchCatboxFileException
import com.uploader.utils.toCatboxFiles

import java.io.File


internal sealed class RequestType(val reqType: String) {
    object FileUpload : RequestType("fileupload")
    object UrlUpload : RequestType("urlupload")
    object DeleteFiles : RequestType("deletefiles")
    object CreateAlbum : RequestType("createalbum")
    object EditAlbum : RequestType("editalbum")
    object AddToAlbum : RequestType("addtoalbum")
    object RemoveFromAlbum : RequestType("removefromalbum")
    object DeleteAlbum : RequestType("deletealbum")

    companion object {
        fun fromReqType(value: String): RequestType? = when (value) {
            FileUpload.reqType -> FileUpload
            UrlUpload.reqType -> UrlUpload
            DeleteFiles.reqType -> DeleteFiles
            CreateAlbum.reqType -> CreateAlbum
            EditAlbum.reqType -> EditAlbum
            AddToAlbum.reqType -> AddToAlbum
            RemoveFromAlbum.reqType -> RemoveFromAlbum
            DeleteAlbum.reqType -> DeleteAlbum
            else -> null
        }
    }
}

class Catbox(private val userHash: String?) : FileHostingService() {

    companion object {
        const val API_URL = "https://catbox.moe/user/api.php"
        const val MAX_ALBUM_FILES = 500
        const val NO_SUCH_FILE_ERROR = "No such file."
        const val NO_SUCH_ALBUM_ERROR = "Album not found."
    }

    private fun makePostRequest(
        reqType: RequestType,
        parameters: Map<String, Any> = emptyMap()
    ): String {
        val allParams = mutableMapOf<String, Any>().apply {
            put("reqtype", reqType)
            if (userHash != null) put("userhash", userHash)
            putAll(parameters)
        }

        val response = Http.Request(API_URL, "POST")
            .setHeader("User-Agent", "CatboxClient/1.0")
            .executeWithMultipartForm(allParams)

        if (response.statusCode != 200) {
            throw Exception("Failed to make request: ${response.statusCode} ${response.statusMessage}")
        }
        return response.text()

    }

    private fun isCatboxError(response: String, errorMessage: String): Boolean =
        response.trim() == errorMessage

    override fun getServiceName(): String = "Catbox"

    override fun getMaxFileSize(): Float = 200F

    override fun isSupportedFileExtension(extension: String): Boolean {
        val unsupportedExtensions = setOf(
            "exe", "scr", "cpl", "doc", "docx", "jar"
        )
        return !unsupportedExtensions.contains(extension.lowercase())
    }

    override fun upload(file: File): String {
        require(file.name.trim().isNotEmpty()) { "'name' must not be blank" }
        return makePostRequest(RequestType.FileUpload, mapOf("fileToUpload" to file))
    }

    override fun upload(url: String): String =
        makePostRequest(RequestType.UrlUpload, mapOf("url" to url))

    override fun delete(files: Set<String>) {
        try {
            val response =
                makePostRequest(RequestType.DeleteFiles, mapOf("files" to files.toCatboxFiles()))
            if (isCatboxError(response, NO_SUCH_FILE_ERROR)) {
                throw NoSuchCatboxFileException(Exception("File not found"))
            }
        } catch (e: Exception) {
            if (e !is NoSuchCatboxFileException) {
                throw e
            } else {
                throw e
            }
        }
    }

    fun createAlbum(
        title: String,
        description: String,
        files: Set<String>
    ): String {
        require(files.size <= MAX_ALBUM_FILES) {
            "Albums can only contain $MAX_ALBUM_FILES files, was given ${files.size}."
        }
        val params = mapOf(
            "title" to title,
            "desc" to description,
            "files" to files.toCatboxFiles()
        )
        return makePostRequest(RequestType.CreateAlbum, params)
    }

    fun editAlbum(
        short: String,
        title: String,
        description: String,
        files: Set<String>
    ) {
        require(files.size <= MAX_ALBUM_FILES) {
            "Albums can only contain $MAX_ALBUM_FILES files, was given ${files.size}."
        }
        val params = mapOf(
            "title" to title,
            "desc" to description,
            "files" to files.toCatboxFiles()
        )
        makeAlbumRequest(short, RequestType.EditAlbum, params)
    }

    fun addToAlbum(short: String, files: Set<String>) {
        makeAlbumRequest(short, RequestType.AddToAlbum, mapOf("files" to files.toCatboxFiles()))
    }

    fun removeFromAlbum(short: String, files: Set<String>) {
        makeAlbumRequest(
            short,
            RequestType.RemoveFromAlbum,
            mapOf("files" to files.toCatboxFiles())
        )
    }

    fun deleteAlbum(short: String) {
        makeAlbumRequest(short, RequestType.DeleteAlbum)
    }

    private fun makeAlbumRequest(
        short: String,
        reqType: RequestType,
        additionalParams: Map<String, String> = emptyMap()
    ): String = try {
        val params = mutableMapOf("short" to short).apply {
            putAll(additionalParams)
        }
        val response = makePostRequest(reqType, params)

        when {
            isCatboxError(response, NO_SUCH_ALBUM_ERROR) ->
                throw NoSuchCatboxAlbumException(short, Exception("Album not found"))

            isCatboxError(response, NO_SUCH_FILE_ERROR) ->
                throw NoSuchCatboxFileException(Exception("File not found"))

            else -> response
        }
    } catch (e: Exception) {
        when (e) {
            is NoSuchCatboxAlbumException, is NoSuchCatboxFileException -> throw e
            else -> {
                throw e
            }
        }
    }
}
