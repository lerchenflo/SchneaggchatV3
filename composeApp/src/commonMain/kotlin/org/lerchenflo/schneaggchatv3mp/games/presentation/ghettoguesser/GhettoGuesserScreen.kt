package org.lerchenflo.schneaggchatv3mp.games.presentation.ghettoguesser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.lerchenflo.schneaggchatv3mp.games.domain.CommonsPhoto
import org.lerchenflo.schneaggchatv3mp.games.domain.GHETTO_GUESSER_MAX_ROUND_POINTS
import org.lerchenflo.schneaggchatv3mp.games.domain.GHETTO_GUESSER_ROUNDS
import org.lerchenflo.schneaggchatv3mp.games.presentation.formatGameTime
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import org.lerchenflo.schneaggchatv3mp.sharedUi.rememberZoomState
import org.lerchenflo.schneaggchatv3mp.sharedUi.zoomable
import org.lerchenflo.schneaggchatv3mp.utilities.formatDistance
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_back_to_maps
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_choose_map
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_close
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_credits_author
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_credits_file_page
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_credits_license
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_credits_title
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_credits_unchanged
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_game_over
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_loading
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_map_info
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_next_round
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_no_network
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_no_photos
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_open_map
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_photo_credit
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_place_pin
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_play_again
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_retry
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_round
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_round_result
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_show_photo
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_show_total
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_skip
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_submit
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_title
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_total_points
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_total_time
import schneaggchatv3mp.composeapp.generated.resources.games_ghettoguesser_unknown_author

@Composable
fun GhettoGuesserScreenRoot(
    onBackClick: () -> Unit,
) {
    val viewmodel = koinViewModel<GhettoGuesserViewModel>()
    val state by viewmodel.state.collectAsStateWithLifecycle()

    GhettoGuesserScreen(
        state = state,
        onAction = viewmodel::onAction,
        onBackClick = onBackClick,
    )
}

@Composable
fun GhettoGuesserScreen(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
    onBackClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ActivityTitle(
            title = state.selectedMap?.name ?: stringResource(Res.string.games_ghettoguesser_title),
            onBackClick = {
                if (state.phase == GhettoGuesserPhase.MAP_SELECTION) onBackClick()
                else onAction(GhettoGuesserAction.BackToMaps)
            },
        )

        when (state.phase) {
            GhettoGuesserPhase.MAP_SELECTION -> MapSelection(state, onAction)
            GhettoGuesserPhase.LOADING -> Loading(state)
            GhettoGuesserPhase.LOAD_FAILED -> LoadFailed(state, onAction)
            GhettoGuesserPhase.GUESSING -> Guessing(state, onAction)
            GhettoGuesserPhase.ROUND_RESULT -> RoundResult(state, onAction)
            GhettoGuesserPhase.GAME_OVER -> GameOver(state, onAction)
        }
    }

    val photo = state.photo
    if (state.isCreditsVisible && photo != null) {
        CreditsDialog(
            photo = photo,
            onDismiss = { onAction(GhettoGuesserAction.ShowCredits(false)) },
        )
    }
}

@Composable
private fun MapSelection(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(Res.string.games_ghettoguesser_choose_map),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        items(state.maps, key = { it.id }) { map ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { onAction(GhettoGuesserAction.SelectMap(map)) },
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null)
                    Column {
                        Text(text = map.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = stringResource(Res.string.games_ghettoguesser_map_info, GHETTO_GUESSER_ROUNDS),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundHeader(state: GhettoGuesserState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.games_ghettoguesser_round, state.round, GHETTO_GUESSER_ROUNDS),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = state.totalPoints.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun Loading(state: GhettoGuesserState) {
    RoundHeader(state)
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text = stringResource(Res.string.games_ghettoguesser_loading))
    }
}

@Composable
private fun LoadFailed(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(
                if (state.loadError == PhotoLoadError.NO_PHOTOS) Res.string.games_ghettoguesser_no_photos
                else Res.string.games_ghettoguesser_no_network
            ),
            textAlign = TextAlign.Center,
        )
        Button(onClick = { onAction(GhettoGuesserAction.RetryLoad) }) {
            Text(text = stringResource(Res.string.games_ghettoguesser_retry))
        }
        TextButton(onClick = { onAction(GhettoGuesserAction.BackToMaps) }) {
            Text(text = stringResource(Res.string.games_ghettoguesser_back_to_maps))
        }
    }
}

