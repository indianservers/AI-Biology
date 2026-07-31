package com.indianservers.AIbiology.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.indianservers.AIbiology.R
import com.indianservers.AIbiology.data.BiologyCatalogQuery
import com.indianservers.AIbiology.data.BiologyCategories
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelDownloadRecord
import com.indianservers.AIbiology.data.ModelDownloadStatus
import com.indianservers.AIbiology.data.ModelRepository
import com.indianservers.AIbiology.data.ModelSort
import com.indianservers.AIbiology.databinding.SheetModelLibraryBinding
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ModelLibraryBottomSheet(
    private val context: Context,
    private val models: List<BiologyModel>,
    private val repository: ModelRepository,
    private val recentModelIds: () -> List<String>,
    private val isAvailable: (BiologyModel) -> Boolean,
    private val isFavourite: (BiologyModel) -> Boolean,
    private val thumbnailFile: (BiologyModel) -> File?,
    private val requestThumbnail: (BiologyModel, (File?) -> Unit) -> Unit,
    private val onSelected: (BiologyModel) -> Unit,
    private val onFavourite: (BiologyModel) -> Unit,
    private val onUnavailable: (BiologyModel, String) -> Unit
) {
    private val dialog = BottomSheetDialog(context)
    private val isTelevision = DeviceProfile.isTelevision(context)
    private val isLandscape =
        context.resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
    private val binding = SheetModelLibraryBinding.inflate(dialog.layoutInflater)
    private val queryExecutor = Executors.newSingleThreadExecutor()
    private val queryGeneration = AtomicInteger()
    private val records = mutableMapOf<String, ModelDownloadRecord>()
    private var activeCategory = BiologyCategories.ALL
    private var activeSort = ModelSort.RECOMMENDED
    private var query = ""
    private var visibleLimit = BiologyCatalogQuery.PAGE_SIZE

    private val adapter = BiologyModelAdapter(
        isAvailable = isAvailable,
        isFavourite = isFavourite,
        downloadRecord = { records[it.id] },
        thumbnailFile = thumbnailFile,
        requestThumbnail = requestThumbnail,
        onSelected = ::handleSelection,
        onDownloadStateSelected = ::handleDownloadStateSelection,
        onFavourite = {
            onFavourite(it)
            binding.modelLibraryGrid.post {
                (binding.modelLibraryGrid.adapter as? BiologyModelAdapter)
                    ?.refreshFavourite(it.id)
            }
        }
    )

    fun show() {
        records.clear()
        records.putAll(repository.records().associateBy(ModelDownloadRecord::modelId))
        dialog.setContentView(binding.root)
        configureGrid()
        configureCategories()
        configureQuickSections()
        configureSearchAndSort()
        configureCompactLayout()
        updateStorageSummary()
        submitQuery(showLoading = true)
        dialog.setOnDismissListener { queryExecutor.shutdownNow() }
        dialog.show()
        dialog.behavior.apply {
            isDraggable = !isTelevision
            isFitToContents = false
            halfExpandedRatio = 0.55f
            expandedOffset = 24.dp
            peekHeight = 72.dp
            state =
                if (isTelevision) BottomSheetBehavior.STATE_EXPANDED
                else BottomSheetBehavior.STATE_EXPANDED
            addBottomSheetCallback(
                object : BottomSheetBehavior.BottomSheetCallback() {
                    override fun onStateChanged(bottomSheet: View, newState: Int) {
                        binding.libraryExpandButton.text =
                            if (newState == BottomSheetBehavior.STATE_EXPANDED) "v" else "^"
                        binding.libraryExpandButton.contentDescription =
                            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                                "Collapse model library"
                            } else {
                                "Expand model library"
                            }
                    }

                    override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
                }
            )
        }
        dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            background = null
            layoutParams.height =
                if (isTelevision) ViewGroup.LayoutParams.MATCH_PARENT
                else (context.resources.displayMetrics.heightPixels * 0.92f).toInt()
        }
        if (isTelevision) {
            listOf(
                binding.librarySearch,
                binding.librarySort,
                binding.libraryRetry,
                binding.libraryExpandButton
            ).forEach { TvFocus.apply(it) }
            binding.librarySearch.requestFocus()
        }
    }

    private fun configureCompactLayout() {
        if (!isLandscape && !isTelevision) return
        binding.libraryHandle.layoutParams =
            binding.libraryHandle.layoutParams.apply { height = 56.dp }
        binding.libraryExpandButton.visibility = View.GONE
        ((binding.libraryQuickSections.parent as? View)?.parent as? View)?.visibility = View.GONE
        binding.librarySearch.layoutParams =
            (binding.librarySearch.layoutParams as ViewGroup.MarginLayoutParams).apply {
                height = 44.dp
                topMargin = 6.dp
            }
    }

    private fun configureGrid() {
        val spanCount =
            when {
                isTelevision -> 5
                context.resources.configuration.smallestScreenWidthDp >= 840 -> 3
                else -> 2
            }
        binding.modelLibraryGrid.layoutManager = GridLayoutManager(context, spanCount)
        binding.modelLibraryGrid.adapter = adapter
        binding.modelLibraryGrid.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    val manager = recyclerView.layoutManager as GridLayoutManager
                    if (
                        manager.findLastVisibleItemPosition() >= adapter.itemCount - 5 &&
                        visibleLimit < models.size
                    ) {
                        visibleLimit += BiologyCatalogQuery.PAGE_SIZE
                        submitQuery(showLoading = false)
                    }
                }
            }
        )
        binding.libraryHandle.setOnClickListener { toggleSheetState() }
        binding.libraryExpandButton.setOnClickListener { toggleSheetState() }
        binding.libraryRetry.setOnClickListener { submitQuery(showLoading = true) }
    }

    private fun configureCategories() {
        binding.libraryCategoryStrip.removeAllViews()
        BiologyCategories.all.forEach { category ->
            binding.libraryCategoryStrip.addView(
                chip(category, selected = category == activeCategory) {
                    activeCategory = category
                    visibleLimit = BiologyCatalogQuery.PAGE_SIZE
                    binding.currentLibraryCategory.text = category
                    configureCategories()
                    submitQuery(showLoading = false)
                }
            )
        }
    }

    private fun configureQuickSections() {
        binding.libraryQuickSections.removeAllViews()
        listOf("Featured", "Recent", "Continue", "Downloaded", "All models").forEach { section ->
            binding.libraryQuickSections.addView(
                chip(section, selected = false) {
                    when (section) {
                        "Featured" -> activeSort = ModelSort.RECOMMENDED
                        "Recent" -> activeSort = ModelSort.RECENTLY_VIEWED
                        "Continue" -> activeSort = ModelSort.RECOMMENDED
                        "Downloaded" -> activeSort = ModelSort.DOWNLOADED
                        else -> {
                            activeCategory = BiologyCategories.ALL
                            activeSort = ModelSort.A_TO_Z
                            configureCategories()
                        }
                    }
                    binding.librarySort.text = activeSort.label
                    submitQuery(showLoading = false)
                }
            )
        }
    }

    private fun configureSearchAndSort() {
        binding.librarySearch.doAfterTextChanged {
            query = it?.toString().orEmpty()
            visibleLimit = BiologyCatalogQuery.PAGE_SIZE
            submitQuery(showLoading = false)
        }
        binding.librarySort.setOnClickListener { anchor ->
            PopupMenu(context, anchor).apply {
                ModelSort.entries.forEachIndexed { index, sort ->
                    menu.add(0, index, index, sort.label)
                }
                setOnMenuItemClickListener { item ->
                    activeSort = ModelSort.entries[item.itemId]
                    binding.librarySort.text = activeSort.label
                    submitQuery(showLoading = false)
                    true
                }
                show()
            }
        }
    }

    private fun submitQuery(showLoading: Boolean) {
        val generation = queryGeneration.incrementAndGet()
        if (showLoading) showState("Loading model library...", progress = true)
        val snapshotQuery = query
        val snapshotCategory = activeCategory
        val snapshotSort = activeSort
        val snapshotLimit = visibleLimit
        queryExecutor.execute {
            val result = BiologyCatalogQuery.filter(
                models = models,
                query = snapshotQuery,
                category = snapshotCategory,
                sort = snapshotSort,
                downloadedIds = repository.downloadedIds(),
                recentIds = recentModelIds(),
                limit = snapshotLimit
            )
            binding.root.post {
                if (generation != queryGeneration.get()) return@post
                adapter.submitList(result)
                if (result.isEmpty()) {
                    showState(
                        if (snapshotQuery.isNotBlank()) {
                            "No models match this search."
                        } else {
                            "No models in ${snapshotCategory.lowercase()}."
                        }
                    )
                } else {
                    hideState()
                }
            }
        }
    }

    private fun handleSelection(model: BiologyModel) {
        if (isAvailable(model)) {
            repository.markOpened(model.id)
            onSelected(model)
            dialog.dismiss()
            return
        }
        if (model.glbUrl.isNullOrBlank() && model.packageUrl.isNullOrBlank()) {
            onUnavailable(
                model,
                "This model is in the catalogue, but its online download source is not configured yet."
            )
            return
        }
        repository.download(model, explicitlySaved = true) { record ->
            records[model.id] = record
            adapter.currentList.indexOfFirst { it.id == model.id }
                .takeIf { it >= 0 }
                ?.let(adapter::notifyItemChanged)
            updateStorageSummary()
            if (record.status == com.indianservers.AIbiology.data.ModelDownloadStatus.DOWNLOADED) {
                onSelected(model)
                dialog.dismiss()
            }
        }
    }

    private fun handleDownloadStateSelection(model: BiologyModel) {
        when (records[model.id]?.status) {
            ModelDownloadStatus.DOWNLOADING,
            ModelDownloadStatus.QUEUED -> {
                android.app.AlertDialog.Builder(context)
                    .setTitle(model.title)
                    .setMessage("Pause this download, or cancel it and remove partial data?")
                    .setNegativeButton("Keep downloading", null)
                    .setNeutralButton("Cancel download") { _, _ ->
                        repository.cancel(model.id)?.let { updateRecord(it, keep = false) }
                    }
                    .setPositiveButton("Pause") { _, _ ->
                        repository.pause(model.id)?.let(::updateRecord)
                    }
                    .show()
            }
            ModelDownloadStatus.PAUSED -> startDownload(model)
            else -> handleSelection(model)
        }
    }

    private fun startDownload(model: BiologyModel) {
        repository.download(model, explicitlySaved = true) { record ->
            updateRecord(record)
            if (record.status == ModelDownloadStatus.DOWNLOADED) {
                onSelected(model)
                dialog.dismiss()
            }
        }
    }

    private fun updateRecord(record: ModelDownloadRecord, keep: Boolean = true) {
        if (keep) records[record.modelId] = record else records.remove(record.modelId)
        adapter.currentList.indexOfFirst { it.id == record.modelId }
            .takeIf { it >= 0 }
            ?.let(adapter::notifyItemChanged)
        updateStorageSummary()
    }

    private fun toggleSheetState() {
        dialog.behavior.state =
            if (dialog.behavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                BottomSheetBehavior.STATE_COLLAPSED
            } else {
                BottomSheetBehavior.STATE_EXPANDED
            }
    }

    private fun updateStorageSummary() {
        val (count, bytes) = repository.storageSummary()
        binding.libraryStorageSummary.text =
            if (count == 0) "No downloads" else "$count  |  ${BiologyModelAdapter.formatBytes(bytes)}"
    }

    private fun showState(message: String, progress: Boolean = false) {
        binding.libraryStatePanel.visibility = View.VISIBLE
        binding.libraryProgress.visibility = if (progress) View.VISIBLE else View.GONE
        binding.libraryStateText.text = message
        binding.libraryRetry.visibility = View.GONE
    }

    private fun hideState() {
        binding.libraryStatePanel.visibility = View.GONE
    }

    private fun chip(title: String, selected: Boolean, action: () -> Unit) =
        TextView(context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (isLandscape || isTelevision) 40.dp else 48.dp
            ).apply { marginEnd = 8.dp }
            minWidth = 72.dp
            gravity = Gravity.CENTER
            setPadding(14.dp, 0, 14.dp, 0)
            text = title
            textSize = 12f
            setTextColor(Color.WHITE)
            background = context.getDrawable(
                if (selected) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip
            )
            isClickable = true
            isFocusable = true
            contentDescription = "$title filter"
            setOnClickListener { action() }
            if (isTelevision) TvFocus.apply(this)
        }

    private val Int.dp: Int
        get() = (this * context.resources.displayMetrics.density).toInt()
}
