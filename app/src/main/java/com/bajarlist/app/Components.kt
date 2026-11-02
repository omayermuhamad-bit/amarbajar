package com.bajarlist.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

/** Upore date / mash dekhano ar age-pore jawar bar. */
@Composable
fun PeriodBar(
    label: String,
    todayLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onPick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "আগের")
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(enabled = onPick != null) { onPick?.invoke() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPick != null) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "পরের")
        }
        TextButton(onClick = onToday) { Text(todayLabel) }
    }
}

@Composable
fun ItemRow(
    item: BazarItem,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.bought)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.bought, onCheckedChange = { onToggle() })
            Text(
                text = item.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.bought) TextDecoration.LineThrough else null,
                color = if (item.bought)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (item.quantity > 0) {
                Text(
                    text = (formatQty(item.quantity) + " " + item.unit).bn(),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "পরিমাণ দিন",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "মুছুন",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛒", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/**
 * Item add / edit dialog.
 * Add korar somoy dialog khola thake jate ekshathe onek item add kora jay (keepOpen = true).
 */
@Composable
fun ItemDialog(
    initial: BazarItem?,
    suggestions: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, unit: String) -> Unit
) {
    val keepOpen = initial == null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var qty by remember {
        mutableStateOf(initial?.let { if (it.quantity > 0) formatQty(it.quantity) else "" } ?: "1")
    }
    var unit by remember { mutableStateOf(initial?.unit ?: UNITS.first()) }
    var addedCount by remember { mutableIntStateOf(0) }
    val nameFocus = remember { FocusRequester() }
    val qtyFocus = remember { FocusRequester() }

    // Notun item hole nam-e, edit hole (jemon demo list-e quantity dite) quantity-te cursor jabe
    LaunchedEffect(Unit) {
        runCatching { if (keepOpen) nameFocus.requestFocus() else qtyFocus.requestFocus() }
    }

    val matches = remember(name, suggestions) {
        val q = name.trim()
        suggestions
            .filter { it.isNotBlank() && !it.equals(q, ignoreCase = true) && (q.isEmpty() || it.contains(q, ignoreCase = true)) }
            .take(8)
    }

    fun submit() {
        val n = name.trim()
        if (n.isEmpty()) return
        // Notun item-e quantity khali thakle 1; edit-e khali mane "quantity deya hoyni" (0)
        val q = qty.toDoubleOrNull()?.takeIf { it > 0 } ?: if (keepOpen) 1.0 else 0.0
        onSave(n, q, unit)
        if (keepOpen) {
            addedCount++
            name = ""
            qty = "1"
            runCatching { nameFocus.requestFocus() }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "নতুন আইটেম" else "আইটেম সম্পাদনা") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (keepOpen && addedCount > 0) {
                    Text(
                        text = "✓ ${addedCount.toString().bn()} টি যোগ হয়েছে",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("আইটেমের নাম (যেমন: আলু)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                )
                if (matches.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(matches) { s ->
                            SuggestionChip(onClick = { name = s }, label = { Text(s) })
                        }
                    }
                }
                OutlinedTextField(
                    value = qty,
                    onValueChange = { v -> qty = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("পরিমাণ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(qtyFocus)
                )
                Text("একক", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(UNITS) { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(if (keepOpen) "যোগ করুন" else "সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (keepOpen) "শেষ" else "বাতিল") }
        }
    )
}