package org.lerchenflo.schneaggchatv3mp.groupextensions.presentation.split

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import org.lerchenflo.schneaggchatv3mp.app.theme.SchneaggchatTheme
import org.lerchenflo.schneaggchatv3mp.sharedUi.buttons.CountSegmentedSwitch
import org.lerchenflo.schneaggchatv3mp.sharedUi.core.ActivityTitle
import schneaggchatv3mp.composeapp.generated.resources.Res
import schneaggchatv3mp.composeapp.generated.resources.group_extension_not_found
import schneaggchatv3mp.composeapp.generated.resources.split_add_expense
import schneaggchatv3mp.composeapp.generated.resources.split_balances_settled
import schneaggchatv3mp.composeapp.generated.resources.split_how_to_balance
import schneaggchatv3mp.composeapp.generated.resources.split_mark_as_paid
import schneaggchatv3mp.composeapp.generated.resources.split_my_total
import schneaggchatv3mp.composeapp.generated.resources.split_no_expenses
import schneaggchatv3mp.composeapp.generated.resources.split_owes
import schneaggchatv3mp.composeapp.generated.resources.split_paid_by
import schneaggchatv3mp.composeapp.generated.resources.split_tab_balances
import schneaggchatv3mp.composeapp.generated.resources.split_tab_expenses
import schneaggchatv3mp.composeapp.generated.resources.split_total_expenses
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_label
import schneaggchatv3mp.composeapp.generated.resources.split_transfer_title

