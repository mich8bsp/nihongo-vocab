package io.github.mich8bsp.nihongovocab.data

private val VOWELS = setOf('a', 'i', 'u', 'e', 'o')

/**
 * Maps a romaji mora/digraph to its hiragana. Built from the same syllables
 * `kana.json` teaches, plus contracted-sound (yoon) combos and the small set
 * of loanword-only combos (fa/fi/fe/fo, di, che, ...) that don't appear in
 * the kana chart itself but do appear in bundled vocab romaji.
 *
 * Long vowels need no special handling: this dataset's romaji spells them
 * out mora-by-mora exactly as the kana does (e.g. "gakkou" -> ga-k-ko-u, not
 * a macron), so converting greedily mora-by-mora reproduces them for free -
 * ponytail: verified by round-tripping every romaji value in the bundled
 * vocab JSON through this table with zero unmapped characters left over.
 */
private val MORA = mapOf(
    "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
    "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
    "sa" to "さ", "shi" to "し", "su" to "す", "se" to "せ", "so" to "そ",
    "ta" to "た", "chi" to "ち", "tsu" to "つ", "te" to "て", "to" to "と",
    "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
    "ha" to "は", "hi" to "ひ", "fu" to "ふ", "he" to "へ", "ho" to "ほ",
    "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
    "ya" to "や", "yu" to "ゆ", "yo" to "よ",
    "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
    "wa" to "わ", "wo" to "を", "n" to "ん",
    "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
    "za" to "ざ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
    "da" to "だ", "de" to "で", "do" to "ど",
    "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
    "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
    // contracted sounds (yoon)
    "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
    "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
    "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
    "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
    "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
    "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
    "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
    "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
    "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
    "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
    "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
    // loanword-only combos
    "fa" to "ふぁ", "fi" to "ふぃ", "fe" to "ふぇ", "fo" to "ふぉ",
    "di" to "でぃ", "du" to "どぅ", "ti" to "てぃ", "tu" to "とぅ",
    "che" to "ちぇ", "she" to "しぇ", "je" to "じぇ",
    "wi" to "うぃ", "we" to "うぇ",
    "va" to "ゔぁ", "vi" to "ゔぃ", "vu" to "ゔ", "ve" to "ゔぇ", "vo" to "ゔぉ",
).toSortedMap(compareByDescending<String> { it.length }.thenBy { it })

/** Converts one run of romaji letters (no spaces/punctuation) to hiragana. */
private fun convertRun(s: String): String {
    val out = StringBuilder()
    var i = 0
    while (i < s.length) {
        when {
            // "n'" disambiguates ん from the next mora (e.g. "ken'i" -> けんい, not けに)
            s[i] == 'n' && i + 1 < s.length && s[i + 1] == '\'' -> {
                out.append('ん'); i += 2
            }
            // sokuon before chi/cha/chu/cho is conventionally spelled "tch", not "cch"
            s[i] == 't' && s.startsWith("ch", i + 1) -> {
                out.append('っ'); i += 1
            }
            // doubled consonant -> small tsu (gemination), e.g. "kippu" -> きっぷ
            i + 1 < s.length && s[i] == s[i + 1] && s[i] !in VOWELS && s[i] != 'n' -> {
                out.append('っ'); i += 1
            }
            else -> {
                val match = MORA.keys.firstOrNull { s.startsWith(it, i) }
                if (match != null) {
                    out.append(MORA.getValue(match)); i += match.length
                } else {
                    out.append(s[i]); i += 1
                }
            }
        }
    }
    return out.toString()
}

/**
 * Converts a romaji reading to hiragana, for the quiz-screen kana hint.
 * Non-letter characters (spaces, "; " between alternates, "~" prefixes,
 * parens) pass through unchanged so multi-alternate entries like
 * "koukou; koutougakkou" round-trip as "こうこう; こうとうがっこう".
 */
fun romajiToKana(romaji: String): String =
    Regex("[a-z']+").replace(romaji.lowercase()) { convertRun(it.value) }
