version = "1.3.2" // Plugin version. Increment this to trigger the updater
description =
    "Upload >10mb file for non-nitro user." // Plugin description that will be shown to user

aliucord {
    // Changelog of your plugin
    changelog.set(
        """
        feat: migrate to new structure
        fix: handle upload failures better
        """.trimIndent()
    )
    // Image or Gif that will be shown at the top of your changelog page
    // changelogMedia.set("https://cool.png")

    // Add additional authors to this plugin
    // author("Name", 0)
    // author("Name", 0)

    // Excludes this plugin from the updater, meaning it won't show up for users.
    // Set this if the plugin is unfinished
    deploy.set(true)
}

// android {
//     sourceSets {
//         getByName("main") {
//             kotlin.srcDir("stubs")
//         }
//     }
// }
