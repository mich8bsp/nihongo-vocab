package io.github.mich8bsp.nihongovocab.data

/**
 * Kana sits outside the JLPT chain (starts enabled alongside N5, no
 * auto-advance target of its own) - see [next]. CUSTOM (user-added "My
 * Vocabulary" words) sits outside it too, but is otherwise a pool like any
 * other - its own [PoolState] row, toggled and practiced the same way as
 * the JLPT levels; only its content is edited via a dedicated screen
 * instead of bundled assets.
 */
enum class Level {
    KANA, N5, N4, N3, N2, N1, CUSTOM;

    /** Pool auto-enabled when this one is fully mastered, if any. */
    fun next(): Level? = when (this) {
        KANA -> null
        N5 -> N4
        N4 -> N3
        N3 -> N2
        N2 -> N1
        N1 -> null
        CUSTOM -> null
    }
}
