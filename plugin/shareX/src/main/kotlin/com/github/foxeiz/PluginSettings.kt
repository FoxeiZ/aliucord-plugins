package com.github.foxeiz

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.fragments.SettingsPage
import com.aliucord.utils.DimenUtils
import com.aliucord.views.Divider
import com.aliucord.views.TextInput
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.github.foxeiz.settings.CatboxSetting
import com.lytefast.flexinput.R

private data class Huh(
    val name: String,
    val checkedSetting: CheckedSetting,
    val extraSettingFactory: ((Context, SettingsAPI) -> CatboxSetting)? = null
) {
    override fun toString(): String = name
}

class PluginSettings(private val settings: SettingsAPI) : SettingsPage() {

    companion object {
        const val USER_AGENT_KEY = "user_agent"
        const val HEADER_KEY = "user_agent"
    }

    enum class UploadProvider(val value: String) {
        CATBOX_ANON("catbox-anon"), CATBOX_USER("catbox-user"),
    }

    private class makeTextWatcher(private val afterTextChanged: (Editable?) -> Unit) :
        TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable?) {
            afterTextChanged(s)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewBound(view: View) {
        super.onViewBound(view)
        setActionBarTitle("Catbox")
        val ctx = requireContext()
        val p = DimenUtils.defaultPadding

        // SERVICE CHOOSER
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Upload Provider"
            addView(this)
        }

        val l = listOf<Huh>(
            Huh(
                UploadProvider.CATBOX_ANON.value, Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO, "Catbox (anon)", "Catbox anonymous upload"
                ), null
            ), Huh(
                UploadProvider.CATBOX_USER.value, Utils.createCheckedSetting(
                    ctx, CheckedSetting.ViewType.RADIO, "Catbox", "Catbox with user hash"
                )
            ) { context, settings -> CatboxSetting(context, settings) })

        val extraSettingLayout = LinearLayout(ctx)

        fun updateExtraSettings(entry: Huh) {
            extraSettingLayout.removeAllViews()
            entry.extraSettingFactory?.let { factory ->
                extraSettingLayout.addView(factory(ctx, settings))
            }
        }

        RadioManager(l.map { it.checkedSetting }).apply {
            val uploadService = settings.getString(SettingsKey.UPLOAD_SERVICE_KEY, "catbox-anon")
            val selected = l.find { it.name == uploadService }

            l.forEach { entry ->
                entry.checkedSetting.e {
                    a(entry.checkedSetting)
                    settings.setString("uploadService", entry.name)
                    updateExtraSettings(entry)
                }
                addView(entry.checkedSetting)
            }

            selected?.let {
                a(it.checkedSetting)
                updateExtraSettings(it)
            }
        }
        addView(extraSettingLayout)

        addView(Divider(ctx))

        // GENERAL SETTINGS
        TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Settings"
            addView(this)
        }

        TextInput(
            ctx,
            "User Agent",
            settings.getString(USER_AGENT_KEY, ""),
            makeTextWatcher { s -> settings.setString(USER_AGENT_KEY, s?.toString() ?: "") }
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, p, 0, 0)
            }
            this@PluginSettings.addView(this)
        }

        TextInput(
            ctx, "Header", settings.getString(HEADER_KEY, ""),
            makeTextWatcher { s -> settings.setString(HEADER_KEY, s?.toString() ?: "") }
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, p, 0, 0)
            }
            this@PluginSettings.addView(this)
        }
    }
}