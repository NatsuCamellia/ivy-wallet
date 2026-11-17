package com.ivy.poll.impl.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ivy.poll.impl.ui.PollUi

@Composable
fun VoteCard(
  poll: PollUi,
  selectedIndex: Int?,
  onOptionClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(all = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Text(
        text = poll.title,
        style = MaterialTheme.typography.bodyLargeEmphasized,
      )
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        poll.options.forEachIndexed { index, option ->
          PollOption(
            option = option,
            selected = index == selectedIndex,
            onClick = {
              onOptionClick(index)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun PollOption(
  option: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .clickable(onClick = onClick)
      .defaultMinSize(minHeight = 48.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    RadioButton(
      selected = selected,
      onClick = null,
    )
    Text(
      text = option,
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}
