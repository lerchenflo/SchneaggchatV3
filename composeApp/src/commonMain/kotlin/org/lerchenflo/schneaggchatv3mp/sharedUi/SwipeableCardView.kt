import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_finish
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_left
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_right

/**
 * Swipeable card view for Account creation etc
 */
@Composable
fun SwipeableCardView(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment.Vertical = Alignment.CenterVertically,
    cardElevation: Dp = 16.dp,
    content: @Composable CardContainerScope.() -> Unit
) {

    val scope = remember { CardContainerScopeImpl() }

    // Collect cards
    scope.content()

    val pagerState = rememberPagerState(pageCount = { scope.cards.size })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
    ) {
        // Content - Display current card
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalAlignment = contentAlignment
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val item = scope.cards[page]

                Card(
                    modifier = Modifier.fillMaxWidth().then(item.modifier),
                    elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        scope.cards[page].content()
                    }
                }
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
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    )
}

private data class CardItemData(
    val modifier: Modifier,
    val content: @Composable () -> Unit
)

private class CardContainerScopeImpl : CardContainerScope {
    val cards = mutableListOf<CardItemData>()

    @Composable
    override fun CardItem(
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        cards.add(CardItemData(modifier, content))
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