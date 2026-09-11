package ru.nekostul.horizonos.ui.settings.launcher.scanning

import java.util.Locale

/**
 * Normalizes ROM file names and compares them with scraped titles.
 *
 * ROM names carry service tags that must never reach a search request or a
 * comparison: [RUS], (USA), [Rev 1], [v1.1], [!], [Disc 1] and similar.
 * The normalizer strips only recognized tags, so meaningful parts of the
 * title stay intact ("Final Fantasy VII [Disc 1]" -> "Final Fantasy VII").
 */
internal object TitleMatcher {

    /** Region / language tokens that may appear inside (...) tags. */
    private val regionTokens = setOf(
        "rus", "russian", "usa", "us", "u", "eur", "europe", "eu", "e",
        "jpn", "japan", "jp", "j", "pal", "ntsc", "ntscu", "ntscj",
        "world", "wor", "w", "kor", "korea", "chn", "china", "asia",
        "br", "brazil", "fra", "france", "fr", "ger", "germany", "de", "deu",
        "esp", "spain", "es", "ita", "italy", "it", "ned", "nl", "holland",
        "por", "portugal", "pt", "swe", "sweden", "se", "nor", "norway", "no",
        "den", "denmark", "dk", "fin", "finland", "fi", "pol", "poland", "pl",
        "cze", "cz", "hun", "hu", "ukr", "gre", "gr", "tur", "tr", "ara", "ar",
        "heb", "he", "eng", "english", "multi", "multi5", "multi6", "multi7",
        "multi8", "m5", "m6", "m7", "m8", "en"
    )

    /** Revision / build / release tokens. */
    private val statusTokens = setOf(
        "rev", "revision", "version", "ver", "beta", "proto", "prototype",
        "demo", "sample", "alpha", "final", "fixed", "fix", "patch", "unk",
        "unknown", "prerelease", "pre", "promo", "preview", "kiosk",
        "unlicensed", "aftermarket", "homebrew", "pd", "public", "domain",
        "hack", "translation", "trans", "fan", "sub", "dub", "eur", "usa"
    )

    /** Disc / media tokens. */
    private val discTokens = setOf(
        "disc", "disk", "cd", "dvd", "side", "tape", "cart", "part", "vol",
        "volume", "chapter"
    )

    private val versionPattern = Regex("^v\\d+(\\.\\d+)*$")
    private val romanPattern = Regex("^[ivx]{2,4}$")

    /**
     * Builds a clean search query from a raw ROM file name.
     * "Driver 2 [RUS].cue" -> "Driver 2"
     */
    fun searchQuery(romName: String): String {
        val withoutExtension = stripExtension(romName)
        val cleaned = withoutExtension
            .replace('_', ' ')
            .replace(Regex("\\[[^\\]]*\\]"), " ")
            .let { removeTaggedParentheses(it) }
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('-', '_', '.', ' ')
        return cleaned.ifBlank {
            withoutExtension.replace('_', ' ').trim()
        }
    }

    /** Comparison key: lowercase alphanumeric tokens, leading "the" removed. */
    fun normalize(value: String): List<String> {
        val tokens = value
            .lowercase(Locale.ROOT)
            .replace('&', ' ')
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
            .split(' ')
            .filter { it.isNotBlank() }
            .map { canonicalToken(it) }
        return if (tokens.firstOrNull() == "the") tokens.drop(1) else tokens
    }

    private fun canonicalToken(token: String): String {
        val roman = romanToInt(token)
        return roman?.toString() ?: token
    }

    /**
     * 100 when equal, 98 when equal ignoring order, otherwise a symmetric
     * token-coverage score with a penalty when numeric tokens differ. This
     * prevents "Driver 2" from matching a plain "Driver".
     */
    fun matchScore(query: String, candidate: String): Int {
        val a = normalize(query)
        val b = normalize(candidate)
        if (a.isEmpty() || b.isEmpty()) return 0
        if (a == b) return 100
        val setA = a.toSet()
        val setB = b.toSet()
        if (setA == setB) return 98

        val common = setA.intersect(setB)
        if (common.isEmpty()) return 0
        val coverageA = common.size.toDouble() / setA.size
        val coverageB = common.size.toDouble() / setB.size
        var score = (((coverageA + coverageB) / 2.0) * 100).toInt()

        val numbersA = a.filter { it.all(Char::isDigit) }.toSet()
        val numbersB = b.filter { it.all(Char::isDigit) }.toSet()
        if (numbersA != numbersB) score -= 35

        return score.coerceIn(0, 100)
    }

    /**
     * Picks the best candidate above [minScore]. Candidates that only share a
     * partial prefix are rejected by the numeric-mismatch penalty.
     */
    fun best(query: String, candidates: List<String>, minScore: Int = 70): Int? =
        candidates.indices
            .map { index -> index to matchScore(query, candidates[index]) }
            .filter { it.second >= minScore }
            .maxByOrNull { it.second }
            ?.first

    private fun stripExtension(name: String): String {
        val extension = name.substringAfterLast('.', "")
        val looksLikeExtension = extension.length in 1..4 && extension.all { it.isLetterOrDigit() }
        return if (looksLikeExtension && name.contains('.')) name.substringBeforeLast('.') else name
    }

    private fun removeTaggedParentheses(value: String): String =
        Regex("\\(([^)]*)\\)").replace(value) { match ->
            if (isServiceTag(match.groupValues[1])) " " else match.value
        }

    private fun isServiceTag(inner: String): Boolean {
        val tokens = inner.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return true
        return tokens.all { token ->
            token in regionTokens ||
                token in statusTokens ||
                token in discTokens ||
                versionPattern.matches(token) ||
                token.all { it.isDigit() }
        }
    }

    private fun romanToInt(token: String): Int? {
        if (!romanPattern.matches(token)) return null
        val values = mapOf('i' to 1, 'v' to 5, 'x' to 10)
        var total = 0
        var previous = 0
        for (char in token.reversed()) {
            val value = values[char] ?: return null
            if (value < previous) total -= value else total += value
            previous = value
        }
        return total.takeIf { it in 1..39 }
    }
}
