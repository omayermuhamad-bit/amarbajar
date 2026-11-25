package com.bajarlist.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.round

/** Chobi na thakle nam dekhe ekta emoji dekhay. Chaile nijer chobi o deya jay. */
private val EMOJIS: List<Pair<String, String>> = listOf(
    "চা পাতা" to "🍵", "তেজপাতা" to "🍃", "ধনিয়া" to "🌿", "মরিচ" to "🌶️",
    "আলু" to "🥔", "পেঁয়াজ" to "🧅", "টমেটো" to "🍅", "লেবু" to "🍋",
    "বেগুন" to "🍆", "শসা" to "🥒", "সবজি" to "🥬", "ডিম" to "🥚",
    "মাছ" to "🐟", "মুরগি" to "🍗", "মাংস" to "🥩", "গুঁড়া দুধ" to "🥛",
    "দুধ" to "🥛", "পাউরুটি" to "🍞", "ইস্ট" to "🍞", "চানাচুর" to "🥜",
    "চাল" to "🍚", "ডাল" to "🥣", "আটা" to "🌾", "বুট" to "🫘",
    "রসুন" to "🧄", "আদা" to "🌱", "তেল" to "🛢️", "ঘি" to "🧈",
    "মাখন" to "🧈", "লবণ" to "🧂", "চিনি" to "🍬", "দই" to "🥣",
    "নুডলস" to "🍜", "সেমাই" to "🍝", "মুড়ি" to "🍿", "সস" to "🥫",
    "দারুচিনি" to "🧂", "এলাচ" to "🧂", "লবঙ্গ" to "🧂", "জিরা" to "🧂",
    "গোলমরিচ" to "🧂", "হলুদ" to "🧂",
    "ওয়াশিং" to "🧺", "সাবান" to "🧼", "হ্যান্ডওয়াশ" to "🧴", "ফিক্সল" to "🧴",
    "টুথপেস্ট" to "🪥", "কয়েল" to "🦟", "টিস্যু" to "🧻", "পলিথিন" to "🛍️",
    "ঝাড়ু" to "🧹"
)

fun emojiFor(name: String): String =
    EMOJIS.firstOrNull { name.contains(it.first) }?.second ?: "🛒"

/** Kg/litre-e 0.5, gram/ml-e 50, baki unit-e 1 kore barbe/kombe. */
fun stepFor(unit: String): Double = when (unit) {
    "কেজি", "লিটার" -> 0.5
    "গ্রাম", "মিলি" -> 50.0
    else -> 1.0
}

private fun roundQty(q: Double) = round(q * 100) / 100

@Composable
private fun rememberItemBitmap(path: String?): ImageBitmap? =
    remember(path) {
        path?.let { runCatching { BitmapFactory.decodeFile(it)?.asImageBitmap() }.getOrNull() }
    }

/** Chobi (ba emoji) dekhanor square box. */
@Composable
fun ProductImage(name: String, image: String?, modifier: Modifier = Modifier, emojiSize: Int = 44) {
    val bmp = rememberItemBitmap(image)
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
        } else {
            Text(emojiFor(name), fontSize = emojiSize.sp)
        }
    }
}

