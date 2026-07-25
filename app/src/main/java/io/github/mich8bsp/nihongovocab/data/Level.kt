package io.github.mich8bsp.nihongovocab.data

/**
 * Kana sits outside the JLPT chain (starts enabled alongside N5, no
 * auto-advance target of its own) - see [next].
 */
enum class Level {
    KANA, N5, N4, N3, N2, N1;

    /** Pool auto-enabled when this one is fully mastered, if any. */
    fun next(): Level? = when (this) {
        KANA -> null
        N5 -> N4
        N4 -> N3
        N3 -> N2
        N2 -> N1
        N1 -> null
    }
}
