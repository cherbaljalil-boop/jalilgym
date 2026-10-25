package com.chdjalil.jalilgym.core.uitest

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasClickAction

/** What a screen reader would announce for this node. */
private fun SemanticsNode.spokenLabel(): String {
    val c = config
    return listOfNotNull(
        c.getOrNull(SemanticsProperties.ContentDescription)?.joinToString(" "),
        c.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text },
        c.getOrNull(SemanticsProperties.EditableText)?.text,
        c.getOrNull(SemanticsActions.OnClick)?.label,
    ).joinToString(" ").trim()
}

/**
 * Fails if any tappable element has nothing for TalkBack to read: no text,
 * content description or click label (docs/BLUEPRINT.md §8, accessibility).
 */
fun SemanticsNodeInteractionsProvider.assertActionsHaveLabels() {
    val unlabeled = onAllNodes(hasClickAction()).fetchSemanticsNodes().filter { it.spokenLabel().isEmpty() }
    check(unlabeled.isEmpty()) {
        "Tappable elements without a label: " + unlabeled.joinToString { "#${it.id} at ${it.boundsInRoot}" }
    }
}
