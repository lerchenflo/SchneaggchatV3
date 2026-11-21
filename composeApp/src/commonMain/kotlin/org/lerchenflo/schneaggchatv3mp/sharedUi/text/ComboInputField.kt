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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Text input that supports inline annotations (see [ComboAnnotationType]): typing `@` and a name
 * suggests matching names of every type in [annotationSources] (a type's trigger, e.g. `@map`,
 * narrows to that type, see [findAnnotationSuggestions]); selecting a suggestion inserts the raw
 * annotation (`@<key>/<id>`) into the text, while the field renders it as the name and edits it as
 * one token ([keepAnnotationsWhole]). A typed name that was not picked stays plain text.
 *
 * The raw annotated text is kept in [value] — callers send/store it unchanged.
 *
 * @param contextIds ids suggested first, e.g. the members of the chat the field is in.
 */
@Composable
fun ComboInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    annotationSources: List<ComboAnnotationSource> = rememberComboAnnotationSources(),
    contextIds: Set<String> = emptySet(),
    placeholder: @Composable (() -> Unit)? = null,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    singleLine: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    val suggestionState = remember(value, annotationSources, contextIds) {
        findAnnotationSuggestions(value, annotationSources, contextIds)
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
                                text = suggestion.label,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        val annotationColor = MaterialTheme.colorScheme.primary
        val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant
        val visualTransformation = remember(annotationSources, annotationColor, placeholderColor) {
            ComboAnnotationVisualTransformation(annotationSources, annotationColor, placeholderColor)
        }

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(keepAnnotationsWhole(value, it, annotationSources)) },
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

/**
 * Renders every annotation in the field as its highlighted display name (an unknown id as the
 * grey placeholder, like [ComboText]) while the underlying (raw) text keeps the annotation.
 * Cursor positions inside an annotation snap to its end so it behaves like a single token.
 */
class ComboAnnotationVisualTransformation(
    private val sources: List<ComboAnnotationSource>,
    private val annotationColor: Color,
    private val placeholderColor: Color
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
                withStyle(
                    if (match.resolved) SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold)
                    else SpanStyle(color = placeholderColor, fontStyle = FontStyle.Italic)
                ) {
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
