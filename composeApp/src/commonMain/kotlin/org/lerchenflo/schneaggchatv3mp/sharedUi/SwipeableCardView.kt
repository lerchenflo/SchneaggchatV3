import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_finish
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_left
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_right

@Composable
fun SwipeableCardView(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable CardContainerScope.() -> Unit
) {
    val scope = remember { CardContainerScopeImpl() }

    // Collect cards
    scope.content()

    val pagerState = rememberPagerState(pageCount = { scope.cards.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Content - Display current card
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            Box(modifier = Modifier.padding(16.dp)) {
                scope.cards[page]()
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage > 0) {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    },
                    enabled = pagerState.currentPage > 0
                ) {
                    Text(
                        text = stringResource(Res.string.swipecardview_left),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < scope.cards.size - 1) {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                onFinished()
                            }
                        }
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage == scope.cards.size - 1) stringResource(Res.string.swipecardview_finish) else stringResource(Res.string.swipecardview_right),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

interface CardContainerScope {
    @Composable
    fun CardItem(
        content: @Composable () -> Unit
    )
}

private class CardContainerScopeImpl : CardContainerScope {
    val cards = mutableListOf<@Composable () -> Unit>()

    @Composable
    override fun CardItem(
        content: @Composable () -> Unit
    ) {
        cards.add(content)
    }
}

@Preview
@Composable
private fun ExampleScreen() {
    SwipeableCardView(
        onFinished = { /* Navigate away */ },
        modifier = Modifier,
        content = {
            CardItem(
                content = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("First Card", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This is the first card content")
                        }
                    }
                }
            )

            CardItem(
                content = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Second Card", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            var text by remember { mutableStateOf("") }
                            OutlinedTextField(
                                value = text,
                                onValueChange = { text = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Enter text") }
                            )
                        }
                    }
                }
            )

            CardItem(
                content = {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Final Card", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You're all done!")
                        }
                    }
                }
            )
        }
    )
}