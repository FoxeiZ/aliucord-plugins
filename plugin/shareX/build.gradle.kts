version = "1.2.0" // Plugin version. Increment this to trigger the updater
description =
    "Upload >10mb file for non-nitro user." // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        feat: Add Litterbox upload support and refactor command system

        - Add Litterbox as new upload provider with configurable time limits (1h, 12h, 24h, 72h)
        - Implement dynamic command registration for upload providers
        - Refactor settings to use factory pattern for extra settings and commands
        - Extract utility functions to PluginUtils for text input creation
        - Rename CatboxSetting to CatboxSettings for consistency
        - Update UploadProcessor to use factory pattern for provider creation
        - Add CatboxCommands for provider-specific commands
        - Replace unused exception variable with underscore
        """.trimIndent()
    )
    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
    // author("Name", 0)
    // author("Name", 0)

    // Excludes this plugin from the updater, meaning it won't show up for users.
    // Set this if the plugin is unfinished
    excludeFromUpdaterJson.set(true)
}

android {
    sourceSets {
        getByName("main") {
            java.srcDirs("stubs")
        }
    }
}

dependencies {
    configurations.all {
        exclude(group = "com.discord", module = "media_picker")
        exclude(group = "com.discord", module = "app")
    }
}
