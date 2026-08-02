package com.indianservers.AIbiology

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelDownloadRecord
import com.indianservers.AIbiology.data.ModelDownloadStatus
import com.indianservers.AIbiology.data.ModelRepository
import com.indianservers.AIbiology.data.RemoteBiologyCatalogRepository
import com.indianservers.AIbiology.data.NetworkAvailability
import com.indianservers.AIbiology.databinding.FragmentFourthBinding
import com.indianservers.AIbiology.ui.AnatomySystemAdapter
import com.indianservers.AIbiology.ui.BiologyModelAdapter
import com.indianservers.AIbiology.ui.DeviceProfile
import com.indianservers.AIbiology.ui.TvFocus
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream

class FourthFragment : Fragment() {
    private var _binding: FragmentFourthBinding? = null
    private val binding get() = _binding!!
    private lateinit var catalogRepository: RemoteBiologyCatalogRepository
    private lateinit var modelRepository: ModelRepository
    private lateinit var adapter: AnatomySystemAdapter
    private var catalogue = emptyList<BiologyModel>()
    private var records = emptyMap<String, ModelDownloadRecord>()
    private var selectedModel: BiologyModel? = null
    private var showLibraryOnly = false
    private var autoRotating = true
    private var isFullscreen = false
    private var isTelevision = false
    private var viewerFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFourthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isTelevision = DeviceProfile.isTelevision(requireContext())
        modelRepository = ModelRepository(requireContext())
        catalogRepository = RemoteBiologyCatalogRepository(
            requireContext(),
            BuildConfig.BIOLOGY_ANATOMY_CATALOG_URL,
            namespace = "anatomy"
        )
        configureInsets()
        configureLayout()
        configureViewer()
        configureActions()
        configureBackHandling()
        loadCatalogue(refreshRemote = false)
    }

    private fun configureInsets() {
        val baseHeight = 82.dp
        val start = if (isTelevision) 36.dp else binding.anatomyTopBar.paddingStart
        val end = if (isTelevision) 36.dp else binding.anatomyTopBar.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(binding.anatomyTopBar) { topBar, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            topBar.setPadding(start + system.left, system.top, end + system.right, 0)
            topBar.layoutParams = topBar.layoutParams.apply { height = baseHeight + system.top }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun configureLayout() {
        val twoPane = isTelevision || resources.configuration.smallestScreenWidthDp >= 600
        binding.anatomyWorkspace.orientation =
            if (twoPane) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        binding.anatomySystemRail.layoutParams = LinearLayout.LayoutParams(
            if (twoPane) (if (isTelevision) 320 else 260).dp
            else ViewGroup.LayoutParams.MATCH_PARENT,
            if (twoPane) ViewGroup.LayoutParams.MATCH_PARENT else 212.dp
        )
        binding.anatomyViewerPane.layoutParams = LinearLayout.LayoutParams(
            if (twoPane) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            if (twoPane) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            1f
        )
        binding.anatomySystemList.layoutManager = LinearLayoutManager(
            requireContext(),
            if (twoPane) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL,
            false
        )
        adapter = AnatomySystemAdapter(
            record = { records[it.id] },
            thumbnailFile = catalogRepository::thumbnailFile,
            requestThumbnail = catalogRepository::loadThumbnail,
            onSelected = ::selectModel,
            onAction = { model ->
                val downloaded =
                    records[model.id]?.status == ModelDownloadStatus.DOWNLOADED
                selectModel(model)
                if (!downloaded) downloadModel(model)
            }
        )
        binding.anatomySystemList.adapter = adapter

        if (isTelevision) {
            binding.anatomyTitle.textSize = 24f
            listOf(
                binding.anatomyBack,
                binding.anatomyRefresh,
                binding.allAnatomyFilter,
                binding.libraryAnatomyFilter,
                binding.anatomyRotation,
                binding.anatomyZoomIn,
                binding.anatomyZoomOut,
                binding.anatomyReset,
                binding.anatomyFullscreen,
                binding.anatomyAr,
                binding.anatomyDownloadAction,
                binding.anatomyDeleteAction,
                binding.closeAnatomyFullscreen
            ).forEach { TvFocus.apply(it, 1.03f) }
            TvFocus.apply(binding.anatomyWebView, 1.01f)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureViewer() {
        binding.anatomyWebView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            settings.allowFileAccess = true
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowContentAccess = false
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: android.webkit.WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    val localFile = viewerFile ?: return null
                    if (
                        uri.host != MODEL_HOST ||
                        !uri.path.orEmpty().startsWith("/models/") ||
                        uri.lastPathSegment != localFile.name ||
                        !localFile.isFile
                    ) {
                        return null
                    }
                    return WebResourceResponse(
                        GLB_MIME_TYPE,
                        null,
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        FileInputStream(localFile)
                    )
                }
            }
            addJavascriptInterface(AnatomyBridge(), "BiologyBridge")
            loadUrl("about:blank")
            setOnKeyListener { _, keyCode, event ->
                if (!isTelevision || event.action != KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener false
                }
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> evaluate("rotateBy(-12)")
                    KeyEvent.KEYCODE_DPAD_RIGHT -> evaluate("rotateBy(12)")
                    KeyEvent.KEYCODE_DPAD_UP -> evaluate("zoomBy(0.86)")
                    KeyEvent.KEYCODE_DPAD_DOWN -> evaluate("zoomBy(1.16)")
                    else -> return@setOnKeyListener false
                }
                true
            }
        }
    }

    private fun configureActions() {
        binding.anatomyBack.setOnClickListener { navigateBack() }
        binding.anatomyRefresh.setOnClickListener { loadCatalogue(refreshRemote = true) }
        binding.allAnatomyFilter.setOnClickListener {
            showLibraryOnly = false
            updateFilters()
            renderList()
        }
        binding.libraryAnatomyFilter.setOnClickListener {
            showLibraryOnly = true
            updateFilters()
            renderList()
        }
        binding.anatomyRotation.setOnClickListener {
            autoRotating = !autoRotating
            binding.anatomyRotation.text = if (autoRotating) "II" else "▶"
            binding.anatomyRotation.contentDescription =
                if (autoRotating) "Pause automatic rotation" else "Resume automatic rotation"
            evaluate("setAutoRotation($autoRotating)")
        }
        binding.anatomyZoomIn.setOnClickListener { evaluate("zoomBy(0.82)") }
        binding.anatomyZoomOut.setOnClickListener { evaluate("zoomBy(1.22)") }
        binding.anatomyReset.setOnClickListener { evaluate("resetView()") }
        binding.anatomyFullscreen.setOnClickListener { enterFullscreen() }
        binding.anatomyAr.setOnClickListener { launchArViewer() }
        binding.closeAnatomyFullscreen.setOnClickListener { exitFullscreen() }
        binding.anatomyDownloadAction.setOnClickListener {
            selectedModel?.let(::downloadModel)
        }
        binding.anatomyDeleteAction.setOnClickListener {
            selectedModel?.let(::confirmDelete)
        }
    }

    private fun configureBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        isFullscreen -> exitFullscreen()
                        binding.anatomyWebView.hasFocus() && isTelevision -> {
                            binding.anatomyWebView.clearFocus()
                            binding.anatomyRotation.requestFocus()
                        }
                        else -> navigateBack()
                    }
                }
            }
        )
    }

    private fun navigateBack() {
        if (!findNavController().popBackStack()) {
            findNavController().navigate(R.id.HomeFragment)
        }
    }

    private fun loadCatalogue(refreshRemote: Boolean) {
        records = modelRepository.records().associateBy(ModelDownloadRecord::modelId)
        showEmpty(
            "Loading human anatomy",
            "Checking systems, thumbnails, and your App Library.",
            loading = true
        )
        val onLoaded: (com.indianservers.AIbiology.data.CatalogLoadResult) -> Unit =
            onLoaded@{ result ->
            if (_binding == null) return@onLoaded
            catalogue = result.models.filter {
                it.id.startsWith("ANATOMY_") ||
                    it.id in SHARED_ANATOMY_ORGAN_IDS ||
                    it.categoryId.contains("anatomy", ignoreCase = true)
            }
            renderList()
            if (catalogue.isEmpty()) {
                val configured = BuildConfig.BIOLOGY_ANATOMY_CATALOG_URL.isNotBlank()
                showEmpty(
                    if (configured) "Anatomy catalogue unavailable" else "Anatomy is ready",
                    if (configured) {
                        "Check the website connection and refresh the catalogue."
                    } else {
                        "Configure the Anatomy catalogue URL to publish body systems without embedding models."
                    },
                    loading = false
                )
            } else if (selectedModel == null) {
                selectModel(catalogue.first())
            }
            result.warning?.takeIf { catalogue.isNotEmpty() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
            }
        if (refreshRemote && !NetworkAvailability.isInternetAvailable(requireContext())) {
            Toast.makeText(
                requireContext(),
                NetworkAvailability.CATALOG_WARNING,
                Toast.LENGTH_LONG
            ).show()
            catalogRepository.loadCached(onLoaded)
        } else if (refreshRemote) {
            catalogRepository.load(onLoaded)
        } else {
            catalogRepository.loadCached(onLoaded)
        }
    }

    private fun renderList() {
        records = modelRepository.records().associateBy(ModelDownloadRecord::modelId)
        val visible = if (showLibraryOnly) {
            catalogue.filter {
                records[it.id]?.status == ModelDownloadStatus.DOWNLOADED
            }
        } else {
            catalogue
        }
        adapter.submitList(visible)
        adapter.selectedId = selectedModel?.id
        val anatomyRecords = catalogue.mapNotNull { records[it.id] }
            .filter { it.status == ModelDownloadStatus.DOWNLOADED }
        val bytes = anatomyRecords.sumOf(ModelDownloadRecord::fileSizeBytes)
        binding.anatomyStorageSummary.text =
            if (anatomyRecords.isEmpty()) "No anatomy models downloaded"
            else "${anatomyRecords.size} downloaded · ${BiologyModelAdapter.formatBytes(bytes)}"
        if (showLibraryOnly && visible.isEmpty()) {
            showEmpty(
                "Your Anatomy Library is empty",
                "Download a body system and it will appear here for offline study.",
                loading = false
            )
        }
    }

    private fun selectModel(model: BiologyModel) {
        selectedModel = model
        adapter.selectedId = model.id
        binding.anatomyDetailPanel.visibility = View.VISIBLE
        binding.selectedAnatomyTitle.text = model.title
        binding.selectedAnatomyScientificName.text =
            model.scientificName ?: "Human anatomy system"
        binding.selectedAnatomyDescription.text = model.description
        val record = records[model.id]
        val file = record?.localFilePath?.let(::File)
            ?.takeIf { record.status == ModelDownloadStatus.DOWNLOADED && it.isFile }
        if (file != null) {
            loadModel(model, file)
        } else {
            viewerFile = null
            binding.anatomyWebView.loadUrl("about:blank")
            binding.anatomyViewerControls.visibility = View.GONE
            showModelPreview(model)
        }
        updateSelectedActions()
    }

    private fun showModelPreview(model: BiologyModel) {
        binding.anatomyPreviewImage.setImageDrawable(null)
        binding.anatomyPreviewImage.tag = model.id
        binding.anatomyPreviewImage.visibility = View.GONE
        showEmpty(
            "Loading ${model.title} preview",
            "The interactive model downloads only when you choose Download.",
            loading = true
        )
        val showFile: (File?) -> Unit = { file ->
            if (
                _binding != null &&
                selectedModel?.id == model.id &&
                binding.anatomyPreviewImage.tag == model.id &&
                file?.isFile == true
            ) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    binding.anatomyPreviewImage.setImageBitmap(bitmap)
                    binding.anatomyPreviewImage.visibility = View.VISIBLE
                    binding.anatomyEmptyState.visibility = View.GONE
                }
            }
        }
        val local = catalogRepository.thumbnailFile(model)
        if (local != null) showFile(local) else catalogRepository.loadThumbnail(model, showFile)
    }

    private fun loadModel(model: BiologyModel, file: File) {
        modelRepository.markOpened(model.id)
        viewerFile = file
        binding.anatomyPreviewImage.visibility = View.GONE
        autoRotating = true
        binding.anatomyRotation.text = "II"
        binding.anatomyEmptyState.visibility = View.VISIBLE
        binding.anatomyLoading.visibility = View.VISIBLE
        binding.anatomyEmptyTitle.text = "Preparing ${model.title}"
        binding.anatomyEmptyMessage.text = "Loading the downloaded 3D anatomy model."
        val url = Uri.parse("file:///android_asset/model_viewer.html")
            .buildUpon()
            .appendQueryParameter(
                "src",
                "https://$MODEL_HOST/models/${Uri.encode(file.name)}"
            )
            .appendQueryParameter("title", model.title)
            .appendQueryParameter("parts", JSONArray().toString())
            .appendQueryParameter("identifyMode", "0")
            .appendQueryParameter("showAllLabels", "0")
            .appendQueryParameter("modelIndex", "-1")
            .build()
            .toString()
        binding.anatomyWebView.loadUrl(url)
    }

    private fun downloadModel(model: BiologyModel) {
        val status = records[model.id]?.status
        if (status == ModelDownloadStatus.DOWNLOADING ||
            status == ModelDownloadStatus.QUEUED
        ) {
            AlertDialog.Builder(requireContext())
                .setTitle(model.title)
                .setMessage("Pause this download, or cancel it and remove partial data?")
                .setNegativeButton("Keep downloading", null)
                .setNeutralButton("Cancel download") { _, _ ->
                    modelRepository.cancel(model.id)
                    records = records - model.id
                    adapter.notifyDataSetChanged()
                    updateSelectedActions()
                }
                .setPositiveButton("Pause") { _, _ ->
                    modelRepository.pause(model.id)?.let {
                        records = records + (model.id to it)
                        adapter.notifyDataSetChanged()
                        updateSelectedActions()
                    }
                }
                .show()
            return
        }
        if (status == ModelDownloadStatus.DOWNLOADED) return
        modelRepository.download(model, explicitlySaved = true) { updated ->
            if (_binding == null) return@download
            records = records + (updated.modelId to updated)
            adapter.notifyDataSetChanged()
            updateStorageSummaryOnly()
            if (selectedModel?.id == updated.modelId) {
                updateSelectedActions()
                when (updated.status) {
                    ModelDownloadStatus.DOWNLOADED -> {
                        catalogRepository.loadThumbnail(model) {}
                        updated.localFilePath?.let(::File)?.takeIf(File::isFile)?.let {
                            loadModel(model, it)
                        }
                        Toast.makeText(
                            requireContext(),
                            "${model.title} added to App Library.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    ModelDownloadStatus.FAILED -> Toast.makeText(
                        requireContext(),
                        updated.errorMessage ?: "Could not download ${model.title}.",
                        Toast.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }

    private fun confirmDelete(model: BiologyModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete ${model.title}?")
            .setMessage(
                "The downloaded model will be removed from this device. " +
                    "Its catalogue card remains available for downloading again."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                modelRepository.remove(model.id, requireExplicitConfirmation = false)
                records = modelRepository.records().associateBy(ModelDownloadRecord::modelId)
                adapter.notifyDataSetChanged()
                selectModel(model)
                renderList()
            }
            .show()
    }

    private fun updateSelectedActions() {
        val model = selectedModel ?: return
        val record = records[model.id]
        val downloaded = record?.status == ModelDownloadStatus.DOWNLOADED
        val downloading = record?.status == ModelDownloadStatus.DOWNLOADING ||
            record?.status == ModelDownloadStatus.QUEUED ||
            record?.status == ModelDownloadStatus.PAUSED
        binding.anatomyDeleteAction.visibility = if (downloaded) View.VISIBLE else View.GONE
        binding.anatomyAr.visibility =
            if (downloaded && !isTelevision) View.VISIBLE else View.GONE
        binding.anatomyDownloadAction.isEnabled = !downloaded
        binding.anatomyDownloadAction.alpha =
            if (binding.anatomyDownloadAction.isEnabled) 1f else 0.65f
        binding.anatomyDownloadProgress.visibility =
            if (downloading) View.VISIBLE else View.GONE
        binding.anatomyDownloadProgress.isIndeterminate =
            record?.status == ModelDownloadStatus.QUEUED
        binding.anatomyDownloadProgress.progress =
            ((record?.progress ?: 0f).coerceIn(0f, 1f) * 100).toInt()
        binding.anatomyDownloadAction.text = when (record?.status) {
            ModelDownloadStatus.DOWNLOADED -> "In Library"
            ModelDownloadStatus.QUEUED -> "Queued"
            ModelDownloadStatus.DOWNLOADING ->
                "Pause ${(record.progress * 100).toInt()}%"
            ModelDownloadStatus.PAUSED ->
                "Resume ${(record.progress * 100).toInt()}%"
            ModelDownloadStatus.FAILED -> "Retry"
            ModelDownloadStatus.UPDATE_AVAILABLE -> "Update"
            else -> model.packageSizeBytes?.let {
                "Download ${BiologyModelAdapter.formatBytes(it)}"
            } ?: "Download"
        }
    }

    private fun updateStorageSummaryOnly() {
        val installed = catalogue.mapNotNull { records[it.id] }
            .filter { it.status == ModelDownloadStatus.DOWNLOADED }
        binding.anatomyStorageSummary.text =
            if (installed.isEmpty()) "No anatomy models downloaded"
            else "${installed.size} downloaded · ${
                BiologyModelAdapter.formatBytes(installed.sumOf(ModelDownloadRecord::fileSizeBytes))
            }"
    }

    private fun launchArViewer() {
        val model = selectedModel ?: return
        val file = viewerFile?.takeIf(File::isFile)
        if (file == null) {
            Toast.makeText(
                requireContext(),
                "Download ${model.title} before opening AR.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startActivity(
            Intent(requireContext(), AnatomyArActivity::class.java).apply {
                putExtra(AnatomyArActivity.EXTRA_MODEL_PATH, file.absolutePath)
                putExtra(AnatomyArActivity.EXTRA_MODEL_TITLE, model.title)
                putExtra(
                    AnatomyArActivity.EXTRA_MODEL_SIZE_METERS,
                    realWorldSizeMeters(model.title)
                )
            }
        )
    }

    private fun realWorldSizeMeters(title: String): Float {
        val normalized = title.lowercase()
        return when {
            "skeleton" in normalized || "human body" in normalized -> 1.72f
            "lung" in normalized -> 0.28f
            "liver" in normalized -> 0.24f
            "brain" in normalized -> 0.17f
            "heart" in normalized -> 0.12f
            "kidney" in normalized -> 0.11f
            "stomach" in normalized -> 0.24f
            else -> 0.35f
        }
    }

    private fun updateFilters() {
        binding.allAnatomyFilter.setBackgroundResource(
            if (showLibraryOnly) R.drawable.bg_filter_chip
            else R.drawable.bg_filter_chip_selected
        )
        binding.libraryAnatomyFilter.setBackgroundResource(
            if (showLibraryOnly) R.drawable.bg_filter_chip_selected
            else R.drawable.bg_filter_chip
        )
        binding.allAnatomyFilter.setTextColor(
            requireContext().getColor(
                if (showLibraryOnly) R.color.model_state_pending else R.color.white
            )
        )
        binding.libraryAnatomyFilter.setTextColor(
            requireContext().getColor(
                if (showLibraryOnly) R.color.white else R.color.model_state_pending
            )
        )
    }

    private fun enterFullscreen() {
        if (selectedModel == null || binding.anatomyViewerControls.visibility != View.VISIBLE) return
        val webView = binding.anatomyWebView
        (webView.parent as? ViewGroup)?.removeView(webView)
        binding.anatomyFullscreenHost.addView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        binding.anatomyFullscreenOverlay.visibility = View.VISIBLE
        isFullscreen = true
        if (isTelevision) binding.closeAnatomyFullscreen.requestFocus()
    }

    private fun exitFullscreen() {
        if (!isFullscreen) return
        val webView = binding.anatomyWebView
        (webView.parent as? ViewGroup)?.removeView(webView)
        binding.anatomyViewerHost.addView(
            webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        binding.anatomyFullscreenOverlay.visibility = View.GONE
        isFullscreen = false
    }

    private fun showEmpty(title: String, message: String, loading: Boolean) {
        binding.anatomyEmptyState.visibility = View.VISIBLE
        binding.anatomyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.anatomyLoadProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.anatomyLoadProgress.isIndeterminate = loading
        binding.anatomyEmptyTitle.text = title
        binding.anatomyEmptyMessage.text = message
    }

    private fun evaluate(script: String) {
        _binding?.anatomyWebView?.evaluateJavascript(script, null)
    }

    private inner class AnatomyBridge {
        @JavascriptInterface
        fun onModelState(state: String, detail: String) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                if (state == "loaded") {
                    binding.anatomyEmptyState.visibility = View.GONE
                    binding.anatomyLoadProgress.visibility = View.GONE
                    binding.anatomyViewerControls.visibility = View.VISIBLE
                } else if (state == "error") {
                    binding.anatomyViewerControls.visibility = View.GONE
                    showEmpty(
                        "Model could not be opened",
                        detail.ifBlank { "Delete this download and download it again." },
                        loading = false
                    )
                }
            }
        }

        @JavascriptInterface
        fun onLoadingStage(stage: String, progress: Int) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                binding.anatomyEmptyTitle.text = stage
                binding.anatomyEmptyMessage.text = "$progress%"
                binding.anatomyLoadProgress.visibility = View.VISIBLE
                binding.anatomyLoadProgress.isIndeterminate = false
                binding.anatomyLoadProgress.progress = progress.coerceIn(0, 100)
            }
        }

        @JavascriptInterface
        fun onPartSelected(index: Int) = Unit

        @JavascriptInterface
        fun onSurfaceAnchorsReady(modelIndex: Int, anchorsJson: String) = Unit

        @JavascriptInterface
        fun onCameraState(zoom: Double, orientation: String) = Unit

        @JavascriptInterface
        fun onViewerTapped() = Unit

        @JavascriptInterface
        fun onThumbnailReady(modelIndex: Int, encoded: String) = Unit
    }

    override fun onDestroyView() {
        if (isFullscreen) exitFullscreen()
        catalogRepository.close()
        modelRepository.close()
        binding.anatomySystemList.adapter = null
        binding.anatomyWebView.apply {
            removeJavascriptInterface("BiologyBridge")
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MODEL_HOST = "biology.local"
        private const val GLB_MIME_TYPE = "model/gltf-binary"
        private val SHARED_ANATOMY_ORGAN_IDS = setOf(
            "HUMAN_HEART",
            "HUMAN_LIVER",
            "HUMAN_LUNGS"
        )
    }
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
