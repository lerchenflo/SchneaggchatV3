package org.lerchenflo.schneaggchatv3mp.settings.presentation.uiElements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun QuotedTextPreview(){

    Column {
        Spacer(modifier = Modifier.height(20.dp))

        QuotedText(
            text = "Blalba you need to respect yourself to respect others etc fortnite",
            author = "— John Doe"
        )
    }
}

@Composable
fun QuotedText(
    text: String,
    author: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable{
                onClick()
            }
            .background(
                color = Color.Gray.copy(alpha = 0.15f),
            )
            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)

    ) {

        Text(
            text = "\"",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic
            ),
            color = Color.Gray.copy(alpha = 0.4f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                    .offset(y = (-15).dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            //Spacer (Qoute moved here with the offset modifier)
            Spacer(modifier = Modifier.height(20.dp))


            // Quote text
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )


            if (author.isNotEmpty()){
                // Author (with negative top padding to compensate for quote offset)
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    color = Color.Gray.copy(alpha = 0.7f),
                    modifier = Modifier
                        .padding(top = 8.dp)
                )
            }
        }
    }
}