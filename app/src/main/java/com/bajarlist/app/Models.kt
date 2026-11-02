package com.bajarlist.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

const val TYPE_DAILY = "DAILY"
const val TYPE_MONTHLY = "MONTHLY"

/** Quantity-r unit gulo. Chaile ekhane notun unit jog korte paren. */
val UNITS = listOf(
    "কেজি", "গ্রাম", "লিটার", "মিলি", "পিস",
    "হালি", "ডজন", "প্যাকেট", "কৌটা", "বান্ডিল"
)

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

/** 2.0 -> "2", 1.5 -> "1.5" */
fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toLong().toString() else q.toString().trimEnd('0').trimEnd('.')

/** English digit gulo ke Bangla digit e convert kore. */
fun String.bn(): String = buildString {
    for (c in this@bn) {
        append(if (c in '0'..'9') '০' + (c - '0') else c)
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
}
