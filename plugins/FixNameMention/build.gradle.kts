version = "1.0.0"
description =
    "Lie to the autocomplete comparator to prevent it from treating different objects as equal, which causes some mentions to not show up in the autocomplete list."

aliucord {
    changelog.set(
        """
        init plugin
        """.trimIndent()
    )

    deploy.set(true)
}

// android {
//     sourceSets {
//         getByName("main") {
//             kotlin.srcDir("stubs")
//         }
//     }
// }