@Composable
private fun ColumnScope.Guessing(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
) {
    val map = state.selectedMap ?: return
    val photo = state.photo ?: return

    RoundHeader(state)
    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
        if (state.isGuessMapVisible) {
            GhettoGuesserMap(
                map = map,
                styleUrl = state.mapStyleUrl,
                guess = state.guess,
                actual = null,
                onMapClick = { onAction(GhettoGuesserAction.PlaceGuess(it)) },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            PhotoView(state = state, photo = photo)
        }
    }

    PhotoCredit(photo = photo, onClick = { onAction(GhettoGuesserAction.ShowCredits(true)) })

    if (state.isGuessMapVisible) {
        if (state.guess == null) {
            Text(
                text = stringResource(Res.string.games_ghettoguesser_place_pin),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                textAlign = TextAlign.Center,
            )
        }
        ButtonRow {
            OutlinedButton(
                onClick = { onAction(GhettoGuesserAction.ShowGuessMap(false)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(Res.string.games_ghettoguesser_show_photo))
            }
            Button(
                onClick = { onAction(GhettoGuesserAction.SubmitGuess) },
                enabled = state.guess != null,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(Res.string.games_ghettoguesser_submit))
            }
        }
    } else {
        ButtonRow {
            OutlinedButton(
                onClick = { onAction(GhettoGuesserAction.SkipPhoto) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(Res.string.games_ghettoguesser_skip))
            }
            Button(
                onClick = { onAction(GhettoGuesserAction.ShowGuessMap(true)) },
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(Res.string.games_ghettoguesser_open_map))
            }
        }
    }
}

@Composable
private fun PhotoView(state: GhettoGuesserState, photo: CommonsPhoto) {
    key(photo.pageId) {
        val zoomState = rememberZoomState()
        // Fit, not crop: the license credit says the photo is shown unchanged
        AsyncImage(
            model = state.imageBytes,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .zoomable(zoomState),
        )
    }
}

@Composable
private fun ColumnScope.RoundResult(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
) {
    val map = state.selectedMap ?: return
    val result = state.lastResult ?: return

    RoundHeader(state)
    GhettoGuesserMap(
        map = map,
        styleUrl = state.mapStyleUrl,
        guess = result.guess,
        actual = result.photo.location,
        onMapClick = null,
        modifier = Modifier.fillMaxWidth().weight(1f),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(
                Res.string.games_ghettoguesser_round_result,
                formatDistance(result.distanceMeters),
                result.points,
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = result.photo.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        result.photo.description?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    PhotoCredit(photo = result.photo, onClick = { onAction(GhettoGuesserAction.ShowCredits(true)) })

    ButtonRow {
        Button(
            onClick = { onAction(GhettoGuesserAction.NextRound) },
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(
                    if (state.round >= GHETTO_GUESSER_ROUNDS) Res.string.games_ghettoguesser_show_total
                    else Res.string.games_ghettoguesser_next_round
                )
            )
        }
    }
}

@Composable
private fun GameOver(
    state: GhettoGuesserState,
    onAction: (GhettoGuesserAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.games_ghettoguesser_game_over),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(
                        Res.string.games_ghettoguesser_total_points,
                        state.totalPoints,
                        GHETTO_GUESSER_ROUNDS * GHETTO_GUESSER_MAX_ROUND_POINTS,
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(Res.string.games_ghettoguesser_total_time, formatGameTime(state.totalTimeMillis)),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        itemsIndexed(state.results, key = { _, result -> result.photo.pageId }) { index, result ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(Res.string.games_ghettoguesser_round, index + 1, GHETTO_GUESSER_ROUNDS),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    Text(
                        text = result.photo.title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            Res.string.games_ghettoguesser_round_result,
                            formatDistance(result.distanceMeters),
                            result.points,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { onAction(GhettoGuesserAction.PlayAgain) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.games_ghettoguesser_play_again))
                }
                OutlinedButton(
                    onClick = { onAction(GhettoGuesserAction.BackToMaps) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(Res.string.games_ghettoguesser_back_to_maps))
                }
            }
        }
    }
}

@Composable
private fun PhotoCredit(photo: CommonsPhoto, onClick: () -> Unit) {
    Text(
        text = stringResource(
            Res.string.games_ghettoguesser_photo_credit,
            photo.author ?: stringResource(Res.string.games_ghettoguesser_unknown_author),
            photo.licenseName,
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textDecoration = TextDecoration.Underline,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}

@Composable
private fun CreditsDialog(photo: CommonsPhoto, onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.games_ghettoguesser_credits_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(
                        Res.string.games_ghettoguesser_credits_author,
                        photo.author ?: stringResource(Res.string.games_ghettoguesser_unknown_author),
                    )
                )
                val licenseUrl = photo.licenseUrl
                Text(
                    text = stringResource(Res.string.games_ghettoguesser_credits_license, photo.licenseName),
                    color = if (licenseUrl != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (licenseUrl != null) TextDecoration.Underline else null,
                    modifier = if (licenseUrl != null) Modifier.clickable { uriHandler.openUri(licenseUrl) } else Modifier,
                )
                Text(
                    text = stringResource(Res.string.games_ghettoguesser_credits_file_page),
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { uriHandler.openUri(photo.filePageUrl) },
                )
                Text(
                    text = stringResource(Res.string.games_ghettoguesser_credits_unchanged),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.games_ghettoguesser_close))
            }
        },
    )
}

@Composable
private fun ButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}
