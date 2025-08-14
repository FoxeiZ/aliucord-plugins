import com.aliucord.Http
import com.uploader.FileHostingService
import java.io.File


private enum class RequestType(val value: String) {
    FILE_UPLOAD("fileupload"),
}

class Litterbox(private val duration: Int = 1) : FileHostingService() {

    companion object {
        const val API_URL = "https://litterbox.catbox.moe/resources/internals/api.php"
    }

    private fun makePostRequest(
        reqType: RequestType,
        parameters: Map<String, Any> = emptyMap()
    ): String {
        val allParams = mutableMapOf<String, Any>().apply {
            put("reqtype", reqType.value)
            putAll(parameters)
        }

        return Http.Request(API_URL, "POST")
            .setHeader("User-Agent", "CatboxClient/1.0")
            .executeWithMultipartForm(allParams)
            .use { response ->
                if (response.statusCode != 200) {
                    throw Exception("Failed to make request: ${response.statusCode} ${response.statusMessage}")
                }
                return response.text()
            }
    }

    override fun getServiceName(): String = "Litterbox"

    override fun getMaxFileSize(): Float = 1000F

    override fun isSupportedFileExtension(extension: String): Boolean {
        val unsupportedExtensions = setOf(
            "exe", "scr", "cpl", "doc", "docx", "jar"
        )
        return !unsupportedExtensions.contains(extension.lowercase())
    }

    override fun upload(file: File): String {
        require(file.name.trim().isNotEmpty()) { "'name' must not be blank" }
        return makePostRequest(
            RequestType.FILE_UPLOAD,
            mapOf("fileToUpload" to file, "time" to duration.toString() + "h")
        )
    }

    override fun upload(url: String): String {
        throw UnsupportedOperationException("Litterbox does not support URL uploads")
    }

    override fun delete(files: Set<String>) {
        throw UnsupportedOperationException("Litterbox does not support file deletion")
    }
}