/** Online shop-er moto product card: upore chobi, niche nam. Select hole badge + border. */
@Composable
fun ProductCard(row: ListRow, onClick: () -> Unit) {
    val selected = row.quantity > 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box {
                ProductImage(
                    name = row.name,
                    image = row.image,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.15f)
                )
                if (selected) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Text(
                            (formatQty(row.quantity) + " " + row.unit).bn(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    row.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (selected) "নির্বাচিত ✓" else "চাপুন, পরিমাণ দিন",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

/**
 * Product-e chaple niche theke sheet ashe: boro chobi, − / + diye poriman, unit, chobi bodlano.
 * Poriman 0 thakle item select hoy na.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductSheet(
    row: ListRow,
    onDismiss: () -> Unit,
    onSave: (qty: Double, unit: String) -> Unit,
    onPickPhoto: () -> Unit,
    onUrlPhoto: (url: String, onResult: (Boolean) -> Unit) -> Unit,
    onRemovePhoto: () -> Unit
) {
    var showUrl by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(row.unit) }
    var qty by remember { mutableStateOf(if (row.quantity > 0) formatQty(row.quantity) else "1") }
    val step = stepFor(unit)
    val current = qty.enDigits().toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ProductImage(
                name = row.name,
                image = row.image,
                emojiSize = 72,
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(20.dp))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onPickPhoto) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (row.image == null) "ছবি যোগ করুন" else "ছবি বদলান")
                }
                if (row.image != null) {
                    TextButton(onClick = onRemovePhoto) { Text("ছবি সরান") }
                }
            }
            TextButton(onClick = { showUrl = true }) { Text("🔗 লিংক থেকে ছবি") }
            Text(
                row.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            // − [ পরিমাণ ] +
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val v = roundQty(current - step)
                        qty = if (v > 0) formatQty(v) else "0"
                    },
                    modifier = Modifier.size(52.dp)
                ) { Text("−", fontSize = 26.sp, fontWeight = FontWeight.Bold) }

                OutlinedTextField(
                    value = qty,
                    onValueChange = { v -> qty = v.filter { it.isDigit() || it == '.' || it in '০'..'৯' } },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp)
                )

                FilledTonalIconButton(
                    onClick = { qty = formatQty(roundQty(current + step)) },
                    modifier = Modifier.size(52.dp)
                ) { Text("+", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            }

            UnitChips(selected = unit, onSelect = { unit = it })

            if (showUrl) {
                UrlPhotoDialog(onDismiss = { showUrl = false }, onSubmit = onUrlPhoto)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (row.quantity > 0) {
                    OutlinedButton(
                        onClick = { onSave(0.0, unit) },
                        modifier = Modifier.weight(1f)
                    ) { Text("বাদ দিন") }
                }
                Button(
                    onClick = { onSave(current.takeIf { it > 0 } ?: 0.0, unit) },
                    modifier = Modifier.weight(1f)
                ) { Text(if (current > 0) "তালিকায় রাখুন" else "বন্ধ করুন") }
            }
        }
    }
}

/** Internet-er chobi-r link paste kore chobi nama-r dialog. */
@Composable
private fun UrlPhotoDialog(
    onDismiss: () -> Unit,
    onSubmit: (url: String, onResult: (Boolean) -> Unit) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    var url by remember {
        val clip = runCatching { clipboard.getText()?.text }.getOrNull()?.trim().orEmpty()
        mutableStateOf(if (clip.startsWith("http")) clip else "")
    }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("লিংক থেকে ছবি") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Google-এ ছবি খুঁজে ছবিতে চেপে ধরে “Copy image address / ছবির লিংক কপি” করুন, তারপর এখানে পেস্ট করুন।",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; failed = false },
                    label = { Text("ছবির লিংক (https://...)") },
                    singleLine = true,
                    isError = failed,
                    supportingText = { if (failed) Text("ছবি আনা যায়নি। অন্য লিংক চেষ্টা করুন।") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (loading) CircularProgressIndicator(Modifier.size(24.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !loading && url.isNotBlank(),
                onClick = {
                    loading = true
                    onSubmit(url) { ok ->
                        loading = false
                        if (ok) onDismiss() else failed = true
                    }
                }
            ) { Text("ছবি নিন") }
        },
        dismissButton = {
            TextButton(enabled = !loading, onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

/** Niche bar: kotogulo select hoyeche + Export (PDF save / share). */
@Composable
fun ExportBar(
    count: Int,
    withImages: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${count.toString().bn()} টি নির্বাচিত",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (withImages) "PDF এক্সপোর্ট (ছবি সহ)" else "PDF এক্সপোর্ট (শুধু লেখা)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            OutlinedButton(onClick = onSave) { Text("সংরক্ষণ") }
            Button(onClick = onShare) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("শেয়ার")
            }
        }
    }
}
