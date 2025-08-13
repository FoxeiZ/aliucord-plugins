version = "1.1.0" // Plugin version. Increment this to trigger the updater
description =
    "Upload >10mb file for non-nitro user." // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        feat: implement file upload functionality with Catbox integration
        feat: add attachment utilities for file handling
        feat: create notification helper for upload progress
        feat: establish command registry for debug commands
        refactor: enhance plugin settings with new upload options
        refactor: update file hosting service interface for better extension support
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
