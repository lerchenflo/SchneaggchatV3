package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Text input that supports inline annotations (see [ComboAnnotationType]): typing a type's
 * trigger (e.g. `@map`) opens name autocomplete over the matching [annotationSources],
 * selecting a suggestion inserts the raw annotation (`@<key>/<id>`) into the text, while
 * the field visually renders it as the name.
 *
 * The raw annotated text is kept in [value] — callers send/store it unchanged.
 */
@Composable
fun ComboInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    annotationSources: List<ComboAnnotationSource> = rememberComboAnnotationSources(),
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val suggestionState = remember(value, annotationSources) {
        findAnnotationSuggestions(value, annotationSources)
    }

    Column(modifier = modifier) {
        if (suggestionState != null && suggestionState.suggestions.isNotEmpty()) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    suggestionState.suggestions.forEach { suggestion ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onValueChange(applySuggestion(value, suggestionState, suggestion))
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = when (suggestion) {
                                    is AnnotationSuggestion.Type ->
                                        suggestion.source.type.displayPrefix + suggestion.source.type.trigger
                                    is AnnotationSuggestion.Name ->
                                        suggestion.source.type.displayName(suggestion.name)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        val annotationColor = MaterialTheme.colorScheme.primary
        val visualTransformation = remember(annotationSources, annotationColor) {
            ComboAnnotationVisualTransformation(annotationSources, annotationColor)
        }

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            shape = shape,
            modifier = Modifier.fillMaxWidth(),
            placeholder = placeholder,
            label = label,
            isError = isError,
            singleLine = singleLine,
            visualTransformation = visualTransformation
        )
    }
}

private sealed interface AnnotationSuggestion {
    /** An available annotation type ("@ function"), shown while only `@`/a partial trigger is typed. */
    data class Type(val source: ComboAnnotationSource) : AnnotationSuggestion

    /** A concrete entity of a type whose trigger is fully typed. */
    data class Name(
        val source: ComboAnnotationSource,
        val id: String,
        val name: String
    ) : AnnotationSuggestion
}

private data class AnnotationSuggestionState(
    /** Index of the `@` that started the token. */
    val tokenStart: Int,
    /** Cursor position; the query is the text between tokenStart+1 and cursor. */
    val cursor: Int,
    val suggestions: List<AnnotationSuggestion>
)

private const val MAX_SUGGESTIONS = 5
private const val MAX_QUERY_LENGTH = 30

private fun findAnnotationSuggestions(
    value: TextFieldValue,
    sources: List<ComboAnnotationSource>
): AnnotationSuggestionState? {
    if (sources.isEmpty()) return null
    if (!value.selection.collapsed) return null
    val cursor = value.selection.start
    val textBefore = value.text.substring(0, cursor)
    val atIndex = textBefore.lastIndexOf('@')
    if (atIndex == -1) return null
    // Token must start at the beginning or after whitespace (rules out email addresses etc.)
    if (atIndex > 0 && !textBefore[atIndex - 1].isWhitespace()) return null
    val token = textBefore.substring(atIndex)
    if (token.contains('\n')) return null

    // Name autocomplete only opens once a type's trigger (e.g. "@map") is fully typed;
    // whatever follows the trigger filters the names
    val triggeredSources = sources.filter { token.startsWith(it.type.trigger, ignoreCase = true) }

    val suggestions = if (triggeredSources.isNotEmpty()) {
        triggeredSources
            .flatMap { source ->
                val query = token
                    .substring(source.type.trigger.length)
                    .trimStart('/', ' ')
                if (query.length > MAX_QUERY_LENGTH) return@flatMap emptyList()
                source.names.entries
                    .filter { it.value.contains(query, ignoreCase = true) }
                    .map { AnnotationSuggestion.Name(source, it.key, it.value) }
            }
            .sortedBy { it.name.lowercase() }
            .take(MAX_SUGGESTIONS)
    } else {
        // Only `@` or a partial trigger typed so far — offer the available @ functions
        sources.filter { it.type.trigger.startsWith(token, ignoreCase = true) }
            .map { AnnotationSuggestion.Type(it) }
    }

    return AnnotationSuggestionState(
        tokenStart = atIndex,
        cursor = cursor,
        suggestions = suggestions
    )
}

private fun applySuggestion(
    value: TextFieldValue,
    state: AnnotationSuggestionState,
    suggestion: AnnotationSuggestion
): TextFieldValue {
    val insertion = when (suggestion) {
        // Complete the trigger only — the name suggestions open right away
        is AnnotationSuggestion.Type -> suggestion.source.type.trigger
        is AnnotationSuggestion.Name -> suggestion.source.type.build(suggestion.id) + " "
    }
    val newText = value.text.replaceRange(state.tokenStart, state.cursor, insertion)
    return TextFieldValue(
        text = newText,
        selection = TextRange(state.tokenStart + insertion.length)
    )
}

/**
 * Renders every resolvable annotation in the field as its highlighted display name while the
 * underlying (raw) text keeps the annotation. Cursor positions inside an annotation snap to
 * its end so it behaves like a single token.
 */
class ComboAnnotationVisualTransformation(
    private val sources: List<ComboAnnotationSource>,
    private val annotationColor: Color
) : VisualTransformation {

    private data class Segment(
        val origStart: Int,
        val origEnd: Int,
        val transStart: Int,
        val transEnd: Int
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val segments = mutableListOf<Segment>()

        val transformed = buildAnnotatedString {
            var last = 0
            findComboAnnotations(original, sources).forEach { match ->
                append(original.substring(last, match.range.first))
                val display = match.source.type.displayName(match.name)
                val transStart = length
                withStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold)) {
                    append(display)
                }
                segments += Segment(
                    origStart = match.range.first,
                    origEnd = match.range.last + 1,
                    transStart = transStart,
                    transEnd = transStart + display.length
                )
                last = match.range.last + 1
            }
            append(original.substring(last))
        }

        if (segments.isEmpty()) return TransformedText(transformed, OffsetMapping.Identity)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var delta = 0
                for (segment in segments) {
                    if (offset <= segment.origStart) break
                    if (offset < segment.origEnd) return segment.transEnd
                    // transEnd already includes the shift of all earlier segments
                    delta = segment.transEnd - segment.origEnd
                }
                return offset + delta
            }

            override fun transformedToOriginal(offset: Int): Int {
                var delta = 0
                for (segment in segments) {
                    if (offset <= segment.transStart) break
                    if (offset < segment.transEnd) return segment.origEnd
                    delta = segment.origEnd - segment.transEnd
                }
                return offset + delta
            }
        }

        return TransformedText(transformed, offsetMapping)
    }
}
