package com.bajarlist.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

const val TYPE_DAILY = "DAILY"
const val TYPE_MONTHLY = "MONTHLY"

/** Quantity-r unit gulo. Chaile ekhane notun unit jog korte paren. */
val UNITS = listOf(
    "কেজি", "গ্রাম", "লিটার", "মিলি", "পিস",
    "হালি", "ডজন", "প্যাকেট", "কৌটা", "বান্ডিল", "বোতল"
)

/**
 * দৈনিক বাজারের শুরুর আইটেম (নাম to ডিফল্ট unit).
 * Eta shudhu prothombar-er jonno; Settings theke jekono shomoy bodlano jay.
 */
val DAILY_TEMPLATE: List<Pair<String, String>> = listOf(
    "আলু" to "কেজি",
    "পেঁয়াজ" to "কেজি",
    "টমেটো" to "কেজি",
    "কাঁচা মরিচ" to "গ্রাম",
    "ধনিয়া পাতা" to "গ্রাম",
    "লেবু" to "হালি",
    "বেগুন" to "কেজি",
    "শসা" to "কেজি",
    "সবজি" to "কেজি",
    "ডিম" to "হালি",
    "মাছ" to "কেজি",
    "মুরগি" to "কেজি",
    "গরুর মাংস" to "কেজি",
    "দুধ" to "লিটার",
    "পাউরুটি" to "প্যাকেট"
)

/** মাসিক বাজারের শুরুর আইটেম (নাম to ডিফল্ট unit). */
val MONTHLY_TEMPLATE: List<Pair<String, String>> = listOf(
    // চাল, ডাল, আটা
    "চাল" to "কেজি",
    "মিনিকেট চাল" to "কেজি",
    "নাজিরশাইল চাল" to "কেজি",
    "পোলাওর চাল" to "কেজি",
    "ডাল" to "কেজি",
    "মুগ ডাল" to "কেজি",
    "মটর ডাল" to "কেজি",
    "আটা" to "কেজি",
    "বুট" to "কেজি",
    // সবজি, তেল, মসলা
    "পেঁয়াজ" to "কেজি",
    "রসুন" to "কেজি",
    "আদা" to "গ্রাম",
    "সয়াবিন তেল" to "লিটার",
    "সরিষার তেল" to "লিটার",
    "ঘি" to "গ্রাম",
    "লবণ" to "কেজি",
    "চিনি" to "কেজি",
    "হলুদ গুঁড়া" to "গ্রাম",
    "জিরা গুঁড়া" to "গ্রাম",
    "মরিচ গুঁড়া" to "গ্রাম",
    "গোলমরিচ" to "গ্রাম",
    "দারুচিনি" to "গ্রাম",
    "এলাচ" to "গ্রাম",
    "লবঙ্গ" to "গ্রাম",
    "তেজপাতা" to "গ্রাম",
    // চা, দুধ, খাবার
    "চা পাতা (বেঙ্গল টি)" to "গ্রাম",
    "গুঁড়া দুধ" to "গ্রাম",
    "দই (প্রাণ/আড়ং ছাড়া)" to "কেজি",
    "মাখন (প্রাণ/আড়ং ছাড়া)" to "প্যাকেট",
    "নুডলস" to "প্যাকেট",
    "লাচ্ছা সেমাই" to "প্যাকেট",
    "নূরানী সেমাই" to "প্যাকেট",
    "সস" to "বোতল",
    "মুড়ি" to "কেজি",
    "চানাচুর (বোম্বে সুইটস)" to "প্যাকেট",
    "চানাচুর (বিবিকিউ)" to "প্যাকেট",
    "ইস্ট" to "গ্রাম",
    // ঘরের জিনিস
    "চাকা ওয়াশিং পাউডার" to "প্যাকেট",
    "ফিক্সল" to "বোতল",
    "সাবান (কেয়া)" to "পিস",
    "হ্যান্ডওয়াশ (রিফিল প্যাক)" to "প্যাকেট",
    "টুথপেস্ট" to "পিস",
    "মশার কয়েল (স্প্রিং)" to "প্যাকেট",
    "ন্যাপকিন টিস্যু (বসুন্ধরা)" to "প্যাকেট",
    "টয়লেট টিস্যু (বসুন্ধরা সাদা)" to "পিস",
    "পলিথিন ব্যাগ" to "প্যাকেট",
    "শলার ঝাড়ু" to "পিস",
    "ঝাড়ু" to "পিস"
)

/** Settings-er "shob item" talikar ek item. type = TYPE_DAILY ba TYPE_MONTHLY */
data class CatalogItem(
    val id: Long,
    val name: String,
    val unit: String,
    val type: String
)

/** Kono ekta din / mash-er jonno ekta item-er poriman. */
data class BazarItem(
    val id: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val bought: Boolean = false,
    /** TYPE_DAILY ba TYPE_MONTHLY */
    val type: String,
    /** daily hole yyyy-MM-dd, monthly hole yyyy-MM */
    val key: String
)

/** Nam milanor jonno: age-pichher space ar boro/chhoto hater farak bad. */
fun normName(s: String): String = s.trim().lowercase()

/** 2.0 -> "2", 1.5 -> "1.5" */
fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString().trimEnd('0').trimEnd('.')

/** English digit gulo ke Bangla digit e convert kore. */
fun String.bn(): String = buildString {
    for (c in this@bn) {
        append(if (c in '0'..'9') '০' + (c - '0') else c)
    }
}

/** Bangla digit (০-৯) ke English digit e convert kore, jate poriman thikmoto porha jay. */
fun String.enDigits(): String = buildString {
    for (c in this@enDigits) {
        append(if (c in '০'..'৯') '0' + (c - '০') else c)
    }
}

class Storage(context: Context) {
    private val prefs = context.getSharedPreferences("bajar_prefs", Context.MODE_PRIVATE)

    fun load(): List<BazarItem> {
        return try {
            val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                BazarItem(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    quantity = o.getDouble("quantity"),
                    unit = o.getString("unit"),
                    bought = o.optBoolean("bought", false),
                    type = o.getString("type"),
                    key = o.getString("key")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(items: List<BazarItem>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("quantity", it.quantity)
                    .put("unit", it.unit)
                    .put("bought", it.bought)
                    .put("type", it.type)
                    .put("key", it.key)
            )
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    /** Settings-er item talika. Ekdom prothombar hole null. */
    fun loadCatalog(): List<CatalogItem>? {
        val raw = prefs.getString("catalog", null) ?: return null
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CatalogItem(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    unit = o.getString("unit"),
                    type = o.getString("type")
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveCatalog(list: List<CatalogItem>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("unit", it.unit)
                    .put("type", it.type)
            )
        }
        prefs.edit().putString("catalog", arr.toString()).apply()
    }
}
