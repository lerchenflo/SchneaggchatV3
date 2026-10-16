package org.lerchenflo.schneaggchatv3mp.games.presentation.cchallenge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Monospace style shared by the code block and the answer options. */
internal val CodeTextStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)

/** One row of a [CCodeBlock]. */
internal class CCodeRow(
    val text: String,
    /** Shown in the gutter; null hides the number (e.g. lines still to be ordered). */
    val number: Int? = null,
    val background: Color? = null,
    val onClick: (() -> Unit)? = null,
)

/**
 * Read-only C code with theme-colored syntax highlighting. Scrolls sideways instead of
 * wrapping, so indentation stays meaningful on narrow phones.
 */
@Composable
internal fun CCodeBlock(
    rows: List<CCodeRow>,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
    ) {
        BoxWithConstraints {
            val minWidth = maxWidth
            Column(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .widthIn(min = minWidth)
                    .width(IntrinsicSize.Max)
                    .padding(vertical = 8.dp)
            ) {
                rows.forEach { row ->
                    CodeRow(row = row, colors = colors)
                }
            }
        }
    }
}

@Composable
private fun CodeRow(row: CCodeRow, colors: ColorScheme) {
    val highlighted = remember(row.text, colors) { highlightC(row.text, colors) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (row.background != null) Modifier.background(row.background) else Modifier)
            .then(if (row.onClick != null) Modifier.clickable(onClick = row.onClick) else Modifier)
            .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.number?.toString().orEmpty(),
            style = CodeTextStyle,
            color = colors.outline,
            modifier = Modifier
                .width(32.dp)
                .padding(end = 8.dp),
            textAlign = TextAlign.End,
        )
        Text(
            text = highlighted,
            style = CodeTextStyle,
            softWrap = false,
        )
    }
}

private val C_KEYWORDS = setOf(
    "int", "long", "double", "float", "char", "void", "const", "unsigned",
    "return", "if", "else", "for", "while", "do", "switch", "case", "default", "break", "continue",
)

// Comments, (possibly unterminated) string and char literals, numbers and words
private val C_TOKEN = Regex("""//.*|"(?:\\.|[^"\\])*"?|'(?:\\.|[^'\\])*'?|\b\d+\b|\b[A-Za-z_]\w*\b""")

private fun highlightC(line: String, colors: ColorScheme): AnnotatedString = buildAnnotatedString {
    if (line.trimStart().startsWith("#")) {
        withStyle(SpanStyle(color = colors.secondary)) { append(line) }
        return@buildAnnotatedString
    }
    var last = 0
    C_TOKEN.findAll(line).forEach { match ->
        append(line.substring(last, match.range.first))
        val token = match.value
        val style = when {
            token.startsWith("//") -> SpanStyle(color = colors.outline)
            token.startsWith("\"") || token.startsWith("'") -> SpanStyle(color = colors.tertiary)
            token.first().isDigit() -> SpanStyle(color = colors.secondary)
            token in C_KEYWORDS -> SpanStyle(color = colors.primary, fontWeight = FontWeight.SemiBold)
            else -> null
        }
        if (style != null) withStyle(style) { append(token) } else append(token)
        last = match.range.last + 1
    }
    append(line.substring(last))
}
