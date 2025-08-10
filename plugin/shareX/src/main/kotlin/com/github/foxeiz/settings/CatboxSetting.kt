package com.github.foxeiz.settings

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.aliucord.views.TextInput
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n", "ViewConstructor")
class CatboxSetting(context: Context, settings: SettingsAPI) : LinearLayout(context) {
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

        TextInput(
            context,
            "Enter your Catbox user hash",
            settings.getString(USER_HASH_KEY, ""),
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?, start: Int, count: Int, after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    settings.setString(USER_HASH_KEY, s?.toString() ?: "")
                }
            }
        ).apply {
            this@CatboxSetting.addView(this)
        }
    }
}