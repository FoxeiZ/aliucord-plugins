package com.github.foxeiz.settings

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Http
import com.aliucord.Logger
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.github.foxeiz.PluginUtils
import com.lytefast.flexinput.R
import com.uploader.services.Pomf
import java.util.concurrent.Future
import java.util.regex.Pattern


private class ServerDetector {

    companion object {

        const val TAG = "BoxUpload.Pomf.ServerDetector"
        private val logger = Logger(TAG)

        val titleRegex = Regex(
            Pattern.compile(
                """<title>(.*?)</title>""",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
        )
        val metaGeneratorRegex = Regex(
            Pattern.compile(
                """<meta\s+name=["']?generator["']?\s+content=["'](.*?)["']""",
                Pattern.CASE_INSENSITIVE or Pattern.DOTALL
            )
        )
        val maxSizeRegex = Regex("""Max upload size is (\d+)(?:&nbsp;\s?)?([mMiIbBgG]+)""")
        val expireTimeRegex = Regex("""files expire after (\d+)\s*(\w+)""")

        private val uguuIndicators =
            listOf("grill-wrapper", "upload-clipboard-btn", "js/uguu.js", "pomf.min.js")
        private val pomfIndicators = listOf("upload.php", "tools.html", "js/app.js", "ShareX")

        private fun extractGroup(pattern: Regex, input: String, group: Int = 1): String? =
            pattern.find(input, 0)?.groupValues?.getOrNull(group)

        private fun extractTitle(html: String): String? = extractGroup(titleRegex, html)

        private fun extractMetaGenerator(html: String): String? =
            extractGroup(metaGeneratorRegex, html)

        private fun parseMaxSize(html: String): Pair<Int, String> {
            val maxSizeMatch = maxSizeRegex.find(html, 0)
                ?: return Pair(0, "MiB")

            val maxSizeStr = maxSizeMatch.groupValues.getOrNull(1) ?: ""
            val maxSizeUnit = maxSizeMatch.groupValues.getOrNull(2) ?: "MiB"

            val maxSize = try {
                if (maxSizeStr.trim().isEmpty()) 0 else maxSizeStr.toInt()
            } catch (_: NumberFormatException) {
                logger.warn("Failed to parse max size number: $maxSizeStr")
                0
            }

            return Pair(maxSize, maxSizeUnit)
        }

        private fun parseExpireTime(html: String): Pair<String, String> {
            val expireMatch = expireTimeRegex.find(html, 0)
                ?: return Pair("", "")

            val expireTime = expireMatch.groupValues.getOrNull(1) ?: ""
            val expireUnit = expireMatch.groupValues.getOrNull(2) ?: ""

            return Pair(expireTime, expireUnit)
        }

        fun detect(url: String): Pomf.ServerConfig {
            logger.info("Detecting server type for URL: $url")

            val response = try {
                Http.Request(url, "GET")
                    .setFollowRedirects(true)
                    .setRequestTimeout(30000)
                    .execute()
            } catch (e: Exception) {
                logger.error("Network error while fetching URL: $url", e)
                throw Exception("Network error: ${e.message}", e)
            }

            try {
                if (!response.ok()) {
                    throw Exception("HTTP error: ${response.statusCode} ${response.statusMessage}")
                }

                val html = response.text()
                logger.debug("Fetched HTML content from $url (${html.length} chars)")

                val serverType = detectServerType(html)
                logger.debug("Detected server type: $serverType")

                return when (serverType) {
                    Pomf.ServerType.UGUU -> parseUguuConfig(url, html)
                    Pomf.ServerType.POMF -> parsePomfConfig(url, html)
                    else -> Pomf.ServerConfig(Pomf.ServerType.UNKNOWN, url)
                }
            } finally {
                response.close()
            }
        }

        fun parseUguuConfig(url: String, html: String): Pomf.ServerConfig {
            val (maxSize, maxSizeUnit) = parseMaxSize(html)
            val (expireTime, expireUnit) = parseExpireTime(html)

            if (maxSize == 0) {
                logger.warn("Could not parse max upload size for Uguu server: $url")
            }

            return Pomf.ServerConfig(
                Pomf.ServerType.UGUU,
                url,
                maxUploadSize = maxSize,
                maxSizeUnit = maxSizeUnit,
                expireTime = expireTime,
                expireTimeUnit = expireUnit
            )
        }

        fun parsePomfConfig(url: String, html: String): Pomf.ServerConfig {
            val (maxSize, maxSizeUnit) = parseMaxSize(html)

            if (maxSize == 0) {
                logger.warn("Could not parse max upload size for Pomf server: $url")
            }

            return Pomf.ServerConfig(
                Pomf.ServerType.POMF,
                url,
                maxUploadSize = maxSize,
                maxSizeUnit = maxSizeUnit
            )
        }

        private fun detectServerType(html: String): Pomf.ServerType {
            val generator = extractMetaGenerator(html)

            generator?.let {
                return when {
                    it.contains("Uguu", ignoreCase = true) -> {
                        logger.info("Detected Uguu server via meta generator")
                        Pomf.ServerType.UGUU
                    }

                    it.contains("Pomf", ignoreCase = true) -> {
                        logger.info("Detected Pomf server via meta generator")
                        Pomf.ServerType.POMF
                    }

                    else -> detectByIndicators(html)
                }
            }

            return detectByIndicators(html)
        }

        private fun detectByIndicators(html: String): Pomf.ServerType {
            val uguuScore = uguuIndicators.count { html.contains(it, ignoreCase = true) }
            val pomfScore = pomfIndicators.count { html.contains(it, ignoreCase = true) }

            logger.info("Detection scores - Uguu: $uguuScore, Pomf: $pomfScore")

            return when {
                uguuScore > pomfScore -> {
                    logger.info("Detected Uguu server via content indicators")
                    Pomf.ServerType.UGUU
                }

                pomfScore > uguuScore -> {
                    logger.info("Detected Pomf server via content indicators")
                    Pomf.ServerType.POMF
                }

                else -> {
                    logger.info("Could not determine server type")
                    Pomf.ServerType.UNKNOWN
                }
            }
        }
    }
}

@SuppressLint("SetTextI18n", "ViewConstructor")
class PomfSettings(context: Context, settings: SettingsAPI) : LinearLayout(context) {
    companion object {
        private val logger = Logger("PomfSettings")
        const val POMF_CONFIG_KEY = "pomf_config"
    }

