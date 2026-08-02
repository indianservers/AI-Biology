package com.indianservers.AIbiology.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.indianservers.AIbiology.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Performs a catalog-only refresh when the app opens. All repository work runs on
 * background executors; downloaded media is never removed, so cached catalog data
 * and explicitly saved content remain available offline.
 */
object CatalogRefreshCoordinator {
    private const val PREFERENCES_NAME = "catalog_refresh_status"
    private const val SOURCE_COUNT = 4

    private val refreshing = AtomicBoolean(false)
    private val mutableStatus = MutableLiveData(CatalogRefreshStatus("OFFLINE READY"))
    val status: LiveData<CatalogRefreshStatus> = mutableStatus

    fun refreshAll(context: Context) {
        if (!refreshing.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        if (!NetworkAvailability.isInternetAvailable(appContext)) {
            mutableStatus.postValue(CatalogRefreshStatus("OFFLINE READY"))
            refreshing.set(false)
            return
        }
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        mutableStatus.postValue(CatalogRefreshStatus("CHECKING"))

        val biology = RemoteBiologyCatalogRepository(
            appContext,
            BuildConfig.BIOLOGY_CATALOG_URL
        )
        val anatomy = RemoteBiologyCatalogRepository(
            appContext,
            BuildConfig.BIOLOGY_ANATOMY_CATALOG_URL,
            namespace = "anatomy"
        )
        val infographics = InfographicRepository(
            appContext,
            BuildConfig.BIOLOGY_INFOGRAPHIC_CATALOG_URL
        )
        val microscopy = MicroscopyRepository(
            appContext,
            BuildConfig.BIOLOGY_MICROSCOPY_CATALOG_URL
        )

        val lock = Any()
        var completed = 0
        var reachableSources = 0
        var changedSources = 0

        fun record(source: String, fingerprint: String, reachable: Boolean, close: () -> Unit) {
            close()
            synchronized(lock) {
                val previous = preferences.getString(source, null)
                if (previous != null && previous != fingerprint) changedSources += 1
                if (fingerprint.isNotBlank()) {
                    preferences.edit().putString(source, fingerprint).apply()
                }
                if (reachable) reachableSources += 1
                completed += 1
                if (completed == SOURCE_COUNT) {
                    preferences.edit()
                        .putLong("last_checked_at", System.currentTimeMillis())
                        .apply()
                    mutableStatus.postValue(
                        CatalogRefreshStatus(
                            when {
                                changedSources > 0 -> "UPDATED"
                                reachableSources > 0 -> "ONLINE"
                                else -> "OFFLINE READY"
                            },
                            changedSources
                        )
                    )
                    refreshing.set(false)
                }
            }
        }

        biology.load { result ->
            record(
                "biology",
                result.models.fingerprint { "${it.id}:${it.version}" },
                result.warning == null && BuildConfig.BIOLOGY_CATALOG_URL.isNotBlank(),
                biology::close
            )
        }
        anatomy.load { result ->
            record(
                "anatomy",
                result.models.fingerprint { "${it.id}:${it.version}" },
                result.warning == null && BuildConfig.BIOLOGY_ANATOMY_CATALOG_URL.isNotBlank(),
                anatomy::close
            )
        }
        infographics.refresh { result ->
            record(
                "infographics",
                result.infographics.fingerprint { "${it.id}:${it.version}" },
                result.warning == null &&
                    BuildConfig.BIOLOGY_INFOGRAPHIC_CATALOG_URL.isNotBlank(),
                infographics::close
            )
        }
        microscopy.refresh { result ->
            record(
                "microscopy",
                result.slides.fingerprint { slide ->
                    "${slide.id}:${slide.reviewedAt.orEmpty()}:${slide.annotations.size}"
                },
                result.warning == null && BuildConfig.BIOLOGY_MICROSCOPY_CATALOG_URL.isNotBlank(),
                microscopy::close
            )
        }
    }

    private fun <T> List<T>.fingerprint(value: (T) -> String): String =
        map(value).sorted().joinToString("|")
}

data class CatalogRefreshStatus(
    val label: String,
    val changedSources: Int = 0
)
