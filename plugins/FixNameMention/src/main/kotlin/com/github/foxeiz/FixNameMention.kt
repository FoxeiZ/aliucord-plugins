package com.github.foxeiz

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.widgets.chat.input.autocomplete.Autocompletable
import com.discord.widgets.chat.input.autocomplete.AutocompletableComparator

@Suppress("unused")
@AliucordPlugin
class FixNameMention : Plugin() {
    override fun start(context: Context) {
        patcher.after<AutocompletableComparator>(
            "compare",
            Autocompletable::class.java,
            Autocompletable::class.java
        ) { param ->
            try {
                val result = param.result as Int

                if (result == 0) {
                    val objA = param.args[0]
                    val objB = param.args[1]
                    if (objA !== objB) {
                        var safeCompare =
                            System.identityHashCode(objA).compareTo(System.identityHashCode(objB))
                        if (safeCompare == 0) safeCompare = 1
                        param.result = safeCompare
                    }
                }
            } catch (e: Throwable) {
                logger.error("Error fixing autocomplete comparator", e)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
