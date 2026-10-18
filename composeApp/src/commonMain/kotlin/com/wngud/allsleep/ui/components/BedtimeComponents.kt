package com.wngud.allsleep.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wngud.allsleep.ui.theme.Spacing

@Composable
fun BedtimePage(contentPadding: PaddingValues, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = modifier.fillMaxSize().padding(contentPadding).imePadding(),
        contentPadding = PaddingValues(horizontal = Spacing.large, vertical = Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.large),
    ) {
        item { Column(verticalArrangement = Arrangement.spacedBy(Spacing.large), content = content) }
    }
}

@Composable
fun BedtimeCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(Spacing.large), verticalArrangement = Arrangement.spacedBy(Spacing.medium), content = content)
    }
}

@Composable
fun BedtimePrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(onClick, modifier = modifier.heightIn(min = 52.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), contentPadding = PaddingValues(16.dp), content = content)
}

@Composable
fun BedtimeTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    TextButton(onClick, modifier = modifier.heightIn(min = 48.dp), enabled = enabled, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary), content = content)
}

@Composable
fun BedtimeSettingsRow(title: String, onClick: () -> Unit, modifier: Modifier = Modifier, supportingText: String? = null, trailingContent: (@Composable () -> Unit)? = null) {
    Surface(onClick, modifier = modifier.heightIn(min = 64.dp), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                supportingText?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            trailingContent?.invoke()
        }
    }
}

@Composable
fun BedtimeStatus(title: String, message: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    BedtimeCard(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        action?.invoke()
    }
}

@Composable
fun BedtimeTextField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, isError: Boolean = false) {
    OutlinedTextField(value, onValueChange, modifier = modifier, label = { Text(label) }, singleLine = true, isError = isError, shape = RoundedCornerShape(16.dp), colors = OutlinedTextFieldDefaults.colors(focusedLabelColor = MaterialTheme.colorScheme.secondary, focusedBorderColor = MaterialTheme.colorScheme.secondary, cursorColor = MaterialTheme.colorScheme.secondary))
}
