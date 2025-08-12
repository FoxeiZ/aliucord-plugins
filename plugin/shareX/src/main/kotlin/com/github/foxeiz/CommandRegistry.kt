package com.github.foxeiz

import com.aliucord.Utils
import com.aliucord.api.CommandsAPI
import com.discord.api.commands.ApplicationCommandType

class CommandRegistry(private val commands: CommandsAPI) {

    fun registerDebugCommands() {
        registerFindHookConstructorCommand()
        registerFindHookCommand()
    }

    private fun registerFindHookConstructorCommand() {
        val options = listOf(
            Utils.createCommandOption(
                ApplicationCommandType.STRING,
                "class",
                required = true,
            )
        )

        commands.registerCommand("find_hook_constructor", "", options) {
            val clazzName = it.getRequiredString("class")
            val result = StringBuilder()

            try {
                val clazz = Class.forName(clazzName)
                result.append("Found class ${clazz.name}\n")
                result.append("**Constructors**: ${clazz.declaredConstructors.size}\n")
                clazz.declaredConstructors.forEach { constructor ->
                    result.append(
                        "${constructor.name}(${constructor.parameterTypes.joinToString(", ") { it.simpleName }})\n"
                    )
                }
                CommandsAPI.CommandResult(result.toString(), null, false)
            } catch (e: ClassNotFoundException) {
                CommandsAPI.CommandResult("Class not found: $clazzName", null, false)
            }
        }
    }

    private fun registerFindHookCommand() {
        val options = listOf(
            Utils.createCommandOption(ApplicationCommandType.STRING, "class", required = true),
            Utils.createCommandOption(ApplicationCommandType.STRING, "class_name"),
            Utils.createCommandOption(ApplicationCommandType.STRING, "method_name"),
            Utils.createCommandOption(ApplicationCommandType.STRING, "field_name")
        )

        commands.registerCommand("find_hook", "", options) {
            val clazzName = it.getRequiredString("class")
            val className = it.getString("class_name")
            val methodName = it.getString("method_name")
            val fieldName = it.getString("field_name")

            val result = StringBuilder()

            try {
                val clazz = Class.forName(clazzName)

                when {
                    !className.isNullOrEmpty() -> findClasses(clazz, className, result)
                    !methodName.isNullOrEmpty() -> findMethods(clazz, methodName, result)
                    !fieldName.isNullOrEmpty() -> findFields(clazz, fieldName, result)
                    else -> findAll(clazz, result)
                }

                CommandsAPI.CommandResult(result.toString(), null, false)
            } catch (_: ClassNotFoundException) {
                CommandsAPI.CommandResult("Class not found: $clazzName", null, false)
            }
        }
    }

    private fun findClasses(clazz: Class<*>, className: String, result: StringBuilder) {
        val classes = clazz.declaredClasses.filter {
            it.simpleName.contains(className, ignoreCase = true)
        }
        result.append("Found ${classes.size} classes in ${clazz.name}:\n")
        classes.forEach { innerClass ->
            result.append("${innerClass.name}\n")
        }
    }

    private fun findMethods(clazz: Class<*>, methodName: String, result: StringBuilder) {
        val methods = clazz.declaredMethods.filter {
            it.name.contains(methodName, ignoreCase = true)
        }
        result.append("Found ${methods.size} methods in class ${clazz.name}:\n")
        methods.forEach { method ->
            result.append(
                "${method.returnType.simpleName} ${method.name}(${
                    method.parameterTypes.joinToString(", ") { it.simpleName }
                })\n"
            )
        }
    }

    private fun findFields(clazz: Class<*>, fieldName: String, result: StringBuilder) {
        val fields = clazz.declaredFields.filter {
            it.name.contains(fieldName, ignoreCase = true)
        }
        result.append("Found ${fields.size} fields in class ${clazz.name}:\n")
        fields.forEach { field ->
            result.append("${field.type.simpleName} ${field.name}\n")
        }
    }

    private fun findAll(clazz: Class<*>, result: StringBuilder) {
        result.append("Found class ${clazz.name}\n")
        result.append("**Methods**: ${clazz.declaredMethods.size}\n")
        clazz.declaredMethods.forEach { result.append("${it.name}\n") }
        result.append("**Classes**: ${clazz.declaredClasses.size}\n")
        clazz.declaredClasses.forEach { result.append("${it.name}\n") }
        result.append("**Fields**: ${clazz.declaredFields.size}\n")
        clazz.declaredFields.forEach { result.append("${it.name}\n") }
    }
}