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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BajarApp(vm: BajarViewModel = viewModel()) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var menuOpen by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<BazarItem?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun say(msg: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(msg)
        }
    }

    fun buildPdf(): File? {
        // Shudhu jegulor quantity deya hoyeche segulo PDF-e jabe
        val list = vm.currentItems.filter { it.quantity > 0 }
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

    val list = vm.currentItems
    val boughtCount = list.count { it.bought }
    val unsetCount = list.count { it.quantity <= 0 }

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
                            if (vm.tab == 1) {
                                DropdownMenuItem(
                                    text = { Text("মাসের ডেমো তালিকা লোড") },
                                    onClick = {
                                        menuOpen = false
                                        val n = vm.loadMonthlyTemplate()
                                        say(
                                            if (n > 0) "${n.toString().bn()} টি আইটেম যোগ হয়েছে"
                                            else "সব আইটেম আগে থেকেই আছে"
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (vm.tab == 0) "গতকালের তালিকা কপি" else "গত মাসের তালিকা কপি") },
                                onClick = {
                                    menuOpen = false
                                    val n = vm.copyFromPrevious()
                                    say(
                                        if (n > 0) "${n.toString().bn()} টি আইটেম কপি হয়েছে"
                                        else "কপি করার মতো নতুন আইটেম নেই"
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("এই তালিকার সব মুছুন") },
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
                text = { Text("আইটেম যোগ") }
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

            if (list.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "মোট ${list.size.toString().bn()} টি আইটেম",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            "কেনা হয়েছে ${boughtCount.toString().bn()} টি",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { boughtCount.toFloat() / list.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    )
                    if (unsetCount > 0) {
                        Text(
                            "পরিমাণ দেওয়া হয়নি: ${unsetCount.toString().bn()} টি (PDF-এ আসবে না)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            if (list.isEmpty()) {
                EmptyState(
                    message = if (vm.tab == 0) "আজকের বাজারের তালিকা খালি।\nনিচের “আইটেম যোগ” চাপুন।"
                    else "এই মাসের বাজারের তালিকা খালি।\nডেমো তালিকা লোড করুন, অথবা নিচের “আইটেম যোগ” চাপুন।",
                    actionLabel = if (vm.tab == 1) "ডেমো তালিকা লোড করুন" else null,
                    onAction = if (vm.tab == 1) ({ vm.loadMonthlyTemplate() }) else null
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.id }) { item ->
                        ItemRow(
                            item = item,
                            onToggle = { vm.toggle(item.id) },
                            onEdit = { editing = item },
                            onDelete = {
                                vm.delete(item.id)
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    val result = snackbar.showSnackbar(
                                        message = "“${item.name}” মুছে ফেলা হয়েছে",
                                        actionLabel = "ফিরিয়ে আনুন"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) vm.restore(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // ---- নতুন আইটেম ----
    if (showAdd) {
        ItemDialog(
            initial = null,
            suggestions = vm.suggestions,
            onDismiss = { showAdd = false },
            onSave = { n, q, u -> vm.addItem(n, q, u) }
        )
    }

    // ---- আইটেম এডিট ----
    editing?.let { item ->
        ItemDialog(
            initial = item,
            suggestions = vm.suggestions,
            onDismiss = { editing = null },
            onSave = { n, q, u ->
                vm.editItem(item.id, n, q, u)
                editing = null
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

    // ---- সব মুছার নিশ্চিতকরণ ----
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("সব আইটেম মুছবেন?") },
            text = { Text("${vm.periodLabel()} এর তালিকার সব আইটেম মুছে যাবে।") },
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