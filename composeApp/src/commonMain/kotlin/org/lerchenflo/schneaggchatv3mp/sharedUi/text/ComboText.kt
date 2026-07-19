package org.lerchenflo.schneaggchatv3mp.sharedUi.text

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors

/**
 * Displays text as plain text or markdown (merged into one composable) and resolves
 * inline annotations (`@<key>/<id>`, see [ComboAnnotationType]) into clickable names.
 *
 * Annotations whose id is not resolvable through [annotationSources] stay raw text.
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
                    val linkText = match.source.type.displayName(match.name)
                        .replace("[", "\\[")
                        .replace("]", "\\]")
                    append("[").append(linkText).append("](")
                    append(match.source.type.linkScheme).append(match.id).append(")")
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

        CompositionLocalProvider(LocalUriHandler provides uriHandler) {
            Markdown(
                content = markdownContent,
                modifier = modifier,

                colors = DefaultMarkdownColors(
                    text = textColor,
                    inlineCodeBackground = MaterialTheme.colorScheme.error,
                    dividerColor = MaterialTheme.colorScheme.onPrimary,
                    tableBackground = MaterialTheme.colorScheme.onSurface,
                    codeBackground = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        }
    } else {
        val annotatedText = remember(text, annotationSources) {
            buildAnnotatedString {
                var last = 0
                findComboAnnotations(text, annotationSources).forEach { match ->
                    append(text.substring(last, match.range.first))
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
                        append(match.source.type.displayName(match.name))
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
