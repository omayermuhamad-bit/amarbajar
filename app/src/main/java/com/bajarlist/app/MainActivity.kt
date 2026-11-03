package com.bajarlist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BajarTheme {
                BajarApp()
            }
        }
    }
}

@Composable
fun BajarApp(vm: BajarViewModel = viewModel()) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen(vm = vm, onBack = { showSettings = false })
    } else {
        MainScreen(vm = vm, onOpenSettings = { showSettings = true })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: BajarViewModel, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var editingRow by remember { mutableStateOf<ListRow?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun say(msg: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg)
        }
    }

    val rows = vm.rows
    val setCount = rows.count { it.quantity > 0 }

    fun buildPdf(): File? {
        // Shudhu jegulor poriman deya hoyeche segulo PDF-e jabe
        val list = rows.filter { it.quantity > 0 }.map {
            BazarItem(it.catalogId, it.name, it.quantity, it.unit, false, vm.type, vm.key)
        }
        if (list.isEmpty()) {
            say("PDF-এর জন্য অন্তত একটি আইটেমের পরিমাণ দিন")
            return null
        }
        return PdfExporter.create(context, vm.pdfTitle(), vm.periodLabel(), list, vm.pdfFileName())
    }

    fun savePdf() {
        val file = buildPdf() ?: return
        if (FileActions.saveToDownloads(context, file)) {
            say("PDF সংরক্ষিত হয়েছে — Downloads ফোল্ডারে (${file.name})")
        } else {
            say("PDF সংরক্ষণ করা যায়নি")
        }
    }

    fun sharePdf() {
        val file = buildPdf() ?: return
        FileActions.share(context, file, vm.pdfTitle())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("বাজার লিস্ট", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = { sharePdf() }) {
                        Icon(Icons.Default.Share, contentDescription = "PDF শেয়ার")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "সেটিংস")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "আরও")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("PDF সংরক্ষণ (Downloads)") },
                                onClick = { menuOpen = false; savePdf() }
                            )
                            DropdownMenuItem(
                                text = { Text("PDF শেয়ার") },
                                onClick = { menuOpen = false; sharePdf() }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (vm.tab == 0) "গতকালের পরিমাণ কপি"
                                        else "গত মাসের পরিমাণ কপি"
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    val n = vm.copyFromPrevious()
                                    say(
                                        if (n > 0) "${n.toString().bn()} টি আইটেমের পরিমাণ কপি হয়েছে"
                                        else "কপি করার মতো নতুন পরিমাণ নেই"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("সব পরিমাণ মুছুন") },
                                onClick = { menuOpen = false; confirmClear = true }
                            )
                        }
                    }
                }
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
            TabRow(selectedTabIndex = vm.tab) {
                Tab(
                    selected = vm.tab == 0,
                    onClick = { vm.tab = 0 },
                    text = { Text("দৈনিক বাজার", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = vm.tab == 1,
                    onClick = { vm.tab = 1 },
                    text = { Text("মাসিক বাজার", fontWeight = FontWeight.SemiBold) }
                )
            }

            PeriodBar(
                label = vm.periodLabel(),
                todayLabel = if (vm.tab == 0) "আজ" else "এই মাস",
                onPrev = { vm.prev() },
                onNext = { vm.next() },
                onToday = { vm.goToday() },
                onPick = if (vm.tab == 0) ({ showDatePicker = true }) else null
            )

            if (rows.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "মোট ${rows.size.toString().bn()} টি আইটেম",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        "পরিমাণ দেওয়া হয়েছে ${setCount.toString().bn()} টি",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (rows.isEmpty()) {
                EmptyState(
                    message = "এই তালিকায় কোনো আইটেম নেই।\nসেটিংস থেকে আইটেম যোগ করুন।",
                    actionLabel = "সেটিংসে যান",
                    onAction = onOpenSettings
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rows, key = { it.catalogId }) { row ->
                        ItemRow(row = row, onClick = { editingRow = row })
                    }
                }
            }
        }
    }

    // ---- + button: notun item ----
    if (showAdd) {
        AddItemDialog(
            onDismiss = { showAdd = false },
            onSave = { n, q, u -> vm.addNewItem(n, q, u) }
        )
    }

    // ---- item-e chaple: shudhu poriman ----
    editingRow?.let { row ->
        QuantityDialog(
            row = row,
            onDismiss = { editingRow = null },
            onSave = { q, u ->
                vm.setQuantity(row.name, q, u)
                editingRow = null
            }
        )
    }

    // ---- তারিখ বাছাই ----
    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = vm.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        vm.date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("ঠিক আছে") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // ---- সব পরিমাণ মুছার নিশ্চিতকরণ ----
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("সব পরিমাণ মুছবেন?") },
            text = {
                Text("${vm.periodLabel()} এর সব আইটেমের পরিমাণ মুছে যাবে। আইটেমের তালিকা (সেটিংস) ঠিক থাকবে।")
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearCurrent()
                    confirmClear = false
                }) { Text("হ্যাঁ, মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("না") }
            }
        )
    }
}
