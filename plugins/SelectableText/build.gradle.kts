version = "0.2.0"
description =
    "Make every text selectable!"

aliucord {
    changelog.set(
        """
        feat(SelectableText): add selectable text for channel
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
