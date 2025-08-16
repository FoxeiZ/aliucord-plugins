package com.github.foxeiz.settings

import android.annotation.SuppressLint
import android.content.Context
import android.widget.LinearLayout
import android.widget.TextView
import com.aliucord.Utils
import com.aliucord.api.SettingsAPI
import com.aliucord.utils.DimenUtils
import com.discord.views.CheckedSetting
import com.discord.views.RadioManager
import com.lytefast.flexinput.R

@SuppressLint("SetTextI18n", "ViewConstructor")
class LitterboxSettings(context: Context, settings: SettingsAPI) : LinearLayout(context) {
    companion object {
        const val TIME_KEY = "litterbox_time_index"
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, DimenUtils.defaultPadding)
        }

        TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply {
            text = "Litterbox Settings"
            addView(this)
        }

        val checkedSettings = mapOf<Int, CheckedSetting>(
            1 to Utils.createCheckedSetting(
                context, CheckedSetting.ViewType.RADIO,
                "1 hour",
                null
            ),
            12 to Utils.createCheckedSetting(
                context, CheckedSetting.ViewType.RADIO,
                "12 hours",
                null
            ),
            24 to Utils.createCheckedSetting(
                context, CheckedSetting.ViewType.RADIO,
                "24 hour",
                null
            ),
            72 to Utils.createCheckedSetting(
                context, CheckedSetting.ViewType.RADIO,
                "72 hour",
                null
            )
        )

        RadioManager(checkedSettings.values.toList()).apply {
            val time = settings.getInt(TIME_KEY, 1)
            val selected = checkedSettings[time]
            checkedSettings.forEach { entry ->
                entry.value.e {
                    a(entry.value)
                    settings.setInt(TIME_KEY, entry.key)
                }
                addView(entry.value)
            }
            a(selected)
        }
    }
}