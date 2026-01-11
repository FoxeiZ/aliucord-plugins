package com.github.foxeiz.settings

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.github.foxeiz.PluginUtils
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n", "ViewConstructor")
class CatboxSettings(context: Context, settings: SettingsAPI) : LinearLayout(context) {
    companion object {
        const val USER_HASH_KEY = "catbox_userhash"
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, DimenUtils.defaultPadding)
        }

        TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Catbox Settings"
            addView(this)
        }

        PluginUtils.createTextInput(
            context,
            this,
            "Enter your Catbox user hash key",
            settings.getString(USER_HASH_KEY, ""),
            PluginUtils.createTextWatcher(
                {
                    settings.setString(USER_HASH_KEY, it?.toString() ?: "")
                }
            )
        )
    }
}
