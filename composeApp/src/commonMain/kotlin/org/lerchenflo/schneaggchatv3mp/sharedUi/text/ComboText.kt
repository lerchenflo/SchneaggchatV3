package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.elements.MarkdownCheckBox
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.DefaultMarkdownAlertIcons
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.markdownAlertColors
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxThemes

/**
 * Displays text as plain text or markdown (merged into one composable) and resolves
 * inline annotations (`@<key>/<id>`, see [ComboAnnotationType]) into clickable names.
 *
 * Annotations whose id is not resolvable through [annotationSources] are still converted to
 * the source's placeholder name (e.g. "unknown user"), but rendered as plain, non-clickable
 * text - the raw `@<key>/<id>` never reaches the reader.
 */
@Composable
fun ComboText(
    text: String,
    useMD: Boolean,
    textColor: Color,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    annotationSources: List<ComboAnnotationSource> = rememberComboAnnotationSources()
) {
    if (useMD) {
        // Rewrite annotations to markdown links with a per-type fake scheme and intercept their clicks
        val markdownContent = remember(text, annotationSources) {
            val matches = findComboAnnotations(text, annotationSources)
            if (matches.isEmpty()) text
            else buildString {
                var last = 0
                matches.forEach { match ->
                    append(text, last, match.range.first)
                    val display = match.source.type.displayName(match.name)
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                    if (match.resolved) {
                        append("[").append(display).append("](")
                        append(match.source.type.linkScheme).append(match.id).append(")")
                    } else {
                        // No link at all - a dead target must not look or behave like a working mention
                        append(display)
                    }
                    last = match.range.last + 1
                }
                append(text, last, text.length)
            }
        }

        val defaultUriHandler = LocalUriHandler.current
        val uriHandler = remember(defaultUriHandler, annotationSources) {
            object : UriHandler {
                override fun openUri(uri: String) {
                    val source = annotationSources.firstOrNull { uri.startsWith(it.type.linkScheme) }
                    if (source != null) {
                        source.onClick(uri.removePrefix(source.type.linkScheme))
                    } else {
                        defaultUriHandler.openUri(uri)
                    }
                }
            }
        }

        // The syntax theme follows the code background, not the system: light text = dark background
        val darkCode = textColor.luminance() > 0.5f
        val highlightsBuilder = remember(darkCode) {
            Highlights.Builder().theme(SyntaxThemes.default(darkMode = darkCode))
        }

        CompositionLocalProvider(LocalUriHandler provides uriHandler) {
            Markdown(
                content = markdownContent,
                modifier = modifier,
                typography = markdownTypography(paragraph = style),
                // Chat messages: a single newline must stay a line break, not collapse to a
                // space like CommonMark soft breaks
                annotator = markdownAnnotator(config = markdownAnnotatorConfig(eolAsNewLine = true)),

                // Code text uses textColor, so every background is tinted from it: readable in
                // both bubble colors and all themes
                colors = DefaultMarkdownColors(
                    text = textColor,
                    inlineCodeBackground = textColor.copy(alpha = 0.15f),
                    dividerColor = textColor.copy(alpha = 0.3f),
                    tableBackground = textColor.copy(alpha = 0.05f),
                    codeBackground = textColor.copy(alpha = 0.12f),
                    alert = markdownAlertColors(darkTheme = isSystemInDarkTheme()),
                    highlightText = MaterialTheme.colorScheme.primary,
                    highlightBackground = MaterialTheme.colorScheme.primaryContainer,
                ),
                components = markdownComponents(
                    checkbox = { MarkdownCheckBox(it.content, it.node, it.typography.text) },
                    codeFence = {
                        MarkdownHighlightedCodeFence(
                            content = it.content,
                            node = it.node,
                            style = it.typography.code,
                            highlightsBuilder = highlightsBuilder,
                            showHeader = true,
                        )
                    },
                    codeBlock = {
                        MarkdownHighlightedCodeBlock(
                            content = it.content,
                            node = it.node,
                            style = it.typography.code,
                            highlightsBuilder = highlightsBuilder,
                            showHeader = true,
                        )
                    },
                ),
            )
        }
    } else {
        val annotatedText = remember(text, annotationSources) {
            buildAnnotatedString {
                var last = 0
                findComboAnnotations(text, annotationSources).forEach { match ->
                    append(text.substring(last, match.range.first))
                    val display = match.source.type.displayName(match.name)
                    if (match.resolved) {
                        withLink(
                            LinkAnnotation.Clickable(
                                tag = match.source.type.linkScheme + match.id,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )
                                ),
                                linkInteractionListener = { match.source.onClick(match.id) }
                            )
                        ) {
                            append(display)
                        }
                    } else {
                        // No link at all - a dead target must not look or behave like a working mention
                        append(display)
                    }
                    last = match.range.last + 1
                }
                append(text.substring(last))
            }
        }

        Text(
            text = annotatedText,
            color = textColor,
            style = style,
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier
        )
    }
}
