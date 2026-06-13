package core

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory

data class TempGameFixture(
    val root: Path,
    val plugins: Path,
    val docs: Path,
)

fun withTempGameFixture(block: (TempGameFixture) -> Unit) {
    val root = createTempDirectory("honkai-game-fixture")
    val plugins = root.resolve("honkai_rts").resolve("GamePlugins").createDirectories()
    val docs = root.resolve("honkai_rts").resolve("docs")

    block(
        TempGameFixture(
            root = root,
            plugins = plugins,
            docs = docs,
        )
    )
}