    fun makeConfigInfo(serverConfig: Pomf.ServerConfig): String {
        if (serverConfig.url.trim().isEmpty()) return "No Pomf/Uguu server configured."

        return buildString {
            append(
                if (serverConfig.serverType != Pomf.ServerType.UNKNOWN)
                    "Current server: ${serverConfig.serverType.key} (${serverConfig.url})\n"
                else
                    "Unknown server type at ${serverConfig.url}\n"
            )

            append(
                if (serverConfig.maxUploadSize != null)
                    "Max upload size: ${serverConfig.maxUploadSize} ${serverConfig.maxSizeUnit ?: "MiB"}\n"
                else
                    "Max upload size: Unknown\n"
            )

            append(
                if (serverConfig.expireTime != null && serverConfig.expireTimeUnit != null)
                    "Expire time: ${serverConfig.expireTime} ${serverConfig.expireTimeUnit}\n"
                else
                    "Expire time: Unknown\n"
            )
        }
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, DimenUtils.defaultPadding)
        }

        TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Pomf Settings"
            addView(this)
        }


        var serverConfig: Pomf.ServerConfig =
            settings.getObject(POMF_CONFIG_KEY, Pomf.ServerConfig.default())

        val configInfo = TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText).apply {
            text = makeConfigInfo(serverConfig)
            addView(this)
        }

        var fut: Future<*>? = null

        PluginUtils.createTextInput(
            context,
            this,
            "Pomf/Uguu server url",
            serverConfig.url,
            PluginUtils.createTextWatcher(
                {
                    val currentFut = fut
                    if (currentFut != null && !currentFut.isDone) {
                        logger.info("Cancelling previous server detection task")
                        logger.info(currentFut.cancel(true).toString())
                        fut = null
                    }

                    // why trim().isEmpty()? because i cant use blank() lol. somehow.
                    if (it.isNullOrEmpty() || it.trim().isEmpty()) {
                        settings.setString(POMF_CONFIG_KEY, "")
                        return@createTextWatcher
                    }

                    val url = it.toString().trim()
                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        return@createTextWatcher
                    }

                    fut = Utils.threadPool.submit {
                        try {
                            Utils.mainThread.post {
                                configInfo.text = "Detecting server..."
                            }
                            val config = ServerDetector.detect(url)
                            Utils.mainThread.post {
                                configInfo.text = makeConfigInfo(config)
                                settings.setObject(POMF_CONFIG_KEY, config)
                            }
                        } catch (e: Throwable) {
                            configInfo.text = "Failed to detect server: ${e.stackTraceToString()}"
                            logger.error(e)
                            return@submit
                        }
                    }
                }
            )
        )
    }
}
