package com.bajarlist.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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

/** Main list-er ek line: shudhu nam ar poriman. Chaple poriman set korar dialog khole. */
@Composable
fun ItemRow(row: ListRow, onClick: () -> Unit) {
    val hasQty = row.quantity > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasQty)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = row.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (hasQty) FontWeight.SemiBold else FontWeight.Normal
            )
            if (hasQty) {
                Text(
                    text = (formatQty(row.quantity) + " " + row.unit).bn(),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "পরিমাণ দিন",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
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

/** Unit bachhai korar chip-er line. */
@Composable
fun UnitChips(selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(UNITS) { u ->
            FilterChip(selected = selected == u, onClick = { onSelect(u) }, label = { Text(u) })
        }
    }
}

/** Item-e chaple: shudhu poriman (ar unit) set kora jay. */
@Composable
fun QuantityDialog(
    row: ListRow,
    onDismiss: () -> Unit,
    onSave: (qty: Double, unit: String) -> Unit
) {
    var qty by remember { mutableStateOf(if (row.quantity > 0) formatQty(row.quantity) else "") }
    var unit by remember { mutableStateOf(row.unit) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = qty,
                    onValueChange = { v -> qty = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("পরিমাণ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                Text("একক", style = MaterialTheme.typography.labelLarge)
                UnitChips(selected = unit, onSelect = { unit = it })
                Text(
                    "পরিমাণ খালি রাখলে এই আইটেম PDF-এ আসবে না",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(qty.enDigits().toDoubleOrNull()?.takeIf { it > 0 } ?: 0.0, unit)
            }) { Text("সংরক্ষণ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

/**
 * + button: notun item (nam + poriman + unit).
 * Dialog khola thake jate ekshathe onek item add kora jay.
 */
@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, unit: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf(UNITS.first()) }
    var addedCount by remember { mutableIntStateOf(0) }
    val nameFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { nameFocus.requestFocus() } }

    fun submit() {
        val n = name.trim()
        if (n.isEmpty()) return
        val q = qty.enDigits().toDoubleOrNull()?.takeIf { it > 0 } ?: 1.0
        onSave(n, q, unit)
        addedCount++
        name = ""
        qty = "1"
        runCatching { nameFocus.requestFocus() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন আইটেম") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (addedCount > 0) {
                    Text(
                        text = "✓ ${addedCount.toString().bn()} টি যোগ হয়েছে",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("আইটেমের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus)
                )
                OutlinedTextField(
                    value = qty,
                    onValueChange = { v -> qty = v.filter { it.isDigit() || it == '.' } },
                    label = { Text("পরিমাণ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("একক", style = MaterialTheme.typography.labelLarge)
                UnitChips(selected = unit, onSelect = { unit = it })
                Text(
                    "নতুন আইটেম সেটিংসের তালিকাতেও যোগ হবে",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) { Text("যোগ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("শেষ") }
        }
    )
}

/**
 * Settings-er item add / edit dialog (poriman chhara: shudhu nam ar default unit).
 * onSave false return korle "ei nam-e age theke ache" dekhay.
 */
@Composable
fun CatalogItemDialog(
    initial: CatalogItem?,
    onDismiss: () -> Unit,
    onSave: (name: String, unit: String) -> Boolean
) {
    val isAdd = initial == null
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: UNITS.first()) }
    var error by remember { mutableStateOf(false) }
    var addedCount by remember { mutableIntStateOf(0) }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    fun submit() {
        if (name.isBlank()) return
        if (onSave(name.trim(), unit)) {
            error = false
            if (isAdd) {
                addedCount++
                name = ""
                runCatching { focus.requestFocus() }
            } else {
                onDismiss()
            }
        } else {
            error = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isAdd) "নতুন আইটেম" else "আইটেম সম্পাদনা") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (isAdd && addedCount > 0) {
                    Text(
                        text = "✓ ${addedCount.toString().bn()} টি যোগ হয়েছে",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; error = false },
                    label = { Text("আইটেমের নাম") },
                    singleLine = true,
                    isError = error,
                    supportingText = {
                        if (error) Text("এই নামে আইটেম আগে থেকেই আছে")
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus)
                )
                Text("সাধারণ একক", style = MaterialTheme.typography.labelLarge)
                UnitChips(selected = unit, onSelect = { unit = it })
            }
        },
        confirmButton = {
            TextButton(onClick = { submit() }) {
                Text(if (isAdd) "যোগ করুন" else "সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isAdd) "শেষ" else "বাতিল") }
        }
    )
}

/** Settings: daily ar monthly-er shob item-er list. Ekhan theke notun item add / nam bodlano / delete. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: BajarViewModel, onBack: () -> Unit) {
    BackHandler(onBack = onBack)

    var tab by remember { mutableIntStateOf(0) }
    val type = if (tab == 0) TYPE_DAILY else TYPE_MONTHLY
    val list = vm.catalog.filter { it.type == type }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CatalogItem?>(null) }
    var deleting by remember { mutableStateOf<CatalogItem?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("সেটিংস", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("নতুন আইটেম") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("দৈনিক আইটেম", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("মাসিক আইটেম", fontWeight = FontWeight.SemiBold) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "মোট ${list.size.toString().bn()} টি আইটেম",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                TextButton(onClick = {
                    val n = vm.restoreDefaults(type)
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        snackbar.showSnackbar(
                            if (n > 0) "${n.toString().bn()} টি ডিফল্ট আইটেম যোগ হয়েছে"
                            else "সব ডিফল্ট আইটেম আগে থেকেই আছে"
                        )
                    }
                }) { Text("ডিফল্ট আইটেম যোগ করুন") }
            }
            Text(
                "আইটেমে চাপলে নাম বা একক বদলাতে পারবেন।",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list, key = { it.id }) { c ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = c },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = c.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = c.unit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            IconButton(onClick = { deleting = c }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "মুছুন",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        CatalogItemDialog(
            initial = null,
            onDismiss = { showAdd = false },
            onSave = { n, u -> vm.addCatalogItem(type, n, u) }
        )
    }

    editing?.let { c ->
        CatalogItemDialog(
            initial = c,
            onDismiss = { editing = null },
            onSave = { n, u -> vm.editCatalogItem(c.id, n, u) }
        )
    }

    deleting?.let { c ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("“${c.name}” মুছবেন?") },
            text = {
                Text(
                    if (c.type == TYPE_DAILY) "সব দৈনিক বাজার থেকে এই আইটেম ও এর পরিমাণ মুছে যাবে।"
                    else "সব মাসিক বাজার থেকে এই আইটেম ও এর পরিমাণ মুছে যাবে।"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCatalogItem(c.id)
                    deleting = null
                }) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("না") }
            }
        )
    }
}
