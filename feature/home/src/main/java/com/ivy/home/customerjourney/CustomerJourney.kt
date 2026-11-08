package com.ivy.home.customerjourney

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.domain.RootScreen
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.rootScreen
import com.ivy.legacy.ui.WalletPreview
import com.ivy.navigation.navigation
import com.ivy.ui.R
import kotlinx.collections.immutable.ImmutableList

@Composable
fun CustomerJourney(
    customerJourneyCards: ImmutableList<CustomerJourneyCardModel>,
    onDismiss: (CustomerJourneyCardModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ivyContext = ivyWalletCtx()
    val nav = navigation()
    // Check is added for Paparazzi Test where context is different
    if (LocalContext.current !is RootScreen) return
    val rootScreen = rootScreen()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (card in customerJourneyCards) {
            CustomerJourneyCard(
                cardData = card,
                onDismiss = { onDismiss(card) }
            ) {
                card.onAction(nav, ivyContext, rootScreen)
            }
        }
    }
}

@Composable
fun CustomerJourneyCard(
    cardData: CustomerJourneyCardModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCTA: () -> Unit,
) {
    Card(
        onClick = onCTA,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CardHeader(cardData, onDismiss)

            Text(
                text = cardData.description,
                style = MaterialTheme.typography.bodyMedium
            )

            if (cardData.cta != null) {
                Button(
                    onClick = onCTA,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("cta_prompt_${cardData.id}")
                ) {
                    Icon(
                        imageVector = cardData.ctaIcon,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize)
                    )
                    Text(
                        text = cardData.cta,
                        modifier = Modifier.padding(start = ButtonDefaults.IconSpacing)
                    )
                }
            }
        }
    }
}

@Composable
private fun CardHeader(cardData: CustomerJourneyCardModel, onDismiss: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = cardData.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium
        )

        if (cardData.hasDismiss) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.dismiss)
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCard() {
    WalletPreview {
        CustomerJourneyCard(
            cardData = CustomerJourneyCardsProvider.adjustBalanceCard(),
            onCTA = { },
            onDismiss = {}
        )
    }
}

/** For screenshot testing */
@Composable
fun CustomerJourneyUiTest(isDark: Boolean, modifier: Modifier = Modifier) {
    WalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        Column(
            modifier = modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomerJourneyCard(CustomerJourneyCardsProvider.adjustBalanceCard(), onDismiss = {}, onCTA = {})
            CustomerJourneyCard(CustomerJourneyCardsProvider.addPlannedPaymentCard(), onDismiss = {}, onCTA = {})
        }
    }
}
