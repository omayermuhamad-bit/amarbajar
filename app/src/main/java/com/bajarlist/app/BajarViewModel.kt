package com.bajarlist.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Main list-er ek line: Settings-er item + ei din/mash-er poriman (0 = deya hoyni). */
data class ListRow(
    val catalogId: Long,
    val name: String,
    val unit: String,
    val quantity: Double,
    val image: String? = null
)

class BajarViewModel(app: Application) : AndroidViewModel(app) {
    private var lastId = 0L
    private val storage = Storage(app)
    private val locale: Locale = Locale.forLanguageTag("bn-BD")

    /** Din/mash-bhittik poriman gulo */
    var items by mutableStateOf(storage.load())
        private set

    /** Settings-er shob item (daily + monthly) */
    var catalog by mutableStateOf(initCatalog())
        private set

    /** Export mode: false = শুধু লেখা, true = ছবি সহ */
    var exportWithImages by mutableStateOf(storage.loadExportWithImages())
        private set

    fun updateExportWithImages(v: Boolean) {
        exportWithImages = v
        storage.saveExportWithImages(v)
    }

    /** 0 = দৈনিক, 1 = মাসিক */
    var tab by mutableIntStateOf(0)
    var date by mutableStateOf(LocalDate.now())
    var month by mutableStateOf(YearMonth.now())

    val type: String get() = if (tab == 0) TYPE_DAILY else TYPE_MONTHLY
    val key: String get() = if (tab == 0) date.toString() else month.toString()

    private fun nextId(): Long {
        lastId = maxOf(System.currentTimeMillis(), lastId + 1)
        return lastId
    }

    /**
     * Prothombar: shuruwat-er daily + monthly list, ar ager version-e jei item gulo
     * chhilo (tader poriman shoho) segulo-o Settings-er list-e dhukiye dey.
     */
    private fun initCatalog(): List<CatalogItem> {
        storage.loadCatalog()?.let { return it }
        val list = mutableListOf<CatalogItem>()
        fun add(name: String, unit: String, type: String) {
            val n = name.trim()
            if (n.isEmpty()) return
            if (list.none { it.type == type && normName(it.name) == normName(n) }) {
                list.add(CatalogItem(nextId(), n, unit, type))
            }
        }
        DAILY_TEMPLATE.forEach { add(it.first, it.second, TYPE_DAILY) }
        MONTHLY_TEMPLATE.forEach { add(it.first, it.second, TYPE_MONTHLY) }
        storage.load().sortedBy { it.id }.forEach { add(it.name, it.unit, it.type) }
        storage.saveCatalog(list)
        return list
    }

    /** Ekhon dekha din/mash-er shob item, poriman shoho. */
    val rows: List<ListRow>
        get() {
            val entries = items
                .filter { it.type == type && it.key == key }
                .associateBy { normName(it.name) }
            return catalog.filter { it.type == type }.map { c ->
                val e = entries[normName(c.name)]
                ListRow(c.id, c.name, e?.unit ?: c.unit, e?.quantity ?: 0.0, c.image)
            }
        }

    private fun updateItems(newItems: List<BazarItem>) {
        items = newItems
        storage.save(newItems)
    }

    private fun updateCatalog(newList: List<CatalogItem>) {
        catalog = newList
        storage.saveCatalog(newList)
    }

    // ---------- main list: poriman ----------

    /** Ekhon dekha din/mash-e ekta item-er poriman set kore. 0 dile poriman muchhe jay. */
    fun setQuantity(name: String, qty: Double, unit: String) {
        val nn = normName(name)
        val existing = items.firstOrNull {
            it.type == type && it.key == key && normName(it.name) == nn
        }
        if (existing != null) {
            updateItems(items.map {
                if (it.id == existing.id) it.copy(quantity = qty, unit = unit) else it
            })
        } else if (qty > 0) {
            updateItems(items + BazarItem(nextId(), name.trim(), qty, unit, false, type, key))
        }
    }

    /** + button: notun item Settings-er list-e jog hoy ar ekhon-er poriman set hoy. */
    fun addNewItem(name: String, qty: Double, unit: String) {
        val n = name.trim()
        if (n.isEmpty()) return
        if (catalog.none { it.type == type && normName(it.name) == normName(n) }) {
            updateCatalog(catalog + CatalogItem(nextId(), n, unit, type))
        }
        setQuantity(n, qty, unit)
    }

    /** Ekhon-er list-er shob poriman muche dey (Settings-er item list thik thake). */
    fun clearCurrent() {
        updateItems(items.filterNot { it.type == type && it.key == key })
    }

    /** Aager din / aager mash-er poriman copy kore. Koyta copy holo ta return kore. */
    fun copyFromPrevious(): Int {
        val prevKey = if (tab == 0) date.minusDays(1).toString() else month.minusMonths(1).toString()
        val alreadySet = rows.filter { it.quantity > 0 }.map { normName(it.name) }.toSet()
        val inCatalog = catalog.filter { it.type == type }.map { normName(it.name) }.toSet()
        val toCopy = items.filter {
            it.type == type && it.key == prevKey && it.quantity > 0 &&
                normName(it.name) in inCatalog && normName(it.name) !in alreadySet
        }
        var count = 0
        toCopy.forEach {
            setQuantity(it.name, it.quantity, it.unit)
            count++
        }
        return count
    }

    // ---------- Settings: shob item ----------

