package com.github.foxeiz

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import com.aliucord.views.TextInput

class PluginUtils {
    companion object {
        fun createTextWatcher(
            afterTextCallback: ((Editable?) -> Unit),
            onTextCallback: ((CharSequence?, Int, Int, Int) -> Unit)? = null,
        ): TextWatcher {
            return object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    onTextCallback?.invoke(s, start, before, count)
                }

                override fun afterTextChanged(s: Editable?) = afterTextCallback(s)
            }
        }

        fun createTextInput(
            context: Context,
            view: ViewGroup,
            hint: String,
            initialText: String,
            textWatcher: TextWatcher,
            layoutParams: ViewGroup.LayoutParams? = null,
        ): TextInput {
            return TextInput(context, hint, initialText, textWatcher).apply {
                this.layoutParams = layoutParams ?: ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                view.addView(this)
            }
        }
    }
}