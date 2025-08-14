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

        private val logger = Logger("ServerDetector")

        private fun extractGroup(pattern: Regex, input: String, group: Int = 1): String? =
            pattern.find(input, 0)?.groupValues?.getOrNull(group)


        private fun extractTitle(html: String): String? =
            extractGroup(
                Regex(
                    Pattern.compile(
                        """<title>(.*?)</title>""",
                        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                    )
                ),
                html
            )

        private fun extractMetaGenerator(html: String): String? =
            extractGroup(
                Regex(
                    Pattern.compile(
                        """<meta\s+name=["']?generator["']?\s+content=["'](.*?)["']""",
                        Pattern.CASE_INSENSITIVE or Pattern.DOTALL
                    )
                ),
                html
            )

        fun detect(url: String): Pomf.ServerConfig {
            logger.info("Detecting server type for URL: $url")

            val response = Http.Request(url, "GET")
                .setFollowRedirects(true)
                .setRequestTimeout(30000)
                .execute()
            logger.warn("HTTP request executed for URL: $url")

            if (!response.ok()) {
                logger.warn("HTTP request failed with status code: ${response.statusCode}, message: ${response.statusMessage}")
                throw Exception("Failed to fetch URL: ${response.statusCode} ${response.statusMessage}")
            }

            val html = response.text().toString()
            logger.info("Fetched HTML content from $url")
            logger.warn("HTML content length: ${html.length}")
            logger.warn(html)

            val serverType = detectServerType(html)
            logger.warn("Detected server type: $serverType")

            val config = when (serverType) {
                Pomf.ServerType.UGUU -> {
                    logger.warn("Parsing UGUU server configuration")
                    parseUguuConfig(url, html)
                }

                Pomf.ServerType.POMF -> {
                    logger.warn("Parsing POMF server configuration")
                    parsePomfConfig(url, html)
                }

                else -> {
                    logger.warn("Unknown server type detected, returning default configuration")
                    Pomf.ServerConfig(Pomf.ServerType.UNKNOWN, url)
                }
            }

            logger.warn("Final server type: ${config.serverType}")
            logger.warn("Final server configuration: $config")

            return config
        }

        fun parseUguuConfig(url: String, html: String): Pomf.ServerConfig {
            logger.warn("Parsing UGUU server configuration for URL: $url")

            val maxSizeRegex = """Max upload size is (\d+)(?:&nbsp;\s?)?([mMiIbBgG]+)""".toRegex()
            val maxSize = maxSizeRegex.find(html, 0)?.groupValues?.get(1) ?: ""
            logger.warn("Extracted max upload size: $maxSize MiB")

            val expireRegex = """files expire after (\d+)\s*(\w+)""".toRegex()
            val expireMatch = expireRegex.find(html, 0)
            val expireTime = expireMatch?.groupValues?.get(1) ?: ""
            val expireUnit = expireMatch?.groupValues?.get(2) ?: ""
            logger.warn("Extracted expire time: $expireTime $expireUnit")

            return Pomf.ServerConfig(
                Pomf.ServerType.UGUU,
                url,
                maxUploadSize = maxSize.trim().isEmpty().let { if (it) 0 else maxSize.toInt() },
                expireTime = expireTime,
                expireTimeUnit = expireUnit,
            )
        }

        fun parsePomfConfig(url: String, html: String): Pomf.ServerConfig {
            logger.warn("Parsing POMF server configuration for URL: $url")

            val maxSizeRegex = """Max upload size is (\d+)(?:&nbsp;\s?)?([mMiIbBgG]+)""".toRegex()
            val maxSize = maxSizeRegex.find(html, 0)?.groupValues?.get(1) ?: ""
            val maxSizeUnit = maxSizeRegex.find(html, 0)?.groupValues?.get(2) ?: "MiB"
            logger.warn("Extracted max upload size: $maxSize $maxSizeUnit")

            return Pomf.ServerConfig(
                Pomf.ServerType.POMF,
                url,
                maxUploadSize = maxSize.trim().isEmpty().let { if (it) 0 else maxSize.toInt() },
            )
        }

        private fun detectServerType(html: String): Pomf.ServerType {
            val generator = extractMetaGenerator(html)
            logger.warn("Extracted meta generator: $generator")

            if (generator == null) {
                logger.warn("No meta generator found, returning UNKNOWN")
                return Pomf.ServerType.UNKNOWN
            }

            return when {
                generator.contains("Uguu", ignoreCase = true) -> {
                    logger.warn("Meta generator contains 'Uguu', returning UGUU")
                    Pomf.ServerType.UGUU
                }

                generator.contains("Pomf", ignoreCase = true) -> {
                    logger.warn("Meta generator contains 'Pomf', returning POMF")
                    Pomf.ServerType.POMF
                }

                else -> {
                    logger.warn("Meta generator did not match, checking structure indicators")
                    val uguuIndicators =
                        listOf("grill-wrapper", "upload-clipboard-btn", "js/uguu.js", "pomf.min.js")
                    val pomfIndicators = listOf("upload.php", "tools.html", "js/app.js", "ShareX")
                    val uguuScore = uguuIndicators.count { html.contains(it, ignoreCase = true) }
                    val pomfScore = pomfIndicators.count { html.contains(it, ignoreCase = true) }
                    logger.warn("Uguu indicator score: $uguuScore")
                    logger.warn("Pomf indicator score: $pomfScore")
                    when {
                        uguuScore > pomfScore -> {
                            logger.warn("Uguu score higher, returning UGUU")
                            Pomf.ServerType.UGUU
                        }

                        pomfScore > uguuScore -> {
                            logger.warn("Pomf score higher, returning POMF")
                            Pomf.ServerType.POMF
                        }

                        else -> {
                            logger.warn("Scores equal or zero, returning UNKNOWN")
                            Pomf.ServerType.UNKNOWN
                        }
                    }
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
        return if (serverConfig.serverType != Pomf.ServerType.UNKNOWN) {
            "Current server: ${serverConfig.serverType.name} (${serverConfig.url})\n" +
                    "Max upload size: ${serverConfig.maxUploadSize} MiB\n" +
                    "Expire time: ${serverConfig.expireTime} ${serverConfig.expireTimeUnit}"
        } else {
            "No valid Pomf/Uguu server configured."
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
            settings.getObject<Pomf.ServerConfig>(POMF_CONFIG_KEY, Pomf.ServerConfig.default())

        val configInfo = TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
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
                                logger.info("Run in main thread")
                                configInfo.text = makeConfigInfo(config)
                                settings.setObject(POMF_CONFIG_KEY, config)
                            }
                        } catch (e: Throwable) {
                            configInfo.text = "Failed to detect server: ${e.message}"
                            logger.error(e)
                            return@submit
                        }
                    }
                }
            )
        )
    }
}