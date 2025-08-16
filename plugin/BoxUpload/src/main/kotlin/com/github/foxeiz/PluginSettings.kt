package com.github.foxeiz

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.CommandsAPI
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Divider
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.github.foxeiz.commands.CatboxCommands
import com.github.foxeiz.settings.CatboxSettings
import com.github.foxeiz.settings.LitterboxSettings
import com.github.foxeiz.settings.PomfSettings
import com.lytefast.flexinput.R


private data class UploadProviderOption(
    val name: String,
    val checkedSetting: CheckedSetting,
    val extraSettingFactory: ((Context, SettingsAPI) -> ViewGroup)? = null,
    val extraCommandFactory: CatboxCommands.Companion? = null
) {
    override fun toString(): String = name
}

class PluginSettings(private val settings: SettingsAPI, private val commands: CommandsAPI) :
    SettingsPage() {

    companion object {
        const val USER_AGENT_KEY = "user_agent"
        const val HEADER_KEY = "header"
        const val UPLOAD_SERVICE_KEY = "upload_service"
        const val ALWAYS_UPLOAD_KEY = "always_upload"
        const val DISABLED_KEY = "disabled"
    }

    enum class UploadProvider(val value: String) {
        CATBOX_ANON("catbox-anon"),
        CATBOX_USER("catbox-user"),
        LITTERBOX("litterbox"),
        POMF("pomf")
    }

    private var removeCommandCallback: ((CommandsAPI) -> Unit)? = null

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("Catbox")
        val ctx = requireContext()
        val padding = DimenUtils.defaultPadding

        setupDisableToggle(ctx)
        setupAlwaysUploadToggle(ctx)
        setupUploadProviderSection(ctx)
        addView(Divider(ctx))
        setupGeneralSettings(ctx, padding)
    }

    private fun setupDisableToggle(ctx: Context) {
        Utils.createCheckedSetting(
            ctx, CheckedSetting.ViewType.SWITCH,
            "Disable toggle", ""
        ).apply {
            isChecked = settings.getBool(DISABLED_KEY, false)
            setOnCheckedListener { settings.setBool(DISABLED_KEY, it) }
            this@PluginSettings.addView(this)
        }
    }

    private fun setupAlwaysUploadToggle(ctx: Context) {
        Utils.createCheckedSetting(
            ctx, CheckedSetting.ViewType.SWITCH,
            "Always upload", "Upload all file, event if they are small"
        ).apply {
            isChecked = settings.getBool(ALWAYS_UPLOAD_KEY, false)
            setOnCheckedListener { settings.setBool(ALWAYS_UPLOAD_KEY, it) }
            this@PluginSettings.addView(this)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupUploadProviderSection(ctx: Context) {
        // Header
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Upload Provider"
            addView(this)
        }

        val providerOptions = createProviderOptions(ctx)
        val extraSettingLayout = LinearLayout(ctx)

        setupRadioManager(ctx, providerOptions, extraSettingLayout)
        addView(extraSettingLayout)
    }

    private fun createProviderOptions(ctx: Context): List<UploadProviderOption> {
        return listOf(
            UploadProviderOption(
                UploadProvider.CATBOX_ANON.value,
                Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO,
                    "Catbox (anon)", "Max of 200MB per file, no user hash required"
                )
            ),
            UploadProviderOption(
                UploadProvider.CATBOX_USER.value,
                Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO,
                    "Catbox", "Max of 200MB per file, with user hash"
                ),
                { context, settings -> CatboxSettings(context, settings) },
                extraCommandFactory = CatboxCommands.Companion
            ),
            UploadProviderOption(
                UploadProvider.LITTERBOX.value,
                Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO,
                    "Litterbox", "Max of 1GB per file, remove after 1 (or set time) hour(s)"
                ),
                { context, settings -> LitterboxSettings(context, settings) }
            ),
            UploadProviderOption(
                UploadProvider.POMF.value,
                Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO,
                    "Pomf", "Max file size is depends on the server"
                ),
                { context, settings -> PomfSettings(context, settings) }
            )
        )
    }

    private fun setupRadioManager(
        ctx: Context,
        providerOptions: List<UploadProviderOption>,
        extraSettingLayout: LinearLayout
    ) {
        fun updateEntry(entry: UploadProviderOption) {
            extraSettingLayout.removeAllViews()
            entry.extraSettingFactory?.let { factory ->
                extraSettingLayout.addView(factory(ctx, settings))
            }
            entry.extraCommandFactory?.let {
                removeCommandCallback?.invoke(commands)
                removeCommandCallback = it::unregisterAll
                it.registerAll(commands)
            }
        }

        RadioManager(providerOptions.map { it.checkedSetting }).apply {
            val uploadService = settings.getString(UPLOAD_SERVICE_KEY, "catbox-anon")
            val selected = providerOptions.find { it.name == uploadService }

            providerOptions.forEach { entry ->
                entry.checkedSetting.e {
                    a(entry.checkedSetting)
                    settings.setString(UPLOAD_SERVICE_KEY, entry.name)
                    updateEntry(entry)
                }
                addView(entry.checkedSetting)
            }

            selected?.let {
                a(it.checkedSetting)
                updateEntry(it)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupGeneralSettings(ctx: Context, padding: Int) {
        // Header
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Settings"
            addView(this)
        }

        // User Agent input
        createTextInput(
            ctx, padding,
            "User Agent",
            USER_AGENT_KEY
        )

        // Header input
        createTextInput(
            ctx, padding,
            "Header",
            HEADER_KEY
        )
    }

    private fun createTextInput(
        ctx: Context,
        padding: Int,
        label: String,
        settingsKey: String
    ) {
        PluginUtils.createTextInput(
            ctx,
            this.linearLayout,
            label,
            settings.getString(settingsKey, ""),
            PluginUtils.createTextWatcher({ s ->
                settings.setString(settingsKey, s?.toString() ?: "")
            }),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, padding, 0, 0)
            }
        )
    }
}