package com.indianservers.biology

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.pm.ActivityInfo
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.indianservers.biology.databinding.FragmentFirstBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var selectedModelIndex = 0
    private var selectedPartIndex = 0
    private var isAutoRotating = true
    private var rotationSpeedIndex = 1
    private var cameraPresetIndex = 0
    private var isFullScreen = false
    private var showAllLabels = false
    private var identifyMode = true
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideFullScreenControls = Runnable { setFullScreenControlsVisible(false) }
    private val hideFullScreenHint = Runnable {
        _binding?.fullScreenHint?.animate()?.alpha(0f)?.withEndAction {
            _binding?.fullScreenHint?.visibility = View.GONE
        }?.start()
    }
    private var partBottomSheet: BottomSheetDialog? = null
    private var activeFilter = FILTER_ALL
    private var modelQuery = ""
    private val recentModelIndices = mutableListOf<Int>()
    private var originalViewerParent: ViewGroup? = null
    private var originalViewerIndex = -1
    private var originalViewerLayoutParams: ViewGroup.LayoutParams? = null
    private lateinit var modelStorageDirectory: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        modelStorageDirectory = File(requireContext().filesDir, MODEL_ASSET_DIRECTORY)
        configureInsets()
        setSystemBarsVisible(true)
        configureViewer()
        configureViewerControls()
        configureExpanders()
        configureModelDiscovery()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(false) {
                override fun handleOnBackPressed() {
                    exitFullScreen()
                }
            }.also { callback ->
                binding.fullScreenOverlay.setTag(R.id.fullScreenOverlay, callback)
            }
        )
    }

    private fun configureInsets() {
        val baseHeight = 76.dp
        val baseStart = binding.topAppBar.paddingStart
        val baseEnd = binding.topAppBar.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(binding.topAppBar) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.statusBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                baseStart + insets.left,
                insets.top,
                baseEnd + insets.right,
                0
            )
            view.layoutParams = view.layoutParams.apply {
                height = baseHeight + insets.top
            }
            windowInsets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDestroyView() {
        uiHandler.removeCallbacksAndMessages(null)
        partBottomSheet?.dismiss()
        partBottomSheet = null
        if (isFullScreen) exitFullScreen()
        binding.modelWebView.removeJavascriptInterface(BRIDGE_NAME)
        binding.modelWebView.destroy()
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configureViewer() {
        binding.modelWebView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            addJavascriptInterface(ModelBridge(), BRIDGE_NAME)
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val uri = request?.url ?: return null
                    if (uri.host != MODEL_HOST || !uri.path.orEmpty().startsWith("/models/")) {
                        return null
                    }

                    val fileName = uri.lastPathSegment ?: return null
                    val modelFile = findStoredModel(fileName) ?: return null
                    return WebResourceResponse(
                        GLB_MIME_TYPE,
                        null,
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        FileInputStream(modelFile)
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (_binding != null && binding.viewerStatusOverlay.visibility == View.VISIBLE) {
                        binding.viewerStatusText.text =
                            if (newProgress < 100) {
                                "Preparing viewer… $newProgress%"
                            } else {
                                "Loading 3D geometry…"
                            }
                    }
                }
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN,
                    MotionEvent.ACTION_POINTER_DOWN,
                    MotionEvent.ACTION_MOVE ->
                        binding.contentScroll.requestDisallowInterceptTouchEvent(true)

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL ->
                        binding.contentScroll.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
        }
    }

    private fun configureViewerControls() {
        binding.rotateLeftButton.setOnClickListener {
            runViewerCommand("rotateBy(-30)")
            onViewerControlUsed()
        }
        binding.rotateRightButton.setOnClickListener {
            runViewerCommand("rotateBy(30)")
            onViewerControlUsed()
        }
        binding.zoomOutButton.setOnClickListener {
            runViewerCommand("zoomBy(1.2)")
            onViewerControlUsed()
        }
        binding.zoomInButton.setOnClickListener {
            runViewerCommand("zoomBy(0.82)")
            onViewerControlUsed()
        }
        binding.resetViewButton.setOnClickListener {
            cameraPresetIndex = 0
            updateCameraPresetControl()
            runViewerCommand("resetView()")
            onViewerControlUsed()
        }
        binding.rotationSpeedButton.setOnClickListener {
            rotationSpeedIndex = (rotationSpeedIndex + 1) % ROTATION_SPEEDS.size
            updateRotationSpeedControl()
            runViewerCommand("setRotationSpeed(${ROTATION_SPEEDS[rotationSpeedIndex]})")
            onViewerControlUsed()
        }
        binding.cameraViewButton.setOnClickListener {
            cameraPresetIndex = (cameraPresetIndex + 1) % CAMERA_PRESETS.size
            updateCameraPresetControl()
            runViewerCommand("setCameraPreset('${CAMERA_PRESETS[cameraPresetIndex].key}')")
            onViewerControlUsed()
        }
        binding.rotationButton.setOnClickListener {
            isAutoRotating = !isAutoRotating
            updateRotationControl()
            runViewerCommand("setAutoRotation($isAutoRotating)")
            onViewerControlUsed()
        }
        binding.fullScreenButton.setOnClickListener {
            if (isFullScreen) exitFullScreen() else enterFullScreen()
        }
        binding.fullScreenClose.setOnClickListener { exitFullScreen() }
        binding.retryModelButton.setOnClickListener {
            loadModel(MODEL_CATALOG[selectedModelIndex])
        }
        binding.closeStatusButton.setOnClickListener {
            binding.viewerStatusOverlay.visibility = View.GONE
        }
        updateRotationSpeedControl()
        updateCameraPresetControl()
    }

    private fun updateRotationControl() {
        binding.rotationButton.text = if (isAutoRotating) "Ⅱ" else "▶"
        binding.rotationButton.contentDescription =
            if (isAutoRotating) "Pause automatic rotation" else "Resume automatic rotation"
        binding.rotationButton.tooltipText = binding.rotationButton.contentDescription
    }

    private fun updateRotationSpeedControl() {
        binding.rotationSpeedButton.text = ROTATION_SPEED_LABELS[rotationSpeedIndex]
        binding.rotationSpeedButton.contentDescription =
            "Rotation speed ${ROTATION_SPEED_LABELS[rotationSpeedIndex]}"
        binding.rotationSpeedButton.tooltipText = binding.rotationSpeedButton.contentDescription
    }

    private fun updateCameraPresetControl() {
        val preset = CAMERA_PRESETS[cameraPresetIndex]
        binding.cameraViewButton.text = preset.shortLabel
        binding.cameraViewButton.contentDescription = "${preset.title} camera view"
        binding.cameraViewButton.tooltipText = binding.cameraViewButton.contentDescription
    }

    private fun runViewerCommand(command: String) {
        binding.modelWebView.evaluateJavascript("window.$command", null)
    }

    private fun enterFullScreen() {
        if (isFullScreen) return
        val viewer = binding.viewerContainer
        val parent = viewer.parent as? ViewGroup ?: return

        originalViewerParent = parent
        originalViewerIndex = parent.indexOfChild(viewer)
        originalViewerLayoutParams = viewer.layoutParams
        parent.removeView(viewer)
        binding.fullScreenViewerHost.addView(
            viewer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        isFullScreen = true
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        binding.fullScreenTitle.text = MODEL_CATALOG[selectedModelIndex].title
        binding.fullScreenButton.text = "×"
        binding.fullScreenButton.contentDescription = "Close full screen"
        binding.fullScreenButton.tooltipText = "Close full screen"
        binding.fullScreenOverlay.visibility = View.VISIBLE
        binding.fullScreenTopBar.visibility = View.VISIBLE
        binding.fullScreenTopBar.bringToFront()
        binding.fullScreenButton.visibility = View.GONE
        updateOrientationTopMargin(76.dp)
        showFullScreenControls()
        showFullScreenHintIfNeeded()
        (binding.fullScreenOverlay.getTag(R.id.fullScreenOverlay) as? OnBackPressedCallback)
            ?.isEnabled = true
        setSystemBarsVisible(false)
    }

    private fun exitFullScreen() {
        if (!isFullScreen || _binding == null) return
        val viewer = binding.viewerContainer
        binding.fullScreenViewerHost.removeView(viewer)
        originalViewerParent?.addView(
            viewer,
            originalViewerIndex.coerceAtLeast(0),
            originalViewerLayoutParams
        )

        isFullScreen = false
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        uiHandler.removeCallbacks(hideFullScreenControls)
        uiHandler.removeCallbacks(hideFullScreenHint)
        setFullScreenControlsVisible(true)
        binding.fullScreenButton.text = "⛶"
        binding.fullScreenButton.contentDescription = "Open full screen"
        binding.fullScreenButton.tooltipText = "Open full screen"
        binding.fullScreenButton.visibility = View.VISIBLE
        updateOrientationTopMargin(12.dp)
        binding.fullScreenTopBar.visibility = View.GONE
        binding.fullScreenOverlay.visibility = View.GONE
        (binding.fullScreenOverlay.getTag(R.id.fullScreenOverlay) as? OnBackPressedCallback)
            ?.isEnabled = false
        setSystemBarsVisible(true)
    }

    private fun onViewerControlUsed() {
        if (isFullScreen) showFullScreenControls()
    }

    private fun showFullScreenControls() {
        if (!isFullScreen) return
        uiHandler.removeCallbacks(hideFullScreenControls)
        setFullScreenControlsVisible(true)
        uiHandler.postDelayed(hideFullScreenControls, FULL_SCREEN_CONTROLS_TIMEOUT_MS)
    }

    private fun setFullScreenControlsVisible(visible: Boolean) {
        if (_binding == null) return
        val controls = listOf(
            binding.fullScreenTopBar,
            binding.viewerControlContainer,
            binding.zoomControls,
            binding.orientationIndicator
        )
        controls.forEach { control ->
            control.animate().cancel()
            if (visible) {
                control.visibility = View.VISIBLE
                control.animate().alpha(1f).setDuration(140L).start()
            } else {
                control.animate()
                    .alpha(0f)
                    .setDuration(180L)
                    .withEndAction { if (isFullScreen) control.visibility = View.GONE }
                    .start()
            }
        }
    }

    private fun showFullScreenHintIfNeeded() {
        val preferences = requireContext().getSharedPreferences(PREFERENCES_NAME, 0)
        if (preferences.getBoolean(PREFERENCE_FULL_SCREEN_HINT_SHOWN, false)) return
        preferences.edit().putBoolean(PREFERENCE_FULL_SCREEN_HINT_SHOWN, true).apply()
        binding.fullScreenHint.alpha = 1f
        binding.fullScreenHint.visibility = View.VISIBLE
        binding.fullScreenHint.bringToFront()
        uiHandler.removeCallbacks(hideFullScreenHint)
        uiHandler.postDelayed(hideFullScreenHint, FULL_SCREEN_HINT_TIMEOUT_MS)
    }

    private fun updateOrientationTopMargin(topMargin: Int) {
        val layoutParams =
            binding.orientationIndicator.layoutParams as? FrameLayout.LayoutParams ?: return
        layoutParams.topMargin = topMargin
        binding.orientationIndicator.layoutParams = layoutParams
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        val window = activity?.window ?: return
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
            if (visible) {
                show(WindowInsetsCompat.Type.systemBars())
            } else {
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    private fun configureExpanders() {
        binding.modelSelectorHeader.setOnClickListener {
            binding.modelDiscoveryPanel.visibility =
                toggledVisibility(binding.modelDiscoveryPanel.visibility)
            val icon =
                if (binding.modelDiscoveryPanel.visibility == View.VISIBLE) {
                    R.drawable.ic_chevron_up
                } else {
                    R.drawable.ic_chevron_down
                }
            binding.modelSelectorHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
        }

        binding.partsHeader.setOnClickListener {
            val nextVisibility = toggledVisibility(binding.partsList.visibility)
            binding.partsList.visibility = nextVisibility
            binding.infoPanel.visibility = nextVisibility
            val icon =
                if (nextVisibility == View.VISIBLE) {
                    R.drawable.ic_chevron_up
                } else {
                    R.drawable.ic_chevron_down
                }
            binding.partsHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
        }
    }

    private fun configureModelDiscovery() {
        recentModelIndices.clear()
        val savedRecent = requireContext()
            .getSharedPreferences(PREFERENCES_NAME, 0)
            .getString(PREFERENCE_RECENT_MODELS, "")
            .orEmpty()
        recentModelIndices += savedRecent
            .split(",")
            .mapNotNull(String::toIntOrNull)
            .filter { it in MODEL_CATALOG.indices }
            .distinct()
            .take(MAX_RECENT_MODELS)

        FILTERS.forEach { filter ->
            binding.filterStrip.addView(createFilterChip(filter))
        }
        binding.modelSearch.doAfterTextChanged {
            modelQuery = it?.toString().orEmpty().trim()
            renderModelCatalog()
        }

        renderModelCatalog()
        selectModel(recentModelIndices.firstOrNull() ?: 0)
    }

    private fun createFilterChip(filter: String): TextView =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                36.dp
            ).apply {
                marginEnd = 7.dp
            }
            minWidth = 68.dp
            gravity = Gravity.CENTER
            setPadding(13.dp, 0, 13.dp, 0)
            text = filter
            textSize = 12f
            setTextColor(Color.WHITE)
            background = requireContext().getDrawable(
                if (filter == activeFilter) {
                    R.drawable.bg_filter_chip_selected
                } else {
                    R.drawable.bg_filter_chip
                }
            )
            setOnClickListener {
                activeFilter = filter
                updateFilterChips()
                renderModelCatalog()
            }
        }

    private fun updateFilterChips() {
        repeat(binding.filterStrip.childCount) { childIndex ->
            val chip = binding.filterStrip.getChildAt(childIndex) as? TextView ?: return@repeat
            chip.background = requireContext().getDrawable(
                if (chip.text.toString() == activeFilter) {
                    R.drawable.bg_filter_chip_selected
                } else {
                    R.drawable.bg_filter_chip
                }
            )
        }
    }

    private fun renderModelCatalog() {
        binding.modelStrip.removeAllViews()
        MODEL_CATALOG.forEachIndexed { index, model ->
            val matchesFilter =
                activeFilter == FILTER_ALL || modelCategory(model) == activeFilter
            val matchesQuery =
                modelQuery.isBlank() ||
                    model.title.contains(modelQuery, ignoreCase = true) ||
                    model.shortTitle.contains(modelQuery, ignoreCase = true)
            if (matchesFilter && matchesQuery) {
                binding.modelStrip.addView(createModelStripItem(index, model))
            }
        }

        if (binding.modelStrip.childCount == 0) {
            binding.modelStrip.addView(
                TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(260.dp, 90.dp)
                    gravity = Gravity.CENTER
                    text = "No models match this search"
                    setTextColor(inactiveTextColor)
                    textSize = 14f
                }
            )
        }

        binding.recentStrip.removeAllViews()
        recentModelIndices.forEach { index ->
            binding.recentStrip.addView(createModelStripItem(index, MODEL_CATALOG[index]))
        }
        val recentVisibility =
            if (recentModelIndices.isEmpty()) View.GONE else View.VISIBLE
        binding.recentModelsLabel.visibility = recentVisibility
        binding.recentStripContainer.visibility = recentVisibility
        updateModelSelectionStyles()
    }

    private fun createModelStripItem(index: Int, model: BiologyModel): View {
        val available = isModelAvailable(model.fileName)
        val item = LinearLayout(requireContext()).apply {
            tag = index
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = if (available) 1f else 0.58f
            contentDescription =
                if (available) model.title else "${model.title}, download required"
            setPadding(4.dp, 0, 4.dp, 0)
            layoutParams = LinearLayout.LayoutParams(126.dp, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { selectModel(index) }
        }

        val preview = FrameLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(112.dp, 68.dp)
            background = requireContext().getDrawable(
                if (index == selectedModelIndex) {
                    R.drawable.bg_thumbnail_selected
                } else {
                    R.drawable.bg_thumbnail
                }
            )
        }

        val image = ImageView(requireContext()).apply {
            tag = THUMBNAIL_IMAGE_TAG
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setPadding(3.dp, 3.dp, 3.dp, 3.dp)
        }
        val placeholder = TextView(requireContext()).apply {
            tag = THUMBNAIL_PLACEHOLDER_TAG
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            text = model.badge
            setTextColor(Color.WHITE)
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
        }
        preview.addView(image)
        preview.addView(placeholder)
        loadCachedThumbnail(index, image, placeholder)

        val label = TextView(requireContext()).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
            maxLines = 2
            text = model.shortTitle
            setTextColor(if (index == selectedModelIndex) selectedTextColor else inactiveTextColor)
            textSize = 11f
        }

        val status = TextView(requireContext()).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
            text = if (available) "READY" else "NEEDS FILE"
            setTextColor(if (available) readyTextColor else inactiveTextColor)
            textSize = 9f
            typeface = Typeface.DEFAULT_BOLD
        }

        item.addView(preview)
        item.addView(label)
        item.addView(status)
        return item
    }

    private fun configurePartList(model: BiologyModel) {
        selectedPartIndex = 0
        binding.partsList.removeAllViews()
        model.parts.forEachIndexed { index, part ->
            binding.partsList.addView(createPartRow(index, part))
        }
    }

    private fun createPartRow(index: Int, part: AnatomyPart): View {
        return TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                50.dp
            ).apply {
                bottomMargin = 6.dp
            }
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14.dp, 0, 12.dp, 0)
            text = "${index + 1}   ${part.title}"
            setTextColor(Color.WHITE)
            textSize = 15f
            maxLines = 1
            background = requireContext().getDrawable(
                if (index == selectedPartIndex) {
                    R.drawable.bg_part_row_selected
                } else {
                    R.drawable.bg_part_row
                }
            )
            setOnClickListener {
                selectPart(index, updateViewer = true)
                showPartBottomSheet()
            }
        }
    }

    private fun selectModel(index: Int) {
        selectedModelIndex = index
        val model = MODEL_CATALOG[index]

        addRecentModel(index)
        updateModelSelectionStyles()
        configurePartList(model)
        selectPart(0, updateViewer = false)
        binding.fullScreenTitle.text = model.title
        loadModel(model)
    }

    private fun addRecentModel(index: Int) {
        recentModelIndices.remove(index)
        recentModelIndices.add(0, index)
        while (recentModelIndices.size > MAX_RECENT_MODELS) {
            recentModelIndices.removeLast()
        }
        requireContext()
            .getSharedPreferences(PREFERENCES_NAME, 0)
            .edit()
            .putString(PREFERENCE_RECENT_MODELS, recentModelIndices.joinToString(","))
            .apply()

        binding.recentStrip.removeAllViews()
        recentModelIndices.forEach { recentIndex ->
            binding.recentStrip.addView(
                createModelStripItem(recentIndex, MODEL_CATALOG[recentIndex])
            )
        }
        binding.recentModelsLabel.visibility = View.VISIBLE
        binding.recentStripContainer.visibility = View.VISIBLE
    }

    private fun updateModelSelectionStyles() {
        updateModelSelectionStyles(binding.modelStrip)
        updateModelSelectionStyles(binding.recentStrip)
    }

    private fun updateModelSelectionStyles(strip: LinearLayout) {
        repeat(strip.childCount) { childIndex ->
            val item = strip.getChildAt(childIndex) as? LinearLayout ?: return@repeat
            val modelIndex = item.tag as? Int ?: return@repeat
            val preview = item.getChildAt(0) as? FrameLayout
            val label = item.getChildAt(1) as? TextView
            preview?.background = requireContext().getDrawable(
                if (modelIndex == selectedModelIndex) {
                    R.drawable.bg_thumbnail_selected
                } else {
                    R.drawable.bg_thumbnail
                }
            )
            label?.setTextColor(
                if (modelIndex == selectedModelIndex) selectedTextColor else inactiveTextColor
            )
        }
    }

    private fun loadCachedThumbnail(
        modelIndex: Int,
        image: ImageView,
        placeholder: TextView
    ) {
        val thumbnail = thumbnailFile(modelIndex)
        if (!thumbnail.isFile || thumbnail.length() == 0L) return
        val bitmap = BitmapFactory.decodeFile(thumbnail.absolutePath) ?: return
        image.setImageBitmap(bitmap)
        placeholder.visibility = View.GONE
    }

    private fun updateVisibleThumbnails(modelIndex: Int, imageBytes: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) ?: return
        listOf(binding.modelStrip, binding.recentStrip).forEach { strip ->
            repeat(strip.childCount) { childIndex ->
                val item = strip.getChildAt(childIndex) as? LinearLayout ?: return@repeat
                if (item.tag != modelIndex) return@repeat
                val preview = item.getChildAt(0) as? FrameLayout ?: return@repeat
                val image = preview.findViewWithTag<ImageView>(THUMBNAIL_IMAGE_TAG)
                val placeholder =
                    preview.findViewWithTag<TextView>(THUMBNAIL_PLACEHOLDER_TAG)
                image?.setImageBitmap(bitmap)
                placeholder?.visibility = View.GONE
            }
        }
    }

    private fun thumbnailFile(modelIndex: Int): File {
        val directory = File(requireContext().cacheDir, THUMBNAIL_DIRECTORY).apply { mkdirs() }
        return File(directory, "$modelIndex.png")
    }

    private fun modelCategory(model: BiologyModel): String =
        when (model.fileName) {
            "Bacteriacell.glb" -> FILTER_MICROBIOLOGY
            "PlantCell.glb",
            "Chloroplast.glb",
            "plant cell wall.glb",
            "Vacuole.glb" -> FILTER_PLANTS
            "Neuron.glb",
            "WhiteBloodCell.glb",
            "epithelial microvilli.glb" -> FILTER_HUMAN
            else -> FILTER_ORGANELLES
        }

    private fun selectPart(index: Int, updateViewer: Boolean) {
        val model = MODEL_CATALOG[selectedModelIndex]
        if (index !in model.parts.indices) return
        selectedPartIndex = index

        model.parts.forEachIndexed { childIndex, _ ->
            binding.partsList.getChildAt(childIndex)?.background =
                requireContext().getDrawable(
                    if (childIndex == selectedPartIndex) {
                        R.drawable.bg_part_row_selected
                    } else {
                        R.drawable.bg_part_row
                    }
                )
        }

        val part = model.parts[index]
        binding.featureNumber.text = (index + 1).toString()
        binding.featureTitle.text = part.title
        binding.featureDescription.text = part.description

        if (updateViewer) {
            binding.modelWebView.evaluateJavascript("window.focusPart($index)", null)
        }
    }

    private fun showPartBottomSheet() {
        if (_binding == null) return
        partBottomSheet?.dismiss()

        val model = MODEL_CATALOG[selectedModelIndex]
        val part = model.parts[selectedPartIndex]
        val dialog = BottomSheetDialog(requireContext())
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 16.dp, 20.dp, 24.dp)
            background = requireContext().getDrawable(R.drawable.bg_surface_panel)
        }

        content.addView(
            TextView(requireContext()).apply {
                text = "${selectedPartIndex + 1} of ${model.parts.size}  •  ${model.title}"
                setTextColor(readyTextColor)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }
        )
        content.addView(
            TextView(requireContext()).apply {
                text = part.title
                setTextColor(Color.WHITE)
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 7.dp, 0, 8.dp)
            }
        )
        content.addView(
            TextView(requireContext()).apply {
                text = part.description
                setTextColor(Color.parseColor("#C7D4E7"))
                textSize = 16f
                setLineSpacing(3.dp.toFloat(), 1f)
            }
        )

        val showAllSwitch = SwitchCompat(requireContext()).apply {
            text = "Show all visible labels"
            setTextColor(Color.WHITE)
            textSize = 14f
            isChecked = showAllLabels
            setPadding(0, 14.dp, 0, 4.dp)
            setOnCheckedChangeListener { _, enabled ->
                showAllLabels = enabled
                runViewerCommand("setShowAllLabels($enabled)")
            }
        }
        val identifySwitch = SwitchCompat(requireContext()).apply {
            text = "Identify mode"
            setTextColor(Color.WHITE)
            textSize = 14f
            isChecked = identifyMode
            setPadding(0, 4.dp, 0, 10.dp)
            setOnCheckedChangeListener { _, enabled ->
                identifyMode = enabled
                runViewerCommand("setIdentifyMode($enabled)")
            }
        }
        content.addView(showAllSwitch)
        content.addView(identifySwitch)

        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        actions.addView(
            createSheetAction("Previous", selectedPartIndex > 0) {
                selectPart(selectedPartIndex - 1, updateViewer = true)
                showPartBottomSheet()
            }
        )
        actions.addView(
            createSheetAction("Next", selectedPartIndex < model.parts.lastIndex) {
                selectPart(selectedPartIndex + 1, updateViewer = true)
                showPartBottomSheet()
            }
        )
        actions.addView(
            createSheetAction("Done", true) {
                dialog.dismiss()
            }
        )
        content.addView(actions)

        dialog.setContentView(content)
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = false
            peekHeight = 250.dp
        }
        dialog.setOnDismissListener {
            if (partBottomSheet === dialog) partBottomSheet = null
        }
        partBottomSheet = dialog
        dialog.show()
    }

    private fun createSheetAction(
        title: String,
        enabled: Boolean,
        action: () -> Unit
    ): TextView =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(0, 46.dp, 1f).apply {
                marginEnd = 7.dp
            }
            gravity = Gravity.CENTER
            text = title
            setTextColor(if (enabled) Color.WHITE else inactiveTextColor)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            alpha = if (enabled) 1f else 0.45f
            isEnabled = enabled
            background = requireContext().getDrawable(
                if (title == "Done") R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip
            )
            setOnClickListener { action() }
        }

    private fun loadModel(model: BiologyModel) {
        binding.modelTitle.text = model.title
        val storedModel = findStoredModel(model.fileName)
        val bundled = isBundledModelAvailable(model.fileName)
        val available = storedModel != null || bundled
        binding.modelAvailability.text = if (available) "READY" else "NEEDS FILE"
        binding.modelAvailability.setTextColor(
            if (available) readyTextColor else inactiveTextColor
        )

        val partsJson = JSONArray().apply {
            model.parts.forEach { part ->
                put(
                    JSONObject()
                        .put("title", part.title)
                        .put("position", part.position)
                        .put("normal", part.normal)
                )
            }
        }.toString()

        val source = when {
            storedModel != null ->
                "https://$MODEL_HOST/models/${Uri.encode(model.fileName)}"
            bundled ->
                "biology/3d/${Uri.encode(model.fileName)}"
            else -> null
        }

        binding.viewerStatusOverlay.visibility = View.VISIBLE
        binding.viewerStatusOverlay.bringToFront()
        binding.modelProgress.visibility = if (available) View.VISIBLE else View.GONE
        binding.viewerStatusActions.visibility = if (available) View.GONE else View.VISIBLE
        binding.viewerStatusText.text =
            if (available) "Preparing ${model.title}…" else "${model.title} is not available."
        isAutoRotating = true
        updateRotationControl()
        binding.zoomLevel.text = "100%"
        binding.orientationIndicator.text = "FRONT"
        updateZoomControls(100)
        val query = buildString {
            append("file:///android_asset/model_viewer.html?")
            if (source != null) {
                append("src=${Uri.encode(source)}")
            } else {
                append("missing=1")
            }
            append("&title=${Uri.encode(model.title)}")
            append("&parts=${Uri.encode(partsJson)}")
            append("&modelIndex=$selectedModelIndex")
            append("&rotationSpeed=${ROTATION_SPEEDS[rotationSpeedIndex]}")
            append("&identifyMode=${if (identifyMode) 1 else 0}")
            append("&showAllLabels=${if (showAllLabels) 1 else 0}")
        }
        binding.modelWebView.loadUrl(query)
    }

    private fun showViewerError(message: String) {
        binding.viewerStatusOverlay.visibility = View.VISIBLE
        binding.viewerStatusOverlay.bringToFront()
        binding.modelProgress.visibility = View.GONE
        binding.viewerStatusActions.visibility = View.VISIBLE
        binding.viewerStatusText.text = message
    }

    private fun updateZoomControls(zoomPercent: Int) {
        binding.zoomLevel.text = "$zoomPercent%"
        val canZoomIn = zoomPercent < MAX_ZOOM_PERCENT
        val canZoomOut = zoomPercent > MIN_ZOOM_PERCENT
        binding.zoomInButton.isEnabled = canZoomIn
        binding.zoomInButton.alpha = if (canZoomIn) 1f else 0.35f
        binding.zoomOutButton.isEnabled = canZoomOut
        binding.zoomOutButton.alpha = if (canZoomOut) 1f else 0.35f
    }

    private fun toggledVisibility(currentVisibility: Int): Int =
        if (currentVisibility == View.VISIBLE) View.GONE else View.VISIBLE

    private fun findStoredModel(fileName: String): File? {
        val modelFile = File(modelStorageDirectory, fileName)
        return modelFile.takeIf { it.exists() && it.length() > 0 }
    }

    private fun isModelAvailable(fileName: String): Boolean =
        findStoredModel(fileName) != null || isBundledModelAvailable(fileName)

    private fun isBundledModelAvailable(fileName: String): Boolean =
        requireContext().assets
            .list(MODEL_ASSET_DIRECTORY)
            ?.contains(fileName) == true

    private inner class ModelBridge {
        @JavascriptInterface
        fun onPartSelected(index: Int) {
            activity?.runOnUiThread {
                if (_binding != null) {
                    selectPart(index, updateViewer = false)
                    showPartBottomSheet()
                }
            }
        }

        @JavascriptInterface
        fun onViewerTapped() {
            activity?.runOnUiThread {
                if (_binding != null && isFullScreen) showFullScreenControls()
            }
        }

        @JavascriptInterface
        fun onModelState(state: String, detail: String) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                when (state) {
                    "loaded" -> binding.viewerStatusOverlay.visibility = View.GONE
                    "error" -> showViewerError(
                        detail.ifBlank { "The 3D model could not be opened." }
                    )
                }
            }
        }

        @JavascriptInterface
        fun onCameraState(zoomPercent: Int, orientation: String) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                updateZoomControls(zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT))
                binding.orientationIndicator.text = orientation.uppercase()
            }
        }

        @JavascriptInterface
        fun onThumbnailReady(modelIndex: Int, encodedImage: String) {
            if (modelIndex !in MODEL_CATALOG.indices || encodedImage.isBlank()) return
            val imageBytes = runCatching {
                Base64.decode(encodedImage, Base64.DEFAULT)
            }.getOrNull() ?: return
            if (imageBytes.isEmpty()) return

            runCatching {
                FileOutputStream(thumbnailFile(modelIndex)).use { output ->
                    output.write(imageBytes)
                }
            }.onSuccess {
                activity?.runOnUiThread {
                    if (_binding != null) updateVisibleThumbnails(modelIndex, imageBytes)
                }
            }
        }
    }

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "biology/3d"
        const val THUMBNAIL_DIRECTORY = "model-thumbnails"
        const val MODEL_HOST = "biology.local"
        const val GLB_MIME_TYPE = "model/gltf-binary"
        const val BRIDGE_NAME = "BiologyBridge"
        const val PREFERENCES_NAME = "biology_explorer"
        const val PREFERENCE_RECENT_MODELS = "recent_models"
        const val PREFERENCE_FULL_SCREEN_HINT_SHOWN = "full_screen_hint_shown"
        const val MAX_RECENT_MODELS = 5
        const val FULL_SCREEN_CONTROLS_TIMEOUT_MS = 3_000L
        const val FULL_SCREEN_HINT_TIMEOUT_MS = 4_500L
        const val THUMBNAIL_IMAGE_TAG = "thumbnail_image"
        const val THUMBNAIL_PLACEHOLDER_TAG = "thumbnail_placeholder"
        const val FILTER_ALL = "All"
        const val FILTER_ORGANELLES = "Organelles"
        const val FILTER_PLANTS = "Plants"
        const val FILTER_HUMAN = "Human"
        const val FILTER_MICROBIOLOGY = "Microbiology"
        const val MIN_ZOOM_PERCENT = 35
        const val MAX_ZOOM_PERCENT = 300

        val selectedTextColor: Int = Color.parseColor("#AFA3FF")
        val inactiveTextColor: Int = Color.parseColor("#98A8C2")
        val readyTextColor: Int = Color.parseColor("#73E8C5")
        val FILTERS =
            listOf(FILTER_ALL, FILTER_ORGANELLES, FILTER_PLANTS, FILTER_HUMAN, FILTER_MICROBIOLOGY)
        val ROTATION_SPEEDS = listOf(10, 18, 30)
        val ROTATION_SPEED_LABELS = listOf("0.5×", "1×", "1.5×")
        val CAMERA_PRESETS = listOf(
            CameraPreset("front", "FR", "Front"),
            CameraPreset("right", "RT", "Right"),
            CameraPreset("back", "BK", "Back"),
            CameraPreset("top", "TOP", "Top")
        )

        fun part(
            title: String,
            description: String,
            position: String,
            normal: String = "0 0 1"
        ) = AnatomyPart(title, description, position, normal)

        val MODEL_CATALOG = listOf(
            BiologyModel(
                "Bacteriacell.glb", "Bacteria Cell", "Bacteria", "BC",
                listOf(
                    part("Capsule", "The outer capsule helps some bacteria adhere to surfaces and resist drying.", "-0.72 0.04 0.31"),
                    part("Cell wall", "A rigid layer that preserves shape and protects the cell from osmotic pressure.", "-0.49 -0.03 0.39"),
                    part("Cell membrane", "The selective membrane controls transport and hosts energy-producing reactions.", "-0.18 0.02 0.41"),
                    part("Nucleoid", "The nucleoid is the region containing the bacterium's circular DNA.", "0.17 0.02 0.40"),
                    part("Ribosomes", "Ribosomes translate genetic instructions to build proteins.", "0.55 -0.04 0.31"),
                    part("Flagellum", "The flagellum rotates to propel a motile bacterium.", "0.88 0.01 0.08", "1 0 0")
                )
            ),
            BiologyModel(
                "Cell Membrane.glb", "Cell Membrane", "Membrane", "CM",
                listOf(
                    part("Phospholipid heads", "Water-attracting heads face the fluid inside and outside the cell.", "-0.55 0.10 0.78"),
                    part("Fatty acid tails", "Water-repelling tails form the membrane's hydrophobic inner core.", "-0.15 -0.02 0.83"),
                    part("Channel protein", "Channel proteins provide selective pathways through the membrane.", "0.27 0.05 0.79"),
                    part("Glycoprotein", "Carbohydrate-tagged proteins support recognition and cell signaling.", "0.62 0.13 0.62")
                )
            ),
            BiologyModel(
                "Chloroplast.glb", "Chloroplast", "Chloroplast", "CH",
                listOf(
                    part("Outer membrane", "The outer membrane forms the chloroplast's external boundary.", "-0.64 0.18 0.55"),
                    part("Stroma", "The stroma contains enzymes used to build sugars from carbon dioxide.", "-0.22 0.04 0.65"),
                    part("Granum", "A granum is a stack of thylakoids where light-dependent reactions occur.", "0.20 0.12 0.64"),
                    part("Stroma lamella", "Lamellae connect grana and organize the thylakoid membrane system.", "0.57 -0.08 0.50")
                )
            ),
            BiologyModel(
                "epithelial microvilli.glb", "Epithelial Microvilli", "Microvilli", "MV",
                listOf(
                    part("Microvilli", "Finger-like projections increase the surface area available for absorption.", "-0.45 0.30 0.78"),
                    part("Actin core", "Parallel actin filaments support each microvillus.", "-0.05 0.12 0.88"),
                    part("Terminal web", "The terminal web anchors microvilli to the cell cortex.", "0.34 -0.08 0.80"),
                    part("Cell surface", "The apical membrane faces the lumen or external environment.", "0.62 -0.25 0.55")
                )
            ),
            BiologyModel(
                "Lysosome.glb", "Lysosome", "Lysosome", "LY",
                listOf(
                    part("Lysosomal membrane", "A single membrane isolates powerful digestive enzymes from the cytoplasm.", "-0.57 0.22 0.72"),
                    part("Acidic lumen", "Proton pumps maintain an acidic interior where lysosomal enzymes work best.", "-0.10 0.04 0.92"),
                    part("Hydrolytic enzymes", "Enzymes break proteins, lipids, nucleic acids, and carbohydrates into reusable units.", "0.42 -0.15 0.78")
                )
            ),
            BiologyModel(
                "Mitochondrion.glb", "Mitochondrion", "Mitochondrion", "MT",
                listOf(
                    part("Outer membrane", "The smooth outer membrane encloses the organelle.", "-0.67 0.08 0.38"),
                    part("Intermembrane space", "Protons accumulate here to power ATP synthase.", "-0.35 -0.02 0.48"),
                    part("Cristae", "Folds of the inner membrane provide a large surface for electron transport.", "0.08 0.06 0.51"),
                    part("Matrix", "The matrix contains enzymes for the citric acid cycle and mitochondrial DNA.", "0.48 -0.04 0.38")
                )
            ),
            BiologyModel(
                "Neuron.glb", "Neuron", "Neuron", "NE",
                listOf(
                    part("Dendrites", "Dendrites receive signals from other neurons.", "-0.73 0.05 0.56"),
                    part("Cell body", "The soma contains the nucleus and maintains the neuron.", "-0.38 0.02 0.77"),
                    part("Axon", "The axon conducts electrical impulses away from the cell body.", "0.18 0.01 0.80"),
                    part("Axon terminals", "Terminals release neurotransmitters to communicate with target cells.", "0.76 -0.03 0.48")
                )
            ),
            BiologyModel(
                "plant cell wall.glb", "Plant Cell Wall", "Cell Wall", "CW",
                listOf(
                    part("Primary wall", "A flexible cellulose-rich wall permits growth while providing support.", "-0.59 0.20 0.62"),
                    part("Middle lamella", "A pectin-rich layer glues adjacent plant cells together.", "-0.18 0.04 0.70"),
                    part("Secondary wall", "Some cells deposit a stronger secondary wall inside the primary wall.", "0.26 -0.04 0.67"),
                    part("Plasmodesmata", "Microscopic channels connect the cytoplasm of neighboring plant cells.", "0.62 -0.18 0.48")
                )
            ),
            BiologyModel(
                "PlantCell.glb", "Plant Cell", "Plant Cell", "PC",
                listOf(
                    part("Cell wall", "The rigid cellulose wall supports and protects the plant cell.", "-0.68 0.11 0.66"),
                    part("Cell membrane", "A selective membrane lies just inside the cell wall.", "-0.48 -0.04 0.77"),
                    part("Nucleus", "The nucleus stores DNA and coordinates cell activity.", "-0.19 0.05 0.84"),
                    part("Chloroplast", "Chloroplasts capture light energy for photosynthesis.", "0.10 0.10 0.83"),
                    part("Central vacuole", "The large vacuole stores water and maintains turgor pressure.", "0.38 -0.02 0.76"),
                    part("Mitochondrion", "Mitochondria release usable energy from nutrients.", "0.67 -0.10 0.56")
                )
            ),
            BiologyModel(
                "Ribosomes.glb", "Ribosomes", "Ribosomes", "RB",
                listOf(
                    part("Large subunit", "The large subunit catalyzes peptide-bond formation.", "-0.40 0.18 0.78"),
                    part("Small subunit", "The small subunit binds and reads messenger RNA.", "0.08 -0.05 0.90"),
                    part("mRNA channel", "Messenger RNA passes between the subunits during translation.", "0.48 -0.16 0.72")
                )
            ),
            BiologyModel(
                "Rough Endoplasmic Reticulum.glb", "Rough Endoplasmic Reticulum", "Rough ER", "ER",
                listOf(
                    part("Cisternae", "Flattened membrane sacs create spaces for protein folding and processing.", "-0.55 0.20 0.66"),
                    part("Bound ribosomes", "Attached ribosomes synthesize proteins entering the secretory pathway.", "-0.12 0.10 0.82"),
                    part("ER lumen", "The lumen supports protein folding and quality control.", "0.28 -0.04 0.79"),
                    part("Transport vesicle", "Vesicles carry processed proteins from the ER toward the Golgi apparatus.", "0.63 -0.18 0.56")
                )
            ),
            BiologyModel(
                "Smooth Endoplasmic Reticulum.glb", "Smooth Endoplasmic Reticulum", "Smooth ER", "SE",
                listOf(
                    part("Tubule network", "Interconnected membrane tubules provide a large reaction surface.", "-0.50 0.32 0.72"),
                    part("ER membrane", "Membrane enzymes synthesize lipids and help detoxify compounds.", "-0.05 0.08 0.92"),
                    part("ER lumen", "The interior stores calcium ions and transports newly made molecules.", "0.48 -0.25 0.68")
                )
            ),
            BiologyModel(
                "Vacuole.glb", "Vacuole", "Vacuole", "VA",
                listOf(
                    part("Tonoplast", "The tonoplast is the vacuole's selective surrounding membrane.", "-0.52 0.20 0.56"),
                    part("Cell sap", "The internal solution stores water, ions, pigments, and other solutes.", "-0.05 0.02 0.65"),
                    part("Transport proteins", "Tonoplast proteins control movement between the vacuole and cytoplasm.", "0.50 -0.18 0.53")
                )
            ),
            BiologyModel(
                "WhiteBloodCell.glb", "White Blood Cell", "White Blood Cell", "WB",
                listOf(
                    part("Cell membrane", "A flexible membrane lets the cell change shape and move through tissues.", "-0.57 0.18 0.76"),
                    part("Nucleus", "Nuclear shape helps distinguish different types of white blood cell.", "-0.12 0.05 0.93"),
                    part("Cytoplasm", "The cytoplasm contains machinery used for movement and immune responses.", "0.30 -0.08 0.88"),
                    part("Granules", "In granulocytes, enzyme-filled granules help destroy pathogens.", "0.62 -0.22 0.64")
                )
            )
        )
    }
}

private data class BiologyModel(
    val fileName: String,
    val title: String,
    val shortTitle: String,
    val badge: String,
    val parts: List<AnatomyPart>
)

private data class AnatomyPart(
    val title: String,
    val description: String,
    val position: String,
    val normal: String
)

private data class CameraPreset(
    val key: String,
    val shortLabel: String,
    val title: String
)

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