@Composable
fun SplitScreenRoot(
    extensionId: String,
) {
    val viewModel = koinViewModel<SplitViewModel> {
        parametersOf(extensionId)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    SplitScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

/** Tricount style: expenses (with totals and an add button) and balances (with settle-up suggestions) as two tabs. */
@Composable
fun SplitScreen(
    state: SplitState,
    onAction: (SplitAction) -> Unit,
) {
    val showExpenses = state.selectedTab == SplitTab.EXPENSES && !state.isLoading && !state.extensionMissing

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The app's root Scaffold already pads for system bars and keyboard
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                ActivityTitle(
                    title = state.title,
                    onBackClick = { onAction(SplitAction.OnBackClick) }
                )
                if (!state.isLoading && !state.extensionMissing) {
                    CountSegmentedSwitch(
                        options = SplitTab.entries,
                        selected = state.selectedTab,
                        label = { tab ->
                            when (tab) {
                                SplitTab.EXPENSES -> stringResource(Res.string.split_tab_expenses)
                                SplitTab.BALANCES -> stringResource(Res.string.split_tab_balances)
                            }
                        },
                        count = { tab -> if (tab == SplitTab.EXPENSES) state.expenseCount else null },
                        onSelect = { onAction(SplitAction.OnTabSelected(it)) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (showExpenses) {
                SplitTotalsBar(
                    myTotal = state.myTotal,
                    totalExpenses = state.totalExpenses
                )
            }
        },
        floatingActionButton = {
            if (showExpenses) {
                ExtendedFloatingActionButton(
                    onClick = { onAction(SplitAction.OnAddExpenseClick) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(Res.string.split_add_expense)) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                state.extensionMissing -> CenteredMessage(text = stringResource(Res.string.group_extension_not_found))

                state.selectedTab == SplitTab.EXPENSES -> ExpenseList(
                    sections = state.expenseSections,
                    onExpenseClick = { onAction(SplitAction.OnExpenseClick(it)) }
                )

                else -> BalanceList(
                    balances = state.balances,
                    settlements = state.settlements,
                    onMarkAsPaidClick = { onAction(SplitAction.OnMarkAsPaidClick(it)) }
                )
            }
        }
    }
}

// ─── Expenses tab ─────────────────────────────────────────────────────────────

@Composable
private fun ExpenseList(
    sections: List<SplitExpenseSectionUi>,
    onExpenseClick: (String) -> Unit,
) {
    if (sections.isEmpty()) {
        CenteredMessage(
            text = stringResource(Res.string.split_no_expenses),
            icon = Icons.AutoMirrored.Filled.ReceiptLong
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // Room for the extended FAB above the last row
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        sections.forEach { section ->
            item(key = "day-${section.dateText}") {
                Text(
                    text = section.dateText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
            }
            items(items = section.expenses, key = { it.id }) { expense ->
                ExpenseRow(
                    expense = expense,
                    onClick = { onExpenseClick(expense.id) }
                )
            }
        }
    }
}

@Composable
private fun ExpenseRow(
    expense: SplitExpenseUi,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        leadingContent = {
            when {
                expense.isTransfer -> RoundIcon(icon = Icons.Default.SwapHoriz)
                expense.emoji != null -> RoundEmoji(emoji = expense.emoji)
                else -> RoundIcon(icon = Icons.AutoMirrored.Filled.ReceiptLong)
            }
        },
        headlineContent = {
            Text(
                text = if (expense.isTransfer) {
                    stringResource(Res.string.split_transfer_title, expense.paidByName, expense.receiverName)
                } else expense.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (expense.isTransfer) {
                    stringResource(Res.string.split_transfer_label)
                } else stringResource(Res.string.split_paid_by, expense.paidByName),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                text = expense.amountText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun SplitTotalsBar(
    myTotal: String,
    totalExpenses: String,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                TotalColumn(
                    label = stringResource(Res.string.split_my_total),
                    value = myTotal,
                    alignment = Alignment.Start,
                    modifier = Modifier.weight(1f)
                )
                TotalColumn(
                    label = stringResource(Res.string.split_total_expenses),
                    value = totalExpenses,
                    alignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TotalColumn(
    label: String,
    value: String,
    alignment: Alignment.Horizontal,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = alignment
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Balances tab ─────────────────────────────────────────────────────────────

@Composable
private fun BalanceList(
    balances: List<SplitBalanceUi>,
    settlements: List<SplitSettlementUi>,
    onMarkAsPaidClick: (SplitSettlementUi) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items = balances, key = { "balance-${it.userId}" }) { balance ->
            BalanceRow(balance = balance)
        }

        item(key = "settle-header") {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(Res.string.split_how_to_balance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            )
        }

        if (settlements.isEmpty()) {
            item(key = "settled") {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Text(
                        text = stringResource(Res.string.split_balances_settled),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(items = settlements, key = { "settle-${it.fromUserId}-${it.toUserId}" }) { settlement ->
                SettlementRow(
                    settlement = settlement,
                    onMarkAsPaidClick = { onMarkAsPaidClick(settlement) }
                )
            }
        }
    }
}

/**
 * Name and signed amount above a bar that grows from the center: to the left in the error color
 * for people who owe money, to the right in the primary color for people who get money back.
 */
@Composable
private fun BalanceRow(balance: SplitBalanceUi) {
    val amountColor = when {
        balance.balanceCents > 0L -> MaterialTheme.colorScheme.primary
        balance.balanceCents < 0L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = balance.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = balance.amountText,
                style = MaterialTheme.typography.titleMedium,
                color = amountColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (balance.balanceCents < 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxWidth(balance.barFraction)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (balance.balanceCents > 0L) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxWidth(balance.barFraction)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettlementRow(
    settlement: SplitSettlementUi,
    onMarkAsPaidClick: () -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
        leadingContent = { RoundIcon(icon = Icons.Default.SwapHoriz) },
        headlineContent = {
            Text(text = stringResource(Res.string.split_owes, settlement.fromName, settlement.toName))
        },
        supportingContent = {
            Text(
                text = settlement.amountText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        trailingContent = {
            FilledTonalButton(onClick = onMarkAsPaidClick) {
                Text(stringResource(Res.string.split_mark_as_paid))
            }
        }
    )
}

// ─── Shared pieces ────────────────────────────────────────────────────────────

@Composable
private fun RoundIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** Same circle as [RoundIcon], showing the emoji the user picked for an expense. */
@Composable
private fun RoundEmoji(emoji: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1
        )
    }
}

@Composable
private fun CenteredMessage(
    text: String,
    icon: ImageVector? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

private val previewState = SplitState(
    title = "Italien 2026",
    expenseSections = listOf(
        SplitExpenseSectionUi(
            dateText = "16.10.2026",
            expenses = listOf(
                SplitExpenseUi(id = "1", emoji = "🍕", title = "Pizza Margherita", amountText = "48,00 €", paidByName = "Fabi (you)", isTransfer = false, receiverName = ""),
                SplitExpenseUi(id = "2", emoji = null, title = "", amountText = "20,00 €", paidByName = "Anna", isTransfer = true, receiverName = "Fabi (you)"),
            )
        ),
        SplitExpenseSectionUi(
            dateText = "15.10.2026",
            expenses = listOf(
                SplitExpenseUi(id = "3", emoji = null, title = "Tanken", amountText = "92,40 €", paidByName = "Ben", isTransfer = false, receiverName = ""),
            )
        ),
    ),
    expenseCount = 3,
    myTotal = "46,80 €",
    totalExpenses = "140,40 €",
    balances = listOf(
        SplitBalanceUi(userId = "me", name = "Fabi (you)", balanceCents = 1200, amountText = "+12,00 €", barFraction = 0.3f),
        SplitBalanceUi(userId = "anna", name = "Anna", balanceCents = -4080, amountText = "-40,80 €", barFraction = 1f),
        SplitBalanceUi(userId = "ben", name = "Ben", balanceCents = 2880, amountText = "+28,80 €", barFraction = 0.7f),
    ),
    settlements = listOf(
        SplitSettlementUi(fromUserId = "anna", fromName = "Anna", toUserId = "ben", toName = "Ben", amountCents = 2880, amountText = "28,80 €"),
        SplitSettlementUi(fromUserId = "anna", fromName = "Anna", toUserId = "me", toName = "Fabi (you)", amountCents = 1200, amountText = "12,00 €"),
    ),
    isLoading = false,
)

@Preview(showBackground = true)
@Composable
private fun SplitScreenPreview() {
    SchneaggchatTheme {
        SplitScreen(
            state = previewState,
            onAction = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplitScreenBalancesPreview() {
    SchneaggchatTheme {
        SplitScreen(
            state = previewState.copy(selectedTab = SplitTab.BALANCES),
            onAction = {}
        )
    }
}
