package com.indianservers.AIbiology.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecentlyViewedItem(
    val modelId: String,
    val title: String,
    val destination: String,
    val viewedAtEpochMs: Long
)

class RecentlyViewedStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun record(model: BiologyModel, destination: String) {
        val updated = items()
            .filterNot { it.modelId == model.id && it.destination == destination }
            .toMutableList()
        updated.add(
            0,
            RecentlyViewedItem(
                modelId = model.id,
                title = model.title,
                destination = destination,
                viewedAtEpochMs = System.currentTimeMillis()
            )
        )
        save(updated.take(MAX_ITEMS))
    }

    fun items(): List<RecentlyViewedItem> {
        val encoded = preferences.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                RecentlyViewedItem(
                    modelId = item.getString("modelId"),
                    title = item.getString("title"),
                    destination = item.getString("destination"),
                    viewedAtEpochMs = item.optLong("viewedAt", 0L)
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun save(items: List<RecentlyViewedItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("modelId", item.modelId)
                    .put("title", item.title)
                    .put("destination", item.destination)
                    .put("viewedAt", item.viewedAtEpochMs)
            )
        }
        preferences.edit().putString(KEY_ITEMS, array.toString()).apply()
    }

    companion object {
        const val DESTINATION_MODELS = "models"
        const val DESTINATION_ANATOMY = "anatomy"
        const val ARG_MODEL_ID = "recent_model_id"
        private const val PREFERENCES_NAME = "recently_viewed_content"
        private const val KEY_ITEMS = "items"
        private const val MAX_ITEMS = 8
    }
}
