package com.chdjalil.jalilgym.core.domain.nutrition

import com.chdjalil.jalilgym.core.model.HalalFilter
import com.chdjalil.jalilgym.core.model.HalalStatus

/**
 * Ingredient words that flag a product (docs/BLUEPRINT.md §3.7): pork, gelatin
 * of unknown origin and alcohol. Whole words only, so "hamburger" or
 * "graham" don't match "ham".
 */
private val FLAGGED_WORDS = listOf(
    "pork", "porc", "lard", "bacon", "ham", "prosciutto", "pancetta", "chorizo", "salami", "pepperoni",
    "gelatin", "gelatine", "gélatine", "e441",
    "alcohol", "ethanol", "wine", "beer", "rum", "brandy", "whisky", "whiskey", "vodka", "liqueur", "sherry", "cognac",
)

/** Phrases that clear a flagged word: the origin is stated or it's not drinking alcohol. */
private val CLEARING_PHRASES = listOf(
    "beef gelatin", "beef gelatine", "bovine gelatin", "fish gelatin", "fish gelatine", "halal gelatin", "halal gelatine",
    "sugar alcohol", "alcohol-free", "alcohol free", "non-alcoholic", "wine vinegar",
)

private val FLAGGED_LABELS = setOf("en:contains-pork", "en:contains-alcohol", "en:pork")

private val flaggedPattern = Regex(
    FLAGGED_WORDS.joinToString("|", prefix = "\\b(", postfix = ")\\b") { Regex.escape(it) },
    RegexOption.IGNORE_CASE,
)

/** True when a label tag (e.g. "en:halal", "fr:halal-certifie") says halal. */
private fun isHalalLabel(tag: String): Boolean = tag.substringAfter(':').startsWith("halal")

/**
 * The halal status of a product from its Open Food Facts label tags and
 * ingredient list. A halal label wins; otherwise any flagged ingredient flags
 * it; otherwise it's unknown. Never certified without a label.
 */
fun halalStatusOf(labelTags: List<String>, ingredientsText: String?): HalalStatus {
    if (labelTags.any(::isHalalLabel)) return HalalStatus.CERTIFIED
    if (labelTags.any { it.lowercase() in FLAGGED_LABELS }) return HalalStatus.FLAGGED
    var text = ingredientsText?.lowercase() ?: return HalalStatus.UNKNOWN
    CLEARING_PHRASES.forEach { text = text.replace(it, " ") }
    return if (flaggedPattern.containsMatchIn(text)) HalalStatus.FLAGGED else HalalStatus.UNKNOWN
}

fun HalalFilter.allows(status: HalalStatus): Boolean = when (this) {
    HalalFilter.ALL -> true
    HalalFilter.HIDE_FLAGGED -> status != HalalStatus.FLAGGED
    HalalFilter.CERTIFIED_ONLY -> status == HalalStatus.CERTIFIED || status == HalalStatus.USER_MARKED
}