    /** Notun item add. Ei nam-e age theke thakle false. */
    fun addCatalogItem(t: String, name: String, unit: String): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        if (catalog.any { it.type == t && normName(it.name) == normName(n) }) return false
        updateCatalog(catalog + CatalogItem(nextId(), n, unit, t))
        return true
    }

    /** Nam / unit bodlay. Nam bodlale purono poriman gulo-o notun name-e cholte thake. */
    fun editCatalogItem(id: Long, name: String, unit: String): Boolean {
        val old = catalog.firstOrNull { it.id == id } ?: return false
        val n = name.trim()
        if (n.isEmpty()) return false
        if (catalog.any { it.id != id && it.type == old.type && normName(it.name) == normName(n) }) {
            return false
        }
        updateCatalog(catalog.map { if (it.id == id) it.copy(name = n, unit = unit) else it })
        if (normName(old.name) != normName(n)) {
            updateItems(items.map {
                if (it.type == old.type && normName(it.name) == normName(old.name)) {
                    it.copy(name = n)
                } else {
                    it
                }
            })
        }
        return true
    }

    // ---------- item-er chobi ----------

    private fun imageDir(): File =
        File(getApplication<Application>().filesDir, "item_images").apply { mkdirs() }

    /** Je-kono bitmap ke 480px-e chhoto kore item-er chobi hishebe save kore. */
    private fun importImage(id: Long, onDone: (Boolean) -> Unit, load: () -> Bitmap?) {
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) {
                runCatching {
                    val src = load() ?: return@runCatching null
                    val big = maxOf(src.width, src.height)
                    val bmp = if (big > 480) {
                        val s = 480f / big
                        Bitmap.createScaledBitmap(
                            src,
                            (src.width * s).toInt().coerceAtLeast(1),
                            (src.height * s).toInt().coerceAtLeast(1),
                            true
                        )
                    } else src
                    val f = File(imageDir(), "$id-${System.currentTimeMillis()}.jpg")
                    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                    f.absolutePath
                }.getOrNull()
            }
            if (path != null) {
                catalog.firstOrNull { it.id == id }?.image?.let { File(it).delete() }
                updateCatalog(catalog.map { if (it.id == id) it.copy(image = path) else it })
            }
            onDone(path != null)
        }
    }

    /** Gallery theke bachha chobi. */
    fun setImage(id: Long, uri: Uri, onDone: (Boolean) -> Unit = {}) {
        importImage(id, onDone) {
            val ctx = getApplication<Application>()
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { d, _, _ ->
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
    }

    /** Internet-er chobi-r link (https://...) theke download kore set kore. */
    fun setImageFromUrl(id: Long, url: String, onDone: (Boolean) -> Unit = {}) {
        importImage(id, onDone) {
            val u = url.trim()
            if (!u.startsWith("http://") && !u.startsWith("https://")) return@importImage null
            val conn = URL(u).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.instanceFollowRedirects = true
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) BajarList")
                if (conn.responseCode !in 200..299) return@importImage null
                val bytes = conn.inputStream.use { ins ->
                    val out = java.io.ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val n = ins.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > 10_000_000) return@importImage null
                        out.write(buf, 0, n)
                    }
                    out.toByteArray()
                }
                // boro chobi hole age bounds dekhe sample kore memory bachay
                val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                var sample = 1
                while (maxOf(opts.outWidth, opts.outHeight) / (sample * 2) >= 480) sample *= 2
                BitmapFactory.decodeByteArray(
                    bytes, 0, bytes.size,
                    BitmapFactory.Options().apply { inSampleSize = sample }
                )
            } finally {
                conn.disconnect()
            }
        }
    }

    fun removeImage(id: Long) {
        catalog.firstOrNull { it.id == id }?.image?.let { File(it).delete() }
        updateCatalog(catalog.map { if (it.id == id) it.copy(image = null) else it })
    }

    /** Item ke shob list theke (poriman shoho) muche dey. */
    fun deleteCatalogItem(id: Long) {
        val old = catalog.firstOrNull { it.id == id } ?: return
        old.image?.let { File(it).delete() }
        updateCatalog(catalog.filter { it.id != id })
        updateItems(items.filterNot {
            it.type == old.type && normName(it.name) == normName(old.name)
        })
    }

    /** Shuruwat-er item-er moddhe je gulo nei segulo abar jog kore. Koyta jog holo ta return kore. */
    fun restoreDefaults(t: String): Int {
        val template = if (t == TYPE_DAILY) DAILY_TEMPLATE else MONTHLY_TEMPLATE
        val existing = catalog.filter { it.type == t }.map { normName(it.name) }.toSet()
        val toAdd = template.filter { normName(it.first) !in existing }
        updateCatalog(catalog + toAdd.map { (name, unit) -> CatalogItem(nextId(), name, unit, t) })
        return toAdd.size
    }

    // ---------- tarikh / mash ----------

    fun prev() {
        if (tab == 0) date = date.minusDays(1) else month = month.minusMonths(1)
    }

    fun next() {
        if (tab == 0) date = date.plusDays(1) else month = month.plusMonths(1)
    }

    fun goToday() {
        if (tab == 0) date = LocalDate.now() else month = YearMonth.now()
    }

    fun periodLabel(): String =
        if (tab == 0) {
            date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale)).bn()
        } else {
            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).bn()
        }

    fun pdfTitle(): String = if (tab == 0) "দৈনিক বাজারের তালিকা" else "মাসিক বাজারের তালিকা"

    fun pdfFileName(): String =
        "bajar-" + (if (tab == 0) "daily-" else "monthly-") + key + (if (exportWithImages) "-photo" else "") + ".pdf"
}
