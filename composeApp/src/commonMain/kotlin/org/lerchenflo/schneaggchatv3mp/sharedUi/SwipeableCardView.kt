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
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_back
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_finish
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_left
import schneaggchatv3mp.composeapp.generated.resources.swipecardview_right

/**
 * Swipeable card view for Account creation etc
 */
@Composable
fun SwipeableCardView(
    onFinished: () -> Unit,
    onBack: () -> Unit,
    finishEnabled: Boolean,
    backEnabled: Boolean,
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
            verticalAlignment = contentAlignment,
            userScrollEnabled = true // Allow swiping between cards
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
                // Left Button - "Back" on first page, "Previous" on other pages
                TextButton(
                    onClick = {
                        if (pagerState.currentPage > 0) {
                            // Navigate to previous card
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        } else {
                            // On first page, trigger onBack callback
                            onBack()
                        }
                    },
                    enabled = if (pagerState.currentPage > 0) {
                        true // Always enabled for previous navigation
                    } else {
                        backEnabled // Use backEnabled parameter for first page
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage > 0) {
                            stringResource(Res.string.swipecardview_left)
                        } else {
                            stringResource(Res.string.swipecardview_back)
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Right Button - "Next" on middle pages, "Finish" on last page
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (pagerState.currentPage < scope.cards.size - 1) {
                                // Navigate to next card
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            } else {
                                // On last page, trigger onFinished callback
                                onFinished()
                            }
                        }
                    },
                    enabled = if (pagerState.currentPage == scope.cards.size - 1) {
                        finishEnabled // Use finishEnabled parameter on last page
                    } else {
                        true // Always enabled for next navigation
                    }
                ) {
                    Text(
                        text = if (pagerState.currentPage == scope.cards.size - 1) {
                            stringResource(Res.string.swipecardview_finish)
                        } else {
                            stringResource(Res.string.swipecardview_right)
                        },
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
    var canFinish by remember { mutableStateOf(false) }

    SwipeableCardView(
        onFinished = {
            println("Finished!")
            /* Navigate away */
        },
        onBack = {
            println("Back pressed from first page!")
            /* Navigate back */
        },
        finishEnabled = canFinish, // Control when finish button is enabled
        backEnabled = true, // Control when back button is enabled on first page
        modifier = Modifier.fillMaxSize(),
        content = {
            CardItem(
                content = {
                    Column {
                        Text("First Card", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("This is the first card content")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Try pressing 'Back' - it will call onBack()")
                    }
                }
            )

            CardItem(
                content = {
                    Column {
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
            )

            CardItem(
                content = {
                    Column {
                        Text("Final Card", style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("You're all done!")
                        Spacer(modifier = Modifier.height(8.dp))

                        // Example: Enable finish button when checkbox is checked
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = canFinish,
                                onCheckedChange = { canFinish = it }
                            )
                            Text("I agree to terms and conditions")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (canFinish) "Finish button is enabled!" else "Check the box to enable Finish",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (canFinish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    )
}