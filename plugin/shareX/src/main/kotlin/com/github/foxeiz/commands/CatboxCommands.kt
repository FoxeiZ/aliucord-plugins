package com.github.foxeiz.commands


import com.aliucord.api.CommandsAPI

class CatboxCommands() {

    companion object {

        fun registerAll(commands: CommandsAPI) {
            registerCatboxAnonCommand(commands)
            registerCatboxUserCommand(commands)
        }

        fun unregisterAll(commands: CommandsAPI) {
            commands.unregisterCommand("catbox_anon")
            commands.unregisterCommand("catbox_user")
        }

        fun registerCatboxAnonCommand(commands: CommandsAPI) {
            commands.registerCommand("catbox_anon", "Upload to Catbox anonymously") {
                CommandsAPI.CommandResult("Uploaded to Catbox anonymously", null, false)
            }
        }

        fun registerCatboxUserCommand(commands: CommandsAPI) {
            commands.registerCommand("catbox_user", "Upload to Catbox with user hash") {
                CommandsAPI.CommandResult("Uploaded to Catbox with user hash", null, false)
            }
        }
    }
}