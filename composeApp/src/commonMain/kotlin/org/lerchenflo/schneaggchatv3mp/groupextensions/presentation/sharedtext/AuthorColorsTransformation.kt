package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.sharedtext

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import org.lerchenflo.schneaggchatv3mp.groupextensions.domain.TextWriterRun

/** Strength of a writer's color behind their text, light enough to keep the text readable in light and dark mode. */
const val AUTHOR_COLOR_ALPHA = 0.35f

/**
 * Etherpad style author colors: paints a light background in each writer's color behind the
 * text they wrote. [writerRuns] cover the text the field shows; writers missing in [colorOf] stay plain.
 */
data class AuthorColorsTransformation(
    private val writerRuns: List<TextWriterRun>,
    private val colorOf: Map<String, Color>,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = buildAnnotatedString {
            append(text)
            var start = 0
            for (run in writerRuns) {
                if (start >= text.length) break
                val end = minOf(start + run.length, text.length)
                colorOf[run.writtenBy]?.let { color ->
                    addStyle(SpanStyle(background = color.copy(alpha = AUTHOR_COLOR_ALPHA)), start, end)
                }
                start = end
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}
