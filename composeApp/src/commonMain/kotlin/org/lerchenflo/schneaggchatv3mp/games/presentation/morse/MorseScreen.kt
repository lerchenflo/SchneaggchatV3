package org.lerchenflo.schneaggchatv3mp.games.presentation.morse

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private const val DASH_THRESHOLD_MS = 200

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorseScreen(
    onBackClick: () -> Unit,
    viewModel: MorseViewModel
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Morse") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CodeDisplay(state = state)

            Spacer(modifier = Modifier.height(8.dp))

            HistoryRow(history = state.history)

            Spacer(modifier = Modifier.height(8.dp))

            MorseTreeView(
                currentCode = state.currentCode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            PressButton(
                onDot = viewModel::addDot,
                onDash = viewModel::addDash
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CodeDisplay(state: MorseState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        val displayCode = state.currentCode
            .replace(".", "·")
            .replace("-", "−")

        Text(
            text = if (state.invalid) "?" else displayCode.ifEmpty { "—" },
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                state.invalid -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            letterSpacing = 6.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = state.currentChar?.toString() ?: "",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HistoryRow(history: List<Char>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = history.joinToString(" "),
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            letterSpacing = 4.sp
        )
    }
}

@Composable
private fun PressButton(onDot: () -> Unit, onDash: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { _ ->
                        val mark = TimeSource.Monotonic.markNow()
                        val released = tryAwaitRelease()
                        if (released) {
                            val elapsed = mark.elapsedNow()
                            if (elapsed < DASH_THRESHOLD_MS.milliseconds) {
                                onDot()
                            } else {
                                onDash()
                            }
                        }
                    }
                )
            },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "·  /  −",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 4.sp
            )
        }
    }
}
