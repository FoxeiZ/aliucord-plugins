package com.github.foxeiz

import android.widget.TextView
import com.aliucord.Logger
import com.aliucord.api.PatcherAPI
import com.aliucord.patcher.after
import com.aliucord.patcher.instead
import com.discord.utilities.view.text.LinkifiedTextView
import com.discord.widgets.chat.list.adapter.`WidgetChatListAdapterItemEmbed$1`

internal val logger: Logger = Logger("SelectableText.Patcher")

fun createSelectableEmbedDescription(patcher: PatcherAPI) {
    patcher.instead<`WidgetChatListAdapterItemEmbed$1`>("invoke", TextView::class.java) {
        val textView = it.args[0] as TextView

        if (textView is LinkifiedTextView) {
            textView.setTextIsSelectable(true)
            textView.setSelectAllOnFocus(false)
            textView.setOnLongClickListener { view ->
                logger.info("Long clicked on LinkifiedTextView: ${view.id}")
                false
            }
        }
    }

    patcher.after<LinkifiedTextView>(
        "setText",
        CharSequence::class.java,
        TextView.BufferType::class.java
    ) {
        (it.thisObject as LinkifiedTextView).movementMethod =
            android.text.method.LinkMovementMethod.getInstance()
    }
}
