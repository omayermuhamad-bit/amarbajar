
package com.bajarlist.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class BajarViewModel(app: Application) : AndroidViewModel(app) {
    private val storage = Storage(app)
    private val locale: Locale = Locale.forLanguageTag("bn-BD")

    var items by mutableStateOf(storage.load())
        private set

    /** 0 = দৈনিক, 1 = মাসিক */
    var tab by mutableIntStateOf(0)
    var date by mutableStateOf(LocalDate.now())
    var month by mutableStateOf(YearMonth.now())

    init {
        // Prothombar app khulle ei mashe-r monthly demo list ready kore dey
        if (!storage.isSeeded()) {
            val monthKey = YearMonth.now().toString()
            if (items.none { it.type == TYPE_MONTHLY && it.key == monthKey }) {
                addTemplate(monthKey)
                tab = 1
            }
            storage.setSeeded()
        }
    }

    private fun addTemplate(monthKey: String): Int {
        val existing = items
            .filter { it.type == TYPE_MONTHLY && it.key == monthKey }
            .map { it.name.trim().lowercase() }
            .toSet()
        val toAdd = MONTHLY_TEMPLATE.filter { it.first.lowercase() !in existing }
        val base = System.currentTimeMillis()
        update(items + toAdd.mapIndexed { i, (name, unit) ->
            BazarItem(base + i, name, 0.0, unit, false, TYPE_MONTHLY, monthKey)
        })
        return toAdd.size
    }

    /** Ekhon dekha mash-e demo list load kore. Koyta notun item holo ta return kore. */
    fun loadMonthlyTemplate(): Int = addTemplate(month.toString())

    val type: String get() = if (tab == 0) TYPE_DAILY else TYPE_MONTHLY
    val key: String get() = if (tab == 0) date.toString() else month.toString()

    val currentItems: List<BazarItem>
        get() = items.filter { it.type == type && it.key == key }.sortedBy { it.id }

    /** Aager use kora nam gulo (notun theke purono) - suggestion er jonno */
    val suggestions: List<String>
        get() = items.asReversed().map { it.name.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun update(newItems: List<BazarItem>) {
        items = newItems
        storage.save(newItems)
    }

    fun addItem(name: String, qty: Double, unit: String) {
        update(items + BazarItem(System.currentTimeMillis(), name, qty, unit, false, type, key))
    }

    fun editItem(id: Long, name: String, qty: Double, unit: String) {
        update(items.map { if (it.id == id) it.copy(name = name, quantity = qty, unit = unit) else it })
    }

    fun toggle(id: Long) {
        update(items.map { if (it.id == id) it.copy(bought = !it.bought) else it })
    }

    fun delete(id: Long) {
        update(items.filter { it.id != id })
    }

    fun restore(item: BazarItem) {
        if (items.none { it.id == item.id }) update(items + item)
    }

    fun clearCurrent() {
        update(items.filterNot { it.type == type && it.key == key })
    }

    /** Aager din / aager mash er list theke notun item gulo copy kore. Koyta copy holo ta return kore. */
    fun copyFromPrevious(): Int {
        val prevKey = if (tab == 0) date.minusDays(1).toString() else month.minusMonths(1).toString()
        val existing = currentItems.map { it.name.trim().lowercase() }.toSet()
        val toCopy = items.filter {
            it.type == type && it.key == prevKey && it.name.trim().lowercase() !in existing
        }
        val base = System.currentTimeMillis()
        update(items + toCopy.mapIndexed { i, item ->
            item.copy(id = base + i, bought = false, key = key)
        })
        return toCopy.size
    }

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

    fun pdfFileName(): String = "bajar-" + (if (tab == 0) "daily-" else "monthly-") + key + ".pdf"
}