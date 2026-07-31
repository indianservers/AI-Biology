package com.indianservers.AIbiology

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.content.pm.ActivityInfo
import android.util.Base64
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.indianservers.AIbiology.databinding.FragmentFirstBinding
import com.indianservers.AIbiology.data.AnatomyPart
import com.indianservers.AIbiology.data.BiologyCategories
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.CameraPreset
import com.indianservers.AIbiology.data.ModelPart
import com.indianservers.AIbiology.data.ModelRepository
import com.indianservers.AIbiology.data.RemoteBiologyCatalogRepository
import com.indianservers.AIbiology.ui.ModelLibraryBottomSheet
import com.indianservers.AIbiology.ui.DeviceProfile
import com.indianservers.AIbiology.ui.TvFocus
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("DEPRECATION")
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private val MODEL_CATALOG = BUILT_IN_MODEL_CATALOG.toMutableList()
    private var selectedModelIndex = 0
    private var selectedPartIndex = 0
    private var isAutoRotating = true
    private var rotationSpeedIndex = 1
    private var cameraPresetIndex = 0
    private var currentModelAvailable = false
    private var hasAutoOpenedModelLibrary = false
    private var isFullScreen = false
    private var showAllLabels = true
    private var identifyMode = false
    private var catalogReady = false
    private var currentMode = ExplorerMode.EXPLORE
    private var modelDiscoveryExpanded = true
    private var modelOverviewExpanded = true
    private var partsExpanded = true
    private var readingLevel = ReadingLevel.STUDENT
    private var reducedMotion = false
    private var highContrast = false
    private var largerLabels = false
    private var screenReaderMode = false
    private var quizModelIndex = -1
    private var quizQuestionIndex = 0
    private var quizScore = 0
    private var quizSelectedAnswer: Int? = null
    private var quizQuestions = emptyList<QuizQuestion>()
    private val quizAwardedQuestions = mutableSetOf<Int>()
    private val bookmarkedParts = mutableSetOf<String>()
    private val bookmarkedModels = mutableSetOf<Int>()
    private val uiHandler = Handler(Looper.getMainLooper())
    private val hideFullScreenControls = Runnable { setFullScreenControlsVisible(false) }
    private val hideFullScreenHint = Runnable {
        _binding?.fullScreenHint?.let { hint ->
            if (reducedMotion) {
                hint.alpha = 0f
                hint.visibility = View.GONE
            } else {
                hint.animate().alpha(0f).withEndAction {
                    hint.visibility = View.GONE
                }.start()
            }
        }
    }
    private var partBottomSheet: BottomSheetDialog? = null
    private var activeFilter = FILTER_ALL
    private var modelQuery = ""
    private val recentModelIndices = mutableListOf<Int>()
    private var originalViewerParent: ViewGroup? = null
    private var originalViewerIndex = -1
    private var originalViewerLayoutParams: ViewGroup.LayoutParams? = null
    private var tabletTwoPaneContainer: LinearLayout? = null
    private var tabletPartsColumn: LinearLayout? = null
    private var tabletViewerColumn: LinearLayout? = null
    private var isTelevision = false
    private var textToSpeech: TextToSpeech? = null
    private lateinit var modelStorageDirectory: File
    private lateinit var modelRepository: ModelRepository
    private lateinit var remoteCatalogRepository: RemoteBiologyCatalogRepository

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
        isTelevision = DeviceProfile.isTelevision(requireContext())
        modelRepository = ModelRepository(requireContext())
        remoteCatalogRepository = RemoteBiologyCatalogRepository(
            requireContext(),
            BuildConfig.BIOLOGY_CATALOG_URL
        )
        restoreIdentificationSettings()
        restoreBiologyExperienceSettings()
        restoreBookmarks()
        configureTextToSpeech()
        configureInsets()
        setSystemBarsVisible(true)
        configureViewer()
        configureViewerControls()
        binding.anatomyButton.setOnClickListener {
            if (!findNavController().popBackStack()) {
                findNavController().navigate(R.id.HomeFragment)
            }
        }
        configureExpanders()
        configureBiologyExperience()
        configureAccessibility()
        configureTabletTwoPane()
        configureTelevisionExperience()
        configureModelDiscovery()
        loadRemoteCatalog()
        configureLearningModes()
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
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        remoteCatalogRepository.close()
        modelRepository.close()
        if (isFullScreen) exitFullScreen()
        binding.modelWebView.removeJavascriptInterface(BRIDGE_NAME)
        binding.modelWebView.destroy()
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
    private fun configureViewer() {
        WebView.setWebContentsDebuggingEnabled(
            requireContext().applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
        )
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
                    val input = findStoredModel(fileName)
                        ?.let(::FileInputStream)
                        ?: runCatching {
                            requireContext().assets.open("$MODEL_ASSET_DIRECTORY/$fileName")
                        }.getOrNull()
                        ?: return null
                    return WebResourceResponse(
                        GLB_MIME_TYPE,
                        null,
                        200,
                        "OK",
                        mapOf("Access-Control-Allow-Origin" to "*"),
                        input
                    )
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    if (
                        _binding != null &&
                        currentModelAvailable &&
                        binding.viewerStatusOverlay.visibility == View.VISIBLE &&
                        binding.modelProgress.visibility == View.VISIBLE
                    ) {
                        binding.viewerStatusText.text =
                            if (newProgress < 100) {
                                "Preparing viewer… $newProgress%"
                            } else {
                                "Loading 3D geometry…"
                            }
                        binding.modelLoadProgress.visibility = View.VISIBLE
                        binding.modelLoadProgress.isIndeterminate = newProgress >= 100
                        binding.modelLoadProgress.progress = newProgress.coerceIn(0, 100)
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
            cameraPresetIndex = 0
            runViewerCommand("resetView()")
            binding.orientationIndicator.text = "Front"
            onViewerControlUsed()
        }
        binding.rotateRightButton.setOnClickListener {
            showPartsPanel()
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
            runViewerCommand("toggleSectionView()")
            onViewerControlUsed()
        }
        binding.rotationSpeedButton.setOnClickListener {
            rotationSpeedIndex = (rotationSpeedIndex + 1) % ROTATION_SPEEDS.size
            updateRotationSpeedControl()
            runViewerCommand("setRotationSpeed(${ROTATION_SPEEDS[rotationSpeedIndex]})")
            onViewerControlUsed()
        }
        binding.cameraViewButton.setOnClickListener {
            runViewerCommand("toggleExplodedView()")
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
        binding.orientationIndicator.setOnClickListener { showOrientationSelector() }
        binding.askAiButton.setOnClickListener { showAskAiPanel() }
        binding.arButton.setOnClickListener {
            runViewerCommand("activateAR()")
            onViewerControlUsed()
        }
        binding.fullScreenClose.setOnClickListener { exitFullScreen() }
        binding.retryModelButton.setOnClickListener {
            loadModel(MODEL_CATALOG[selectedModelIndex])
        }
        binding.closeStatusButton.setOnClickListener {
            binding.viewerStatusOverlay.visibility = View.GONE
        }
        updateRotationSpeedControl()
        updateViewerCapabilities(MODEL_CATALOG[selectedModelIndex])
    }

    private fun updateRotationControl() {
        binding.rotationButton.text = if (isAutoRotating) "II" else ">"
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
        binding.orientationIndicator.text = preset.title
        binding.orientationIndicator.contentDescription =
            "${preset.title} orientation. Tap to choose another orientation."
    }

    private fun updateViewerCapabilities(model: BiologyModel) {
        binding.rotateRightButton.visibility =
            if (model.supportsPartSelection) View.VISIBLE else View.GONE
        binding.cameraViewButton.visibility =
            if (model.supportsExplodedView) View.VISIBLE else View.GONE
        binding.resetViewButton.visibility =
            if (model.supportsSectionView) View.VISIBLE else View.GONE
        binding.arButton.visibility = if (model.supportsAr) View.VISIBLE else View.GONE
    }

    private fun showOrientationSelector() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera orientation")
            .setSingleChoiceItems(
                CAMERA_PRESETS.map(CameraPreset::title).toTypedArray(),
                cameraPresetIndex
            ) { dialog, index ->
                cameraPresetIndex = index
                updateCameraPresetControl()
                runViewerCommand("setCameraPreset('${CAMERA_PRESETS[index].key}')")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        requireActivity().requestedOrientation =
            if (isTelevision) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        if (isTelevision) {
            viewer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            viewer.requestFocus()
        }
        binding.fullScreenTitle.text = MODEL_CATALOG[selectedModelIndex].title
        binding.fullScreenButton.text = "X"
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
        requireActivity().requestedOrientation =
            if (isTelevision) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        uiHandler.removeCallbacks(hideFullScreenControls)
        uiHandler.removeCallbacks(hideFullScreenHint)
        setFullScreenControlsVisible(true)
        binding.fullScreenButton.text = "[]"
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
            if (reducedMotion) {
                control.alpha = if (visible) 1f else 0f
                control.visibility = if (visible) View.VISIBLE else View.GONE
                return@forEach
            }
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
            showModelLibrary()
        }
        modelDiscoveryExpanded = false
        binding.modelDiscoveryPanel.visibility = View.GONE
        binding.modelSelectorHeader.setCompoundDrawablesWithIntrinsicBounds(
            0,
            0,
            R.drawable.ic_chevron_up,
            0
        )

        binding.modelOverviewHeader.setOnClickListener {
            modelOverviewExpanded = !modelOverviewExpanded
            binding.modelOverviewPanel.visibility =
                if (modelOverviewExpanded) View.VISIBLE else View.GONE
            binding.modelOverviewHeader.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                if (modelOverviewExpanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down,
                0
            )
        }

        binding.partsHeader.setOnClickListener {
            if (MODEL_CATALOG[selectedModelIndex].parts.isEmpty()) return@setOnClickListener
            partsExpanded = !partsExpanded
            val nextVisibility = if (partsExpanded) View.VISIBLE else View.GONE
            binding.partsList.visibility = nextVisibility
            binding.infoPanel.visibility = nextVisibility
            val icon =
                if (partsExpanded) {
                    R.drawable.ic_chevron_up
                } else {
                    R.drawable.ic_chevron_down
                }
            binding.partsHeader.setCompoundDrawablesWithIntrinsicBounds(0, 0, icon, 0)
        }
    }

    private fun configureTextToSpeech() {
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.82f)
            }
        }
    }

    private fun configureBiologyExperience() {
        val levels = listOf(
            binding.beginnerLevel to ReadingLevel.BEGINNER,
            binding.studentLevel to ReadingLevel.STUDENT,
            binding.advancedLevel to ReadingLevel.ADVANCED
        )
        levels.forEach { (view, level) ->
            view.setOnClickListener {
                readingLevel = level
                preferences().edit().putString(PREFERENCE_READING_LEVEL, level.name).apply()
                updateReadingLevelTabs()
                updateModelBriefing()
                selectPart(selectedPartIndex, updateViewer = false)
                if (currentMode != ExplorerMode.EXPLORE) renderActiveWorkflow()
            }
        }
        binding.pronounceModelButton.setOnClickListener {
            speakTerm(modelMetadata(MODEL_CATALOG[selectedModelIndex]).pronunciation)
        }
        binding.bookmarkModelButton.setOnClickListener {
            toggleModelBookmark(selectedModelIndex)
        }
        binding.compareModelButton.setOnClickListener { showComparisonSelector() }
        binding.sourceAttribution.setOnClickListener {
            val metadata = modelMetadata(MODEL_CATALOG[selectedModelIndex])
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(metadata.sourceUrl)))
        }
        updateReadingLevelTabs()
    }

    private fun updateReadingLevelTabs() {
        listOf(
            binding.beginnerLevel to ReadingLevel.BEGINNER,
            binding.studentLevel to ReadingLevel.STUDENT,
            binding.advancedLevel to ReadingLevel.ADVANCED
        ).forEach { (view, level) ->
            val selected = readingLevel == level
            view.background = requireContext().getDrawable(
                if (selected) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip
            )
            view.setTextColor(if (selected) Color.WHITE else inactiveTextColor)
            view.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun updateModelBriefing() {
        if (_binding == null) return
        val model = MODEL_CATALOG[selectedModelIndex]
        val metadata = modelMetadata(model)
        binding.commonNameText.text = metadata.commonName
        binding.scientificNameText.text = metadata.scientificName
        setGlossaryText(binding.overviewDescription, overviewForLevel(model))
        binding.bookmarkModelButton.text =
            if (selectedModelIndex in bookmarkedModels) "Saved" else "Bookmark"
        binding.bookmarkModelButton.contentDescription =
            if (selectedModelIndex in bookmarkedModels) {
                "Remove ${model.title} bookmark"
            } else {
                "Bookmark ${model.title}"
            }
        binding.sourceAttribution.text =
            "Source: ${metadata.sourceTitle}  |  Reviewed $CONTENT_REVIEWED_DATE"
        binding.sourceAttribution.contentDescription =
            "Source ${metadata.sourceTitle}. Last reviewed $CONTENT_REVIEWED_DATE. Open source."
    }

    private fun speakTerm(term: String) {
        val engine = textToSpeech
        if (engine == null) {
            Toast.makeText(requireContext(), "Pronunciation is not ready yet", Toast.LENGTH_SHORT)
                .show()
            return
        }
        engine.speak(term, TextToSpeech.QUEUE_FLUSH, null, "biology-term")
    }

    private fun setGlossaryText(view: TextView, value: String) {
        val spannable = SpannableString(value)
        GLOSSARY.forEach { (term, definition) ->
            var start = value.indexOf(term, ignoreCase = true)
            while (start >= 0) {
                val end = start + term.length
                spannable.setSpan(
                    object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            AlertDialog.Builder(requireContext())
                                .setTitle(term.replaceFirstChar { it.uppercase() })
                                .setMessage(definition)
                                .setPositiveButton("Close", null)
                                .show()
                        }

                        override fun updateDrawState(ds: android.text.TextPaint) {
                            ds.color = if (highContrast) Color.YELLOW else readyTextColor
                            ds.isUnderlineText = true
                        }
                    },
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                start = value.indexOf(term, start + term.length, ignoreCase = true)
            }
        }
        view.text = spannable
        view.movementMethod = LinkMovementMethod.getInstance()
        view.highlightColor = Color.TRANSPARENT
    }

    private fun showComparisonSelector() {
        val choices = MODEL_CATALOG.indices
            .filter { it != selectedModelIndex }
        AlertDialog.Builder(requireContext())
            .setTitle("Compare ${MODEL_CATALOG[selectedModelIndex].shortTitle} with")
            .setItems(choices.map { MODEL_CATALOG[it].title }.toTypedArray()) { _, which ->
                showComparison(choices[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showComparison(otherIndex: Int) {
        val currentIndex = selectedModelIndex
        val container = LinearLayout(requireContext()).apply {
            orientation =
                if (resources.configuration.smallestScreenWidthDp >= 600) {
                    LinearLayout.HORIZONTAL
                } else {
                    LinearLayout.VERTICAL
                }
            setPadding(14.dp, 6.dp, 14.dp, 12.dp)
        }
        listOf(currentIndex, otherIndex).forEach { modelIndex ->
            val model = MODEL_CATALOG[modelIndex]
            val metadata = modelMetadata(model)
            val panel = createSurfacePanel(
                topMargin = if (container.orientation == LinearLayout.VERTICAL) 8 else 0
            ).apply {
                layoutParams =
                    if (container.orientation == LinearLayout.HORIZONTAL) {
                        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                            .apply { marginEnd = 7.dp }
                    } else {
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
            }
            panel.addView(createWorkflowText(model.title, 20f, Color.WHITE, true))
            panel.addView(
                createWorkflowText(metadata.scientificName, 13f, readyTextColor, false, 3)
            )
            panel.addView(
                createWorkflowText(overviewForLevel(model), 14f, bodyTextColor, false, 9)
            )
            panel.addView(
                createWorkflowText(
                    "${model.parts.size} identified structures  |  ${modelCategory(model)}",
                    12f,
                    selectedTextColor,
                    true,
                    10
                )
            )
            panel.addView(
                createWorkflowAction("Open model", primary = modelIndex != currentIndex, topMargin = 12) {
                    selectModel(modelIndex)
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        46.dp
                    ).apply { topMargin = 12.dp }
                }
            )
            container.addView(panel)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Biology comparison")
            .setView(container)
            .setPositiveButton("Done", null)
            .show()
    }

    private fun configureAccessibility() {
        binding.accessibilityButton.setOnClickListener { showAccessibilitySettings() }
        applyAccessibilitySettings()
    }

    private fun showAccessibilitySettings() {
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp, 8.dp, 20.dp, 12.dp)
            background = requireContext().getDrawable(R.drawable.bg_surface_panel)
        }
        fun addSwitch(title: String, checked: Boolean, changed: (Boolean) -> Unit) {
            content.addView(
                SwitchCompat(requireContext()).apply {
                    text = title
                    setTextColor(Color.WHITE)
                    textSize = 16f
                    isChecked = checked
                    minHeight = 52.dp
                    setOnCheckedChangeListener { _, enabled -> changed(enabled) }
                }
            )
        }
        addSwitch("Reduced motion", reducedMotion) { reducedMotion = it; saveAccessibilitySettings() }
        addSwitch("High contrast", highContrast) { highContrast = it; saveAccessibilitySettings() }
        addSwitch("Larger labels", largerLabels) { largerLabels = it; saveAccessibilitySettings() }
        addSwitch("Screen reader mode", screenReaderMode) {
            screenReaderMode = it
            saveAccessibilitySettings()
        }
        BottomSheetDialog(requireContext()).apply {
            setContentView(content)
            setOnDismissListener { applyAccessibilitySettings() }
            show()
        }
    }

    private fun saveAccessibilitySettings() {
        preferences().edit()
            .putBoolean(PREFERENCE_REDUCED_MOTION, reducedMotion)
            .putBoolean(PREFERENCE_HIGH_CONTRAST, highContrast)
            .putBoolean(PREFERENCE_LARGER_LABELS, largerLabels)
            .putBoolean(PREFERENCE_SCREEN_READER, screenReaderMode)
            .apply()
    }

    private fun applyAccessibilitySettings() {
        if (_binding == null) return
        if (reducedMotion || screenReaderMode) {
            isAutoRotating = false
            updateRotationControl()
        }
        val labelScale = if (largerLabels) 1.18f else 1f
        binding.featureTitle.textSize = 21f * labelScale
        binding.featureDescription.textSize = 15f * labelScale
        binding.overviewDescription.textSize = 15f * labelScale
        binding.partsHeader.textSize = 17f * labelScale
        binding.modelOverviewHeader.textSize = 17f * labelScale
        binding.root.background =
            if (highContrast) {
                android.graphics.drawable.ColorDrawable(Color.parseColor("#000814"))
            } else {
                requireContext().getDrawable(R.drawable.bg_biology_screen)
            }
        configurePartList(MODEL_CATALOG[selectedModelIndex])
        selectPart(selectedPartIndex.coerceAtMost(MODEL_CATALOG[selectedModelIndex].parts.lastIndex), false)
        runViewerCommand(
            "setAccessibility($reducedMotion,$highContrast,$largerLabels,$screenReaderMode)"
        )
    }

    private fun restoreBiologyExperienceSettings() {
        readingLevel = runCatching {
            ReadingLevel.valueOf(
                preferences().getString(PREFERENCE_READING_LEVEL, ReadingLevel.STUDENT.name)
                    ?: ReadingLevel.STUDENT.name
            )
        }.getOrDefault(ReadingLevel.STUDENT)
        reducedMotion = preferences().getBoolean(PREFERENCE_REDUCED_MOTION, false)
        highContrast = preferences().getBoolean(PREFERENCE_HIGH_CONTRAST, false)
        largerLabels = preferences().getBoolean(PREFERENCE_LARGER_LABELS, false)
        screenReaderMode = preferences().getBoolean(PREFERENCE_SCREEN_READER, false)
    }

    private fun configureTabletTwoPane() {
        if (resources.configuration.smallestScreenWidthDp < 600) return
        val content = binding.contentColumn
        listOf(
            binding.viewerContainer,
            binding.interactionHint,
            binding.partsHeader,
            binding.partsList,
            binding.infoPanel
        ).forEach { (it.parent as? ViewGroup)?.removeView(it) }
        val insertAt =
            (content.indexOfChild(binding.modelOverviewPanel) + 1)
                .coerceIn(0, content.childCount)

        val left = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (isTelevision) 0.28f else 0.38f
            )
            setPadding(0, 0, 8.dp, 0)
            addView(binding.partsHeader)
            addView(binding.partsList)
            addView(binding.infoPanel)
        }
        val televisionViewerHeight =
            (resources.displayMetrics.heightPixels * 0.62f)
                .toInt()
                .coerceIn(320.dp, 520.dp)
        val right = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                if (isTelevision) 0.72f else 0.62f
            )
            setPadding(8.dp, 0, 0, 0)
            addView(
                binding.viewerContainer,
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    if (isTelevision) televisionViewerHeight
                    else resources.getDimensionPixelSize(R.dimen.biology_viewer_height)
                )
            )
            addView(binding.interactionHint)
        }
        tabletPartsColumn = left
        tabletViewerColumn = right
        tabletTwoPaneContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12.dp }
            addView(left)
            addView(right)
        }
        content.addView(tabletTwoPaneContainer, insertAt)
    }

    private fun configureTelevisionExperience() {
        if (!isTelevision) return
        modelOverviewExpanded = false
        modelDiscoveryExpanded = false
        binding.modelOverviewPanel.visibility = View.GONE
        binding.modelOverviewHeader.visibility = View.GONE
        binding.modelDiscoveryPanel.visibility = View.GONE
        binding.modelWebView.isFocusable = false
        binding.modelWebView.isFocusableInTouchMode = false
        binding.viewerContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
        binding.contentScroll.isFocusable = false
        binding.topAppBar.setPadding(36.dp, binding.topAppBar.paddingTop, 36.dp, 0)
        binding.learningModeTabs.setPadding(28.dp, 0, 28.dp, 0)
        binding.contentColumn.setPadding(
            28.dp,
            binding.contentColumn.paddingTop,
            28.dp,
            binding.contentColumn.paddingBottom
        )
        (binding.viewerControlContainer.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.bottomMargin = 34.dp
            binding.viewerControlContainer.layoutParams = it
        }
        (binding.askAiButton.layoutParams as? FrameLayout.LayoutParams)?.let {
            it.bottomMargin = 96.dp
            binding.askAiButton.layoutParams = it
        }
        binding.screenTitle.textSize = 22f
        binding.modelTitle.textSize = 14f
        listOf(
            binding.exploreTab,
            binding.learnTab,
            binding.quizTab,
            binding.notesTab
        ).forEach { it.textSize = 16f }

        listOf(
            binding.anatomyButton,
            binding.microscopyButton,
            binding.libraryButton,
            binding.accessibilityButton,
            binding.exploreTab,
            binding.learnTab,
            binding.quizTab,
            binding.notesTab,
            binding.modelSelectorHeader,
            binding.rotateLeftButton,
            binding.rotateRightButton,
            binding.rotationButton,
            binding.rotationSpeedButton,
            binding.resetViewButton,
            binding.cameraViewButton,
            binding.fullScreenButton,
            binding.zoomInButton,
            binding.zoomOutButton,
            binding.orientationIndicator,
            binding.askAiButton,
            binding.retryModelButton,
            binding.closeStatusButton
        ).forEach { TvFocus.apply(it) }

        TvFocus.apply(binding.viewerContainer, focusedScale = 1.01f)
        binding.viewerContainer.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            if (
                !isFullScreen &&
                keyCode != KeyEvent.KEYCODE_DPAD_CENTER &&
                keyCode != KeyEvent.KEYCODE_ENTER
            ) {
                return@setOnKeyListener false
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> runViewerCommand("rotateBy(-15)")
                KeyEvent.KEYCODE_DPAD_RIGHT -> runViewerCommand("rotateBy(15)")
                KeyEvent.KEYCODE_DPAD_UP -> runViewerCommand("zoomBy(0.88)")
                KeyEvent.KEYCODE_DPAD_DOWN -> runViewerCommand("zoomBy(1.14)")
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER -> {
                    if (isFullScreen) exitFullScreen() else enterFullScreen()
                }
                else -> return@setOnKeyListener false
            }
            onViewerControlUsed()
            true
        }
        binding.viewerContainer.post { binding.viewerContainer.requestFocus() }
    }

    private fun configureLearningModes() {
        binding.exploreTab.setOnClickListener { setLearningMode(ExplorerMode.EXPLORE) }
        binding.learnTab.setOnClickListener { setLearningMode(ExplorerMode.LEARN) }
        binding.quizTab.setOnClickListener { setLearningMode(ExplorerMode.QUIZ) }
        binding.notesTab.setOnClickListener { setLearningMode(ExplorerMode.NOTES) }
        binding.infoPanel.setOnClickListener { showPartBottomSheet() }
        updateLearningTabs()
    }

    private fun setLearningMode(mode: ExplorerMode) {
        if (currentMode == mode && mode == ExplorerMode.EXPLORE) return
        if (isFullScreen) exitFullScreen()
        currentMode = mode
        updateLearningTabs()

        val isExplore = mode == ExplorerMode.EXPLORE
        tabletTwoPaneContainer?.visibility = if (isExplore) View.VISIBLE else View.GONE
        binding.viewerContainer.visibility = if (isExplore) View.VISIBLE else View.GONE
        binding.interactionHint.visibility = if (isExplore) View.VISIBLE else View.GONE
        binding.modelSelectorHeader.visibility = if (isExplore) View.VISIBLE else View.GONE
        binding.modelDiscoveryPanel.visibility =
            View.GONE
        binding.modelOverviewHeader.visibility = if (isExplore) View.VISIBLE else View.GONE
        binding.modelOverviewPanel.visibility =
            if (isExplore && modelOverviewExpanded) View.VISIBLE else View.GONE
        val hasParts = MODEL_CATALOG[selectedModelIndex].parts.isNotEmpty()
        binding.partsHeader.visibility =
            if (isExplore && hasParts) View.VISIBLE else View.GONE
        binding.partsList.visibility =
            if (isExplore && hasParts && partsExpanded) View.VISIBLE else View.GONE
        binding.infoPanel.visibility =
            if (isExplore && hasParts && partsExpanded) View.VISIBLE else View.GONE
        binding.workflowPanel.visibility = if (isExplore) View.GONE else View.VISIBLE

        if (isExplore) {
            updateExploreAvailability()
            binding.modelWebView.post {
                if (_binding != null) runViewerCommand("refreshHotspots()")
            }
        } else {
            renderActiveWorkflow()
        }
        binding.contentScroll.post { binding.contentScroll.smoothScrollTo(0, 0) }
        binding.root.announceForAccessibility("${mode.title} mode")
    }

    private fun updateLearningTabs() {
        val tabs = listOf(
            binding.exploreTab to ExplorerMode.EXPLORE,
            binding.learnTab to ExplorerMode.LEARN,
            binding.quizTab to ExplorerMode.QUIZ,
            binding.notesTab to ExplorerMode.NOTES
        )
        tabs.forEach { (tab, mode) ->
            val selected = currentMode == mode
            tab.background =
                if (selected) requireContext().getDrawable(R.drawable.bg_filter_chip_selected)
                else null
            tab.setTextColor(if (selected) Color.WHITE else inactiveTextColor)
            tab.typeface = if (selected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            tab.isSelected = selected
        }
    }

    private fun renderActiveWorkflow() {
        if (_binding == null || currentMode == ExplorerMode.EXPLORE) return
        binding.workflowPanel.removeAllViews()
        binding.modelAvailability.text = "${totalXp()} XP"
        binding.modelAvailability.setTextColor(selectedTextColor)
        when (currentMode) {
            ExplorerMode.LEARN -> renderLearnMode()
            ExplorerMode.QUIZ -> renderQuizMode()
            ExplorerMode.NOTES -> renderNotesMode()
            ExplorerMode.EXPLORE -> Unit
        }
    }

    private fun renderLearnMode() {
        val model = MODEL_CATALOG[selectedModelIndex]
        val lessonComplete = preferences().getBoolean(lessonKey(selectedModelIndex), false)
        val introduction = createSurfacePanel()
        introduction.addView(createWorkflowText("GUIDED LESSON", 12f, readyTextColor, true))
        introduction.addView(createWorkflowText(model.title, 26f, Color.WHITE, true, 6))
        introduction.addView(
            createGlossaryWorkflowText(overviewForLevel(model), 16f, bodyTextColor, 10)
        )
        introduction.addView(
            createWorkflowText(
                "Learning goal: identify ${model.parts.size} structures and explain what each one does.",
                14f,
                selectedTextColor,
                true,
                14
            )
        )
        introduction.addView(
            createWorkflowText("DID YOU KNOW?", 11f, readyTextColor, true, 18)
        )
        introduction.addView(createWorkflowText(modelFact(model), 14f, bodyTextColor, false, 5))

        val lessonActions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 16.dp, 0, 0)
        }
        lessonActions.addView(
            createWorkflowAction(
                if (lessonComplete) "Completed" else "Mark complete",
                primary = !lessonComplete
            ) {
                preferences().edit().putBoolean(lessonKey(selectedModelIndex), true).apply()
                if (!lessonComplete) addXp(25)
                renderActiveWorkflow()
                Toast.makeText(requireContext(), "Lesson progress saved", Toast.LENGTH_SHORT).show()
            }
        )
        lessonActions.addView(
            createWorkflowAction("Open 3D", primary = false) {
                setLearningMode(ExplorerMode.EXPLORE)
                selectPart(selectedPartIndex, updateViewer = true)
            }
        )
        introduction.addView(lessonActions)
        binding.workflowPanel.addView(introduction)

        binding.workflowPanel.addView(
            createWorkflowText(
                "Anatomy guide",
                20f,
                Color.WHITE,
                true,
                22
            )
        )
        model.parts.forEachIndexed { index, part ->
            val partPanel = createSurfacePanel(topMargin = 8)
            partPanel.isClickable = true
            partPanel.isFocusable = true
            val description = partDescriptionForLevel(part)
            partPanel.contentDescription =
                "${part.title}. $description. Open in 3D."
            partPanel.addView(
                createWorkflowText(
                    "${index + 1}. ${part.title}",
                    17f,
                    Color.WHITE,
                    true
                )
            )
            partPanel.addView(createGlossaryWorkflowText(description, 14f, bodyTextColor, 7))
            partPanel.addView(createWorkflowText("VIEW IN 3D", 11f, readyTextColor, true, 11))
            partPanel.setOnClickListener {
                selectPart(index, updateViewer = false)
                setLearningMode(ExplorerMode.EXPLORE)
                selectPart(index, updateViewer = true)
            }
            binding.workflowPanel.addView(partPanel)
        }
    }

    private fun renderQuizMode() {
        ensureQuizSession()
        val model = MODEL_CATALOG[selectedModelIndex]
        if (quizQuestions.isEmpty()) {
            val panel = createSurfacePanel()
            panel.addView(createWorkflowText("QUIZ PREPARATION", 12f, readyTextColor, true))
            panel.addView(createWorkflowText(model.title, 24f, Color.WHITE, true, 6))
            panel.addView(
                createWorkflowText(
                    "Download this model to unlock its labelled parts and identification quiz.",
                    15f,
                    bodyTextColor,
                    false,
                    10
                )
            )
            panel.addView(
                createWorkflowAction("Open model library", primary = true, topMargin = 14) {
                    setLearningMode(ExplorerMode.EXPLORE)
                    showModelLibrary()
                }.apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        48.dp
                    ).apply { topMargin = 14.dp }
                }
            )
            binding.workflowPanel.addView(panel)
            return
        }
        if (quizQuestionIndex >= quizQuestions.size) {
            renderQuizResults(model)
            return
        }

        val question = quizQuestions[quizQuestionIndex]
        val header = createSurfacePanel()
        header.addView(
            createWorkflowText(
                "QUESTION ${quizQuestionIndex + 1} OF ${quizQuestions.size}",
                12f,
                readyTextColor,
                true
            )
        )
        val progress = ProgressBar(
            requireContext(),
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                8.dp
            ).apply { topMargin = 10.dp }
            max = quizQuestions.size
            setProgress(quizQuestionIndex + 1, !reducedMotion)
            progressTintList = android.content.res.ColorStateList.valueOf(selectedTextColor)
            progressBackgroundTintList =
                android.content.res.ColorStateList.valueOf(Color.parseColor("#243853"))
        }
        header.addView(progress)
        header.addView(
            createWorkflowText(
                "Score $quizScore  |  Best ${quizBestScore(selectedModelIndex)}/${quizQuestions.size}",
                13f,
                inactiveTextColor,
                false,
                10
            )
        )
        header.addView(createWorkflowText(question.prompt, 21f, Color.WHITE, true, 18))
        binding.workflowPanel.addView(header)

        question.choices.forEachIndexed { answerIndex, answer ->
            val selected = quizSelectedAnswer == answerIndex
            val correct = answerIndex == question.correctIndex
            val answered = quizSelectedAnswer != null
            val answerView = createSurfacePanel(topMargin = 8).apply {
                minimumHeight = 58.dp
                gravity = Gravity.CENTER_VERTICAL
                isClickable = !answered
                isFocusable = !answered
                contentDescription = "Answer ${answerIndex + 1}: $answer"
                background = requireContext().getDrawable(
                    when {
                        answered && correct -> R.drawable.bg_filter_chip_selected
                        selected -> R.drawable.bg_part_row_selected
                        else -> R.drawable.bg_surface_panel
                    }
                )
            }
            answerView.addView(
                createWorkflowText(
                    "${answerIndex + 1}   $answer",
                    16f,
                    if (answered && correct) Color.WHITE else bodyTextColor,
                    selected || (answered && correct)
                )
            )
            answerView.setOnClickListener {
                answerQuizQuestion(answerIndex)
            }
            binding.workflowPanel.addView(answerView)
        }

        quizSelectedAnswer?.let { selectedAnswer ->
            val correct = selectedAnswer == question.correctIndex
            val feedback = createSurfacePanel(topMargin = 12)
            feedback.addView(
                createWorkflowText(
                    if (correct) "Correct" else "Not quite",
                    18f,
                    if (correct) readyTextColor else Color.parseColor("#FFB2B9"),
                    true
                )
            )
            feedback.addView(
                createWorkflowText(
                    question.explanation,
                    14f,
                    bodyTextColor,
                    false,
                    7
                )
            )
            val feedbackActions = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 14.dp, 0, 0)
            }
            feedbackActions.addView(
                createWorkflowAction("View part", primary = false) {
                    selectPart(question.partIndex, updateViewer = false)
                    setLearningMode(ExplorerMode.EXPLORE)
                    selectPart(question.partIndex, updateViewer = true)
                }
            )
            feedbackActions.addView(
                createWorkflowAction(
                    if (quizQuestionIndex == quizQuestions.lastIndex) "Results" else "Next",
                    primary = true
                ) {
                    quizQuestionIndex += 1
                    quizSelectedAnswer = null
                    renderActiveWorkflow()
                    binding.contentScroll.post { binding.contentScroll.smoothScrollTo(0, 0) }
                }
            )
            feedback.addView(feedbackActions)
            binding.workflowPanel.addView(feedback)
        }
    }

    private fun renderQuizResults(model: BiologyModel) {
        val total = quizQuestions.size.coerceAtLeast(1)
        val percentage = (quizScore * 100f / total).roundToInt()
        val previousBest = quizBestScore(selectedModelIndex)
        if (quizScore > previousBest) {
            preferences().edit().putInt(quizBestKey(selectedModelIndex), quizScore).apply()
        }
        val result = createSurfacePanel()
        result.gravity = Gravity.CENTER_HORIZONTAL
        result.addView(createWorkflowText("QUIZ COMPLETE", 12f, readyTextColor, true))
        result.addView(createWorkflowText("$percentage%", 42f, Color.WHITE, true, 12))
        result.addView(
            createWorkflowText(
                "$quizScore of $total correct for ${model.title}",
                16f,
                bodyTextColor,
                false,
                5
            )
        )
        result.addView(
            createWorkflowText(
                when {
                    percentage >= 80 -> "Excellent work. You can identify the key structures."
                    percentage >= 60 -> "Good progress. Review the highlighted parts and try again."
                    else -> "Review the anatomy guide, then take another pass."
                },
                14f,
                inactiveTextColor,
                false,
                12
            )
        )
        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16.dp, 0, 0)
        }
        actions.addView(
            createWorkflowAction("Review", primary = false) {
                setLearningMode(ExplorerMode.LEARN)
            }
        )
        actions.addView(
            createWorkflowAction("Try again", primary = true) {
                startQuizSession()
                renderActiveWorkflow()
            }
        )
        result.addView(actions)
        binding.workflowPanel.addView(result)
        binding.root.announceForAccessibility("Quiz complete. Score $quizScore out of $total")
    }

    private fun answerQuizQuestion(answerIndex: Int) {
        if (quizSelectedAnswer != null) return
        val question = quizQuestions.getOrNull(quizQuestionIndex) ?: return
        quizSelectedAnswer = answerIndex
        val correct = answerIndex == question.correctIndex
        if (correct) {
            quizScore += 1
            if (quizAwardedQuestions.add(quizQuestionIndex)) addXp(10)
        }
        binding.root.performHapticFeedback(
            if (correct) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.REJECT
        )
        renderActiveWorkflow()
        binding.root.announceForAccessibility(if (correct) "Correct" else "Incorrect")
    }

    private fun ensureQuizSession() {
        if (quizModelIndex != selectedModelIndex || quizQuestions.isEmpty()) {
            startQuizSession()
        }
    }

    private fun startQuizSession() {
        val model = MODEL_CATALOG[selectedModelIndex]
        quizModelIndex = selectedModelIndex
        quizQuestionIndex = 0
        quizScore = 0
        quizSelectedAnswer = null
        quizAwardedQuestions.clear()
        if (model.parts.isEmpty()) {
            quizQuestions = emptyList()
            return
        }
        val firstPart = selectedPartIndex.coerceIn(model.parts.indices)
        val partOrder = model.parts.indices.toMutableList().apply {
            remove(firstPart)
            add(0, firstPart)
        }
        quizQuestions = partOrder.map { partIndex ->
            val part = model.parts[partIndex]
            val choices = model.parts
                .map(AnatomyPart::title)
                .shuffled(Random(model.fileName.hashCode() + partIndex))
                .let { shuffled ->
                    val distractors = shuffled.filterNot { it == part.title }.take(3)
                    (distractors + part.title).shuffled(
                        Random(model.fileName.hashCode() * 31 + partIndex)
                    )
                }
            QuizQuestion(
                prompt = "Which structure matches this function?\n\n${part.description}",
                choices = choices,
                correctIndex = choices.indexOf(part.title),
                explanation = "${part.title}: ${part.description}",
                partIndex = partIndex
            )
        }.take(MAX_QUIZ_QUESTIONS)
    }

    private fun renderNotesMode() {
        val model = MODEL_CATALOG[selectedModelIndex]
        val notesKey = notesKey(selectedModelIndex)
        val panel = createSurfacePanel()
        panel.addView(createWorkflowText("STUDY NOTES", 12f, readyTextColor, true))
        panel.addView(createWorkflowText(model.title, 24f, Color.WHITE, true, 6))
        panel.addView(
            createWorkflowText(
                "Notes are saved automatically on this device.",
                13f,
                inactiveTextColor,
                false,
                5
            )
        )
        val savedStatus = createWorkflowText("Saved", 11f, readyTextColor, true, 7)
        val editor = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                180.dp
            ).apply { topMargin = 12.dp }
            background = requireContext().getDrawable(R.drawable.bg_search_field)
            gravity = Gravity.TOP or Gravity.START
            hint = "Write observations, definitions, or revision prompts..."
            setHintTextColor(Color.parseColor("#71849F"))
            setTextColor(Color.WHITE)
            textSize = 15f
            setPadding(14.dp, 13.dp, 14.dp, 13.dp)
            setText(preferences().getString(notesKey, "").orEmpty())
            contentDescription = "Study notes for ${model.title}"
            doAfterTextChanged { value ->
                preferences().edit().putString(notesKey, value?.toString().orEmpty()).apply()
                savedStatus.text = "Saved just now"
            }
        }
        panel.addView(editor)
        panel.addView(savedStatus)

        val noteActions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10.dp, 0, 0)
        }
        noteActions.addView(
            createWorkflowAction("Share", primary = true) {
                shareNotes(model.title, editor.text.toString())
            }
        )
        noteActions.addView(
            createWorkflowAction("Clear", primary = false) {
                confirmClearNotes(notesKey)
            }
        )
        panel.addView(noteActions)
        binding.workflowPanel.addView(panel)

        val savedModelsPanel = createSurfacePanel(topMargin = 12)
        savedModelsPanel.addView(createWorkflowText("SAVED MODELS", 12f, readyTextColor, true))
        if (bookmarkedModels.isEmpty()) {
            savedModelsPanel.addView(
                createWorkflowText(
                    "Bookmark models from their briefing to build a study collection.",
                    14f,
                    inactiveTextColor,
                    false,
                    7
                )
            )
        } else {
            bookmarkedModels.sorted().forEach { modelIndex ->
                val savedModel = MODEL_CATALOG.getOrNull(modelIndex) ?: return@forEach
                savedModelsPanel.addView(
                    createWorkflowAction(savedModel.title, primary = modelIndex == selectedModelIndex, topMargin = 8) {
                        selectModel(modelIndex)
                        setLearningMode(ExplorerMode.EXPLORE)
                    }.apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            46.dp
                        ).apply { topMargin = 8.dp }
                    }
                )
            }
        }
        binding.workflowPanel.addView(savedModelsPanel)

        val currentPart = model.parts.getOrNull(selectedPartIndex)
        if (currentPart == null) {
            val unavailablePanel = createSurfacePanel(topMargin = 12)
            unavailablePanel.addView(
                createWorkflowText("PART NOTES", 12f, selectedTextColor, true)
            )
            unavailablePanel.addView(
                createWorkflowText(
                    "Download this model to add bookmarks and notes for individual structures.",
                    14f,
                    inactiveTextColor,
                    false,
                    7
                )
            )
            binding.workflowPanel.addView(unavailablePanel)
            return
        }
        val currentKey = bookmarkKey(selectedModelIndex, selectedPartIndex)
        val bookmarked = currentKey in bookmarkedParts
        val bookmarkPanel = createSurfacePanel(topMargin = 12)
        bookmarkPanel.addView(createWorkflowText("BOOKMARK CURRENT PART", 12f, selectedTextColor, true))
        bookmarkPanel.addView(createWorkflowText(currentPart.title, 19f, Color.WHITE, true, 6))
        bookmarkPanel.addView(
            createGlossaryWorkflowText(
                partDescriptionForLevel(currentPart),
                14f,
                bodyTextColor,
                6
            )
        )
        bookmarkPanel.addView(
            createWorkflowAction(
                if (bookmarked) "Remove bookmark" else "Add bookmark",
                primary = !bookmarked,
                topMargin = 14
            ) {
                toggleBookmark(selectedModelIndex, selectedPartIndex)
                renderActiveWorkflow()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    48.dp
                ).apply { topMargin = 14.dp }
            }
        )
        binding.workflowPanel.addView(bookmarkPanel)

        binding.workflowPanel.addView(
            createWorkflowText("Saved parts", 20f, Color.WHITE, true, 22)
        )
        val savedParts = model.parts.indices.filter {
            bookmarkKey(selectedModelIndex, it) in bookmarkedParts
        }
        if (savedParts.isEmpty()) {
            binding.workflowPanel.addView(
                createWorkflowText(
                    "No bookmarked parts yet. Save structures you want to revisit.",
                    14f,
                    inactiveTextColor,
                    false,
                    8
                )
            )
        } else {
            savedParts.forEach { partIndex ->
                val part = model.parts[partIndex]
                val row = createSurfacePanel(topMargin = 8).apply {
                    isClickable = true
                    isFocusable = true
                    contentDescription = "Open bookmarked part ${part.title} in 3D"
                }
                row.addView(
                    createWorkflowText(
                        "${partIndex + 1}. ${part.title}",
                        16f,
                        Color.WHITE,
                        true
                    )
                )
                row.addView(createWorkflowText("OPEN IN 3D", 11f, readyTextColor, true, 6))
                row.setOnClickListener {
                    selectPart(partIndex, updateViewer = false)
                    setLearningMode(ExplorerMode.EXPLORE)
                    selectPart(partIndex, updateViewer = true)
                }
                binding.workflowPanel.addView(row)
            }
        }
    }

    private fun createSurfacePanel(topMargin: Int = 0): LinearLayout =
        LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { this.topMargin = topMargin.dp }
            orientation = LinearLayout.VERTICAL
            background = requireContext().getDrawable(R.drawable.bg_surface_panel)
            setPadding(18.dp, 17.dp, 18.dp, 17.dp)
        }

    private fun createWorkflowText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean,
        topMargin: Int = 0
    ): TextView =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { this.topMargin = topMargin.dp }
            text = value
            textSize = size
            setTextColor(color)
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setLineSpacing(2.dp.toFloat(), 1f)
        }

    private fun createGlossaryWorkflowText(
        value: String,
        size: Float,
        color: Int,
        topMargin: Int = 0
    ): TextView =
        createWorkflowText(value, size, color, false, topMargin).also {
            setGlossaryText(it, value)
        }

    private fun createWorkflowAction(
        title: String,
        primary: Boolean,
        topMargin: Int = 0,
        action: () -> Unit
    ): TextView =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                48.dp,
                1f
            ).apply {
                marginEnd = 8.dp
                this.topMargin = topMargin.dp
            }
            minWidth = 96.dp
            gravity = Gravity.CENTER
            text = title
            setTextColor(if (primary) Color.WHITE else bodyTextColor)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            background = requireContext().getDrawable(
                if (primary) R.drawable.bg_filter_chip_selected else R.drawable.bg_filter_chip
            )
            isClickable = true
            isFocusable = true
            contentDescription = title
            setOnClickListener { action() }
        }

    private fun restoreBookmarks() {
        bookmarkedParts.clear()
        bookmarkedParts += preferences()
            .getStringSet(PREFERENCE_BOOKMARKS, emptySet())
            .orEmpty()
        bookmarkedModels.clear()
        bookmarkedModels += preferences()
            .getStringSet(PREFERENCE_MODEL_BOOKMARKS, emptySet())
            .orEmpty()
            .mapNotNull { token ->
                token.toIntOrNull()
                    ?.takeIf { it in MODEL_CATALOG.indices }
                    ?: MODEL_CATALOG.indexOfFirst { it.id == token }.takeIf { it >= 0 }
            }
    }

    private fun toggleModelBookmark(modelIndex: Int) {
        val added = if (modelIndex in bookmarkedModels) {
            bookmarkedModels.remove(modelIndex)
            false
        } else {
            bookmarkedModels.add(modelIndex)
            true
        }
        preferences().edit()
            .putStringSet(
                PREFERENCE_MODEL_BOOKMARKS,
                bookmarkedModels.mapNotNull { MODEL_CATALOG.getOrNull(it)?.id }.toSet()
            )
            .apply()
        updateModelBriefing()
        renderModelCatalog()
        binding.root.announceForAccessibility(
            if (added) "Model bookmarked" else "Model bookmark removed"
        )
    }

    private fun toggleBookmark(modelIndex: Int, partIndex: Int) {
        val key = bookmarkKey(modelIndex, partIndex)
        val added = if (key in bookmarkedParts) {
            bookmarkedParts.remove(key)
            false
        } else {
            bookmarkedParts.add(key)
            true
        }
        preferences().edit()
            .putStringSet(PREFERENCE_BOOKMARKS, bookmarkedParts.toSet())
            .apply()
        binding.root.announceForAccessibility(
            if (added) "Bookmark added" else "Bookmark removed"
        )
    }

    private fun shareNotes(modelTitle: String, notes: String) {
        if (notes.isBlank()) {
            Toast.makeText(requireContext(), "Write a note before sharing", Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "$modelTitle study notes")
                    putExtra(Intent.EXTRA_TEXT, "$modelTitle\n\n$notes")
                },
                "Share study notes"
            )
        )
    }

    private fun confirmClearNotes(key: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear study notes?")
            .setMessage("This removes the notes saved for the selected model.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear") { _, _ ->
                preferences().edit().remove(key).apply()
                renderActiveWorkflow()
            }
            .show()
    }

    private fun preferences() =
        requireContext().getSharedPreferences(PREFERENCES_NAME, 0)

    private fun totalXp(): Int = preferences().getInt(PREFERENCE_TOTAL_XP, 0)

    private fun addXp(amount: Int) {
        preferences().edit().putInt(PREFERENCE_TOTAL_XP, totalXp() + amount).apply()
    }

    private fun quizBestScore(modelIndex: Int): Int =
        preferences().getInt(quizBestKey(modelIndex), 0)

    private fun bookmarkKey(modelIndex: Int, partIndex: Int) = "$modelIndex:$partIndex"

    private fun lessonKey(modelIndex: Int) = "lesson_complete_$modelIndex"

    private fun quizBestKey(modelIndex: Int) = "quiz_best_$modelIndex"

    private fun notesKey(modelIndex: Int) = "notes_${MODEL_CATALOG[modelIndex].id}"

    private fun exploredPartsKey(modelId: String) = "explored_parts_$modelId"

    private fun modelOverview(model: BiologyModel): String =
        MODEL_OVERVIEWS[model.fileName]
            ?: "${model.title} is explored here as a three-dimensional biological structure."

    private fun overviewForLevel(model: BiologyModel): String =
        when (readingLevel) {
            ReadingLevel.BEGINNER ->
                BEGINNER_OVERVIEWS[model.fileName]
                    ?: "${model.title} is a biological structure with parts that work together."
            ReadingLevel.STUDENT -> modelOverview(model)
            ReadingLevel.ADVANCED ->
                "${modelOverview(model)} ${ADVANCED_CONTEXT[model.fileName] ?: modelFact(model)}"
        }

    private fun partDescriptionForLevel(part: AnatomyPart): String =
        when (readingLevel) {
            ReadingLevel.BEGINNER -> part.description
                .replace("selective", "controlling")
                .replace("osmotic pressure", "changes in water pressure")
                .replace("synthesize", "make")
                .replace("catalyzes", "helps carry out")
                .replace("hydrophobic", "water-repelling")
                .replace("lumen", "inner space")
            ReadingLevel.STUDENT -> part.description
            ReadingLevel.ADVANCED -> {
                val context = ADVANCED_PART_CONTEXT.entries.firstOrNull {
                    part.title.contains(it.key, ignoreCase = true)
                }?.value ?: "Its molecular organization links structure directly to biological function."
                "${part.description} $context"
            }
        }

    private fun modelMetadata(model: BiologyModel): ModelMetadata =
        MODEL_METADATA[model.fileName] ?: ModelMetadata(
            commonName = model.title,
            scientificName = model.title,
            pronunciation = model.title,
            sourceTitle = "OpenStax Biology 2e",
            sourceUrl = OPENSTAX_EUKARYOTIC_CELLS
        )

    private fun modelFact(model: BiologyModel): String =
        MODEL_FACTS[model.fileName]
            ?: "Its shape and organization are closely linked to its biological function."

    private fun updateExploreAvailability() {
        val available = isModelAvailable(MODEL_CATALOG[selectedModelIndex].fileName)
        binding.modelAvailability.text = if (available) "READY" else "NEEDS FILE"
        binding.modelAvailability.setTextColor(
            if (available) readyTextColor else inactiveTextColor
        )
    }

    private fun configureModelDiscovery() {
        restoreRecentModels()

        FILTERS.forEach { filter ->
            binding.filterStrip.addView(createFilterChip(filter))
        }
        binding.modelSearch.doAfterTextChanged {
            modelQuery = it?.toString().orEmpty().trim()
            if (catalogReady) renderModelCatalog()
        }

        if (BuildConfig.BIOLOGY_CATALOG_URL.isBlank()) {
            completeCatalogSetup()
        } else {
            showCatalogLoadingState()
        }
    }

    private fun loadRemoteCatalog() {
        remoteCatalogRepository.load { result ->
            if (_binding == null) return@load
            if (result.models.isEmpty()) {
                if (BuildConfig.BIOLOGY_CATALOG_URL.isNotBlank() && !result.warning.isNullOrBlank()) {
                    Toast.makeText(requireContext(), result.warning, Toast.LENGTH_LONG).show()
                }
                completeCatalogSetup()
                return@load
            }
            val activeId = MODEL_CATALOG.getOrNull(selectedModelIndex)?.id
            MODEL_CATALOG.clear()
            MODEL_CATALOG.addAll(result.models.map(modelRepository::hydrateInstalledModel))
            restoreRecentModels()
            restoreBookmarks()
            selectedModelIndex = activeId
                ?.let { id -> MODEL_CATALOG.indexOfFirst { it.id == id } }
                ?.takeIf { it >= 0 }
                ?: recentModelIndices.firstOrNull()
                ?: 0
            completeCatalogSetup(selectedModelIndex)
            if (!hasAutoOpenedModelLibrary) {
                hasAutoOpenedModelLibrary = true
                binding.root.post { if (_binding != null) showModelLibrary() }
            }
            result.warning?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showCatalogLoadingState() {
        catalogReady = false
        binding.modelTitle.text = "Loading biology catalogue"
        binding.modelAvailability.text = "SYNCING"
        binding.modelAvailability.setTextColor(inactiveTextColor)
        binding.modelStrip.removeAllViews()
        binding.recentStrip.removeAllViews()
        binding.recentModelsLabel.visibility = View.GONE
        binding.recentStripContainer.visibility = View.GONE
        binding.partsList.removeAllViews()
        binding.featureTitle.text = "Preparing models"
        binding.featureDescription.text = "Fetching titles, thumbnails and availability..."
        binding.viewerStatusOverlay.visibility = View.VISIBLE
        binding.viewerStatusOverlay.bringToFront()
        binding.modelProgress.visibility = View.VISIBLE
        binding.modelLoadProgress.visibility = View.VISIBLE
        binding.modelLoadProgress.isIndeterminate = true
        binding.viewerStatusActions.visibility = View.GONE
        binding.viewerStatusText.text = "Loading biology catalogue..."
    }

    private fun completeCatalogSetup(preferredIndex: Int? = null) {
        if (_binding == null || MODEL_CATALOG.isEmpty()) return
        catalogReady = true
        restoreRecentModels()
        restoreBookmarks()
        val index = preferredIndex
            ?.takeIf { it in MODEL_CATALOG.indices }
            ?: recentModelIndices.firstOrNull()
            ?: 0
        renderModelCatalog()
        selectModel(index)
    }

    private fun restoreRecentModels() {
        recentModelIndices.clear()
        val saved = preferences().getString(PREFERENCE_RECENT_MODELS, "").orEmpty()
        recentModelIndices += saved.split(",")
            .map(String::trim)
            .mapNotNull { token ->
                token.toIntOrNull()
                    ?.takeIf { it in MODEL_CATALOG.indices }
                    ?: MODEL_CATALOG.indexOfFirst { it.id == token }.takeIf { it >= 0 }
            }
            .distinct()
            .take(MAX_RECENT_MODELS)
    }

    private fun showModelLibrary() {
        val downloadedIds = modelRepository.downloadedIds()
        val libraryModels = MODEL_CATALOG.mapIndexed { index, model ->
            val exploredCount = preferences()
                .getStringSet(exploredPartsKey(model.id), emptySet())
                .orEmpty()
                .size
            model.copy(
                isDownloaded = model.id in downloadedIds,
                isFavourite = index in bookmarkedModels,
                learningProgress =
                    if (model.parts.isEmpty()) 0f
                    else exploredCount.toFloat() / model.parts.size,
                viewCount =
                    recentModelIndices.indexOf(index)
                        .takeIf { it >= 0 }
                        ?.let { MAX_RECENT_MODELS - it }
                        ?: 0
            )
        }
        ModelLibraryBottomSheet(
            context = requireContext(),
            models = libraryModels,
            repository = modelRepository,
            recentModelIds = {
                recentModelIndices.map { MODEL_CATALOG[it].id }
            },
            isAvailable = { isModelAvailable(it.fileName) },
            isFavourite = { model ->
                MODEL_CATALOG.indexOfFirst { it.id == model.id } in bookmarkedModels
            },
            thumbnailFile = { model ->
                remoteCatalogRepository.thumbnailFile(model)
                    ?: if (model.thumbnailUrl.isNullOrBlank()) {
                        MODEL_CATALOG.indexOfFirst { it.id == model.id }
                            .takeIf { it >= 0 }
                            ?.let(::thumbnailFile)
                    } else {
                        null
                    }
            },
            requestThumbnail = remoteCatalogRepository::loadThumbnail,
            onSelected = { model ->
                MODEL_CATALOG.indexOfFirst { it.id == model.id }
                    .takeIf { it >= 0 }
                    ?.let { index ->
                        MODEL_CATALOG[index] = modelRepository.hydrateInstalledModel(model)
                        selectModel(index)
                    }
            },
            onFavourite = { model ->
                MODEL_CATALOG.indexOfFirst { it.id == model.id }
                    .takeIf { it >= 0 }
                    ?.let(::toggleModelBookmark)
            },
            onUnavailable = { model, reason ->
                showUnavailableModel(model, reason)
            }
        ).show()
    }

    private fun showUnavailableModel(model: BiologyModel, reason: String) {
        val sizeLine = (model.packageSizeBytes ?: model.fileSizeBytes)?.let {
            "\n\nApproximate download size: ${com.indianservers.AIbiology.ui.BiologyModelAdapter.formatBytes(it)}"
        }.orEmpty()
        AlertDialog.Builder(requireContext())
            .setTitle("${model.title} is not installed")
            .setMessage("$reason$sizeLine")
            .setNegativeButton("Close", null)
            .setPositiveButton("Retry") { _, _ -> showModelLibrary() }
            .show()
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
            if (isTelevision) TvFocus.apply(this)
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
            text = when {
                index in bookmarkedModels && available -> "SAVED | READY"
                index in bookmarkedModels -> "SAVED | NEEDS FILE"
                available -> "READY"
                else -> "NEEDS FILE"
            }
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
        val hasParts = model.parts.isNotEmpty()
        binding.partsHeader.visibility =
            if (currentMode == ExplorerMode.EXPLORE && hasParts) View.VISIBLE else View.GONE
        binding.partsList.visibility =
            if (currentMode == ExplorerMode.EXPLORE && hasParts && partsExpanded) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.infoPanel.visibility = binding.partsList.visibility
        if (isTelevision) {
            tabletPartsColumn?.visibility =
                if (hasParts && currentMode == ExplorerMode.EXPLORE) View.VISIBLE else View.GONE
            tabletViewerColumn?.layoutParams =
                (tabletViewerColumn?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    weight = if (hasParts) 0.72f else 1f
                }
        }
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
            textSize = if (largerLabels) 18f else 15f
            maxLines = 1
            contentDescription =
                "${part.title}. ${partDescriptionForLevel(part)}. Part ${index + 1}."
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
            if (isTelevision) TvFocus.apply(this)
        }
    }

    private fun selectModel(index: Int) {
        selectedModelIndex = index
        val model = MODEL_CATALOG[index]

        addRecentModel(index)
        updateModelSelectionStyles()
        configurePartList(model)
        selectPart(0, updateViewer = false)
        updateModelBriefing()
        binding.fullScreenTitle.text = model.title
        updateViewerCapabilities(model)
        loadModel(model)
        if (currentMode != ExplorerMode.EXPLORE) renderActiveWorkflow()
    }

    private fun addRecentModel(index: Int) {
        recentModelIndices.remove(index)
        recentModelIndices.add(0, index)
        while (recentModelIndices.size > MAX_RECENT_MODELS) {
            recentModelIndices.removeAt(recentModelIndices.lastIndex)
        }
        requireContext()
            .getSharedPreferences(PREFERENCES_NAME, 0)
            .edit()
            .putString(
                PREFERENCE_RECENT_MODELS,
                recentModelIndices.mapNotNull { MODEL_CATALOG.getOrNull(it)?.id }.joinToString(",")
            )
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
        if (updateViewer) {
            val key = exploredPartsKey(model.id)
            val explored = preferences().getStringSet(key, emptySet()).orEmpty().toMutableSet()
            if (explored.add(part.id)) {
                preferences().edit().putStringSet(key, explored).apply()
            }
        }
        binding.featureNumber.text = (index + 1).toString()
        binding.featureTitle.text = part.title
        setGlossaryText(binding.featureDescription, partDescriptionForLevel(part))
        binding.infoPanel.contentDescription =
            "${part.title}. ${partDescriptionForLevel(part)}. Tap for details."

        if (updateViewer) {
            binding.modelWebView.evaluateJavascript("window.focusPart($index)", null)
            if (screenReaderMode) {
                binding.root.announceForAccessibility(
                    "${part.title}. ${partDescriptionForLevel(part)}"
                )
            }
        }
    }

    private fun showPartsPanel() {
        val model = MODEL_CATALOG[selectedModelIndex]
        if (model.parts.isEmpty()) return
        val dialog = BottomSheetDialog(requireContext())
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp, 14.dp, 16.dp, 20.dp)
            background = requireContext().getDrawable(R.drawable.bg_surface_panel)
        }
        content.addView(createWorkflowText("PARTS", 12f, readyTextColor, true))
        content.addView(createWorkflowText(model.title, 23f, Color.WHITE, true, 4))
        val search = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                48.dp
            ).apply { topMargin = 12.dp }
            background = requireContext().getDrawable(R.drawable.bg_search_field)
            hint = "Search model parts"
            setHintTextColor(Color.parseColor("#7F91AB"))
            setTextColor(Color.WHITE)
            setSingleLine()
            setPadding(14.dp, 0, 14.dp, 0)
        }
        content.addView(search)
        val rows = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroller = android.widget.ScrollView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                460.dp
            ).apply { topMargin = 10.dp }
            addView(rows)
        }
        content.addView(scroller)

        fun render(query: String) {
            rows.removeAllViews()
            val matches = model.parts.withIndex().filter {
                query.isBlank() ||
                    it.value.title.contains(query, ignoreCase = true) ||
                    it.value.scientificName.orEmpty().contains(query, ignoreCase = true)
            }
            val groups = matches.groupBy { it.value.parentPartId ?: "OTHER" }
            groups.forEach { (groupId, entries) ->
                rows.addView(
                    createWorkflowText(
                        when (groupId) {
                            "CELL_ENVELOPE" -> "Cell Envelope"
                            "CYTOPLASM" -> "Cytoplasm"
                            "EXTERNAL_STRUCTURES" -> "External Structures"
                            else -> if (groups.size == 1) "Structures" else "Other Structures"
                        },
                        13f,
                        selectedTextColor,
                        true,
                        10
                    )
                )
                entries.forEach { indexed ->
                    rows.addView(
                        createSheetAction(indexed.value.title, true) {
                            selectPart(indexed.index, updateViewer = true)
                            dialog.dismiss()
                            showPartBottomSheet()
                        }.apply {
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                50.dp
                            ).apply { topMargin = 6.dp }
                            gravity = Gravity.CENTER_VERTICAL
                            setPadding(14.dp, 0, 14.dp, 0)
                            contentDescription =
                                "${indexed.value.title}. Focus this structure in 3D."
                        }
                    )
                }
            }
            if (matches.isEmpty()) {
                rows.addView(
                    createWorkflowText("No matching parts.", 15f, inactiveTextColor, false, 18)
                )
            }
        }
        search.doAfterTextChanged { render(it?.toString().orEmpty()) }
        render("")
        dialog.setContentView(content)
        dialog.behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            isDraggable = true
            skipCollapsed = false
            peekHeight = 360.dp
        }
        dialog.show()
    }

    private fun showAskAiPanel() {
        val model = MODEL_CATALOG[selectedModelIndex]
        val part = model.parts.getOrNull(selectedPartIndex)
        val dialog = BottomSheetDialog(requireContext())
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18.dp, 16.dp, 18.dp, 24.dp)
            background = requireContext().getDrawable(R.drawable.bg_surface_panel)
        }
        content.addView(createWorkflowText("ASK AI", 12f, readyTextColor, true))
        content.addView(
            createWorkflowText(
                part?.let { "${model.title}  |  ${it.title}" } ?: model.title,
                21f,
                Color.WHITE,
                true,
                5
            )
        )
        val response = createGlossaryWorkflowText(
            "Ask about structure, function, or how this model compares with another organism.",
            14f,
            bodyTextColor,
            10
        )
        val input = EditText(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                92.dp
            ).apply { topMargin = 14.dp }
            background = requireContext().getDrawable(R.drawable.bg_search_field)
            hint = "What would you like to understand?"
            setHintTextColor(Color.parseColor("#7F91AB"))
            setTextColor(Color.WHITE)
            gravity = Gravity.TOP or Gravity.START
            setPadding(14.dp, 12.dp, 14.dp, 12.dp)
        }
        content.addView(input)
        content.addView(
            createWorkflowAction("Ask", primary = true, topMargin = 10) {
                val question = input.text.toString().trim()
                if (question.isBlank()) {
                    input.error = "Enter a biology question"
                } else {
                    val answer = when {
                        question.contains("function", true) && part != null ->
                            "${part.title}: ${partDescriptionForLevel(part)}"
                        question.contains("compare", true) ->
                            "${model.title} is a ${model.categoryId.lowercase()} model. Use Compare in the model briefing to place its structures beside another model."
                        part != null ->
                            "${part.title} is one of ${model.parts.size} identified structures in ${model.title}. ${partDescriptionForLevel(part)}"
                        else -> overviewForLevel(model)
                    }
                    setGlossaryText(
                        response,
                        "$answer\n\nThis on-device answer uses the reviewed lesson content. A remote AI provider is not configured."
                    )
                    response.announceForAccessibility("Answer ready. $answer")
                }
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    48.dp
                ).apply { topMargin = 10.dp }
            }
        )
        content.addView(response)
        dialog.setContentView(content)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    private fun showPartBottomSheet() {
        if (_binding == null) return
        partBottomSheet?.dismiss()

        val model = MODEL_CATALOG[selectedModelIndex]
        if (model.parts.isEmpty()) return
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
                val description = partDescriptionForLevel(part)
                setTextColor(Color.parseColor("#C7D4E7"))
                textSize = if (largerLabels) 19f else 16f
                setLineSpacing(3.dp.toFloat(), 1f)
                setGlossaryText(this, description)
            }
        )
        content.addView(
            createSheetAction("Hear pronunciation", true) {
                speakTerm(part.title)
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    46.dp
                ).apply { topMargin = 12.dp }
            }
        )

        val showAllSwitch = SwitchCompat(requireContext()).apply {
            text = "Show all visible labels"
            setTextColor(Color.WHITE)
            textSize = 14f
            isChecked = showAllLabels
            isEnabled = !identifyMode
            alpha = if (identifyMode) 0.45f else 1f
            setPadding(0, 14.dp, 0, 4.dp)
            setOnCheckedChangeListener { _, enabled ->
                showAllLabels = enabled
                preferences().edit()
                    .putBoolean(PREFERENCE_SHOW_ALL_LABELS, enabled)
                    .apply()
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
                preferences().edit()
                    .putBoolean(PREFERENCE_IDENTIFY_MODE, enabled)
                    .apply()
                showAllSwitch.isEnabled = !enabled
                showAllSwitch.alpha = if (enabled) 0.45f else 1f
                runViewerCommand("setIdentifyMode($enabled)")
                if (!enabled) {
                    runViewerCommand("selectPart($selectedPartIndex)")
                }
            }
        }
        content.addView(showAllSwitch)
        content.addView(identifySwitch)

        val learningActions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        learningActions.addView(
            createSheetAction("Ask AI", true) {
                dialog.dismiss()
                showAskAiPanel()
            }
        )
        learningActions.addView(
            createSheetAction("Quiz me", true) {
                dialog.dismiss()
                setLearningMode(ExplorerMode.QUIZ)
                startQuizSession()
                renderActiveWorkflow()
            }
        )
        learningActions.addView(
            createSheetAction("Add note", true) {
                addPartNote(model, part)
            }
        )
        content.addView(learningActions)

        val actions = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8.dp, 0, 0)
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
            createSheetAction(
                if (bookmarkKey(selectedModelIndex, selectedPartIndex) in bookmarkedParts) {
                    "Saved"
                } else {
                    "Bookmark"
                },
                true
            ) {
                toggleBookmark(selectedModelIndex, selectedPartIndex)
                showPartBottomSheet()
            }
        )
        content.addView(actions)

        content.addView(
            createSheetAction("Done", true) {
                dialog.dismiss()
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    46.dp
                ).apply { topMargin = 8.dp }
            }
        )

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

    private fun addPartNote(model: BiologyModel, part: ModelPart) {
        val input = EditText(requireContext()).apply {
            hint = "What do you want to remember?"
            minLines = 3
            maxLines = 6
            setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Note about ${part.title}")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val note = input.text.toString().trim()
                if (note.isBlank()) return@setPositiveButton
                val key = notesKey(selectedModelIndex)
                val existing = preferences().getString(key, "").orEmpty().trim()
                val timestamp = SimpleDateFormat(
                    "dd MMM yyyy, h:mm a",
                    Locale.getDefault()
                ).format(Date())
                val entry = "[$timestamp] ${part.title} (${model.id}/${part.id})\n$note"
                preferences().edit()
                    .putString(key, listOf(existing, entry).filter(String::isNotBlank).joinToString("\n\n"))
                    .apply()
                Toast.makeText(requireContext(), "Part note saved", Toast.LENGTH_SHORT).show()
                binding.root.announceForAccessibility("Note saved for ${part.title}")
            }
            .show()
    }

    private fun restoreIdentificationSettings() {
        identifyMode = preferences().getBoolean(PREFERENCE_IDENTIFY_MODE, false)
        showAllLabels = preferences().getBoolean(PREFERENCE_SHOW_ALL_LABELS, true)
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
        currentModelAvailable = available
        binding.modelAvailability.text = if (available) "READY" else "NEEDS FILE"
        binding.modelAvailability.setTextColor(
            if (available) readyTextColor else inactiveTextColor
        )

        val savedSurfaceAnchors = savedSurfaceAnchors(selectedModelIndex, model.parts.size)
        val partsJson = JSONArray().apply {
            model.parts.forEachIndexed { index, part ->
                put(
                    JSONObject()
                        .put("title", part.title)
                        .put("position", part.position)
                        .put("normal", part.normal)
                        .put(
                            "autoAnchor",
                            part.position.trim().replace(Regex("\\s+"), " ") == "0 0 0"
                        )
                        .apply {
                            savedSurfaceAnchors.getOrNull(index)
                                ?.takeIf(String::isNotBlank)
                                ?.let { surface -> put("surface", surface) }
                        }
                )
            }
        }.toString()

        val source =
            if (storedModel != null || bundled) {
                "https://$MODEL_HOST/models/${Uri.encode(model.fileName)}"
            } else {
                null
            }

        binding.viewerStatusOverlay.visibility = View.VISIBLE
        binding.viewerStatusOverlay.bringToFront()
        binding.modelProgress.visibility = if (available) View.VISIBLE else View.GONE
        binding.modelLoadProgress.visibility = if (available) View.VISIBLE else View.GONE
        binding.modelLoadProgress.isIndeterminate = available
        binding.viewerStatusActions.visibility = if (available) View.GONE else View.VISIBLE
        binding.viewerStatusText.text =
            if (available) "Preparing ${model.title}…" else "${model.title} is not available."
        isAutoRotating = !reducedMotion && !screenReaderMode
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
            append("&reducedMotion=${if (reducedMotion) 1 else 0}")
            append("&highContrast=${if (highContrast) 1 else 0}")
            append("&largerLabels=${if (largerLabels) 1 else 0}")
            append("&screenReader=${if (screenReaderMode) 1 else 0}")
        }
        binding.modelWebView.loadUrl(query)
    }

    private fun showViewerError(message: String) {
        binding.viewerStatusOverlay.visibility = View.VISIBLE
        binding.viewerStatusOverlay.bringToFront()
        binding.modelProgress.visibility = View.GONE
        binding.modelLoadProgress.visibility = View.GONE
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

    private fun savedSurfaceAnchors(modelIndex: Int, expectedCount: Int): List<String> {
        val encoded = preferences().getString(surfaceAnchorsKey(modelIndex), null)
            ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            if (array.length() != expectedCount) return emptyList()
            List(array.length()) { index -> array.optString(index, "") }
        }.getOrDefault(emptyList())
    }

    private fun surfaceAnchorsKey(modelIndex: Int) = "surface_anchors_$modelIndex"

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
                    "loaded" -> {
                        binding.viewerStatusText.text = "Ready"
                        binding.modelProgress.visibility = View.GONE
                        binding.modelLoadProgress.visibility = View.GONE
                        binding.viewerStatusOverlay.postDelayed({
                            if (_binding != null) binding.viewerStatusOverlay.visibility = View.GONE
                        }, 450L)
                        binding.root.announceForAccessibility(
                            "${MODEL_CATALOG[selectedModelIndex].title} model loaded"
                        )
                    }
                    "error" -> {
                        val message = detail.ifBlank { "The 3D model could not be opened." }
                        showViewerError(message)
                        binding.root.announceForAccessibility("Model loading failed. $message")
                    }
                }
            }
        }

        @JavascriptInterface
        fun onLoadingStage(stage: String, progress: Int) {
            activity?.runOnUiThread {
                if (_binding == null || !currentModelAvailable) return@runOnUiThread
                binding.viewerStatusOverlay.visibility = View.VISIBLE
                binding.modelProgress.visibility = View.VISIBLE
                binding.modelLoadProgress.visibility = View.VISIBLE
                binding.modelLoadProgress.isIndeterminate = progress !in 1..99
                binding.modelLoadProgress.progress = progress.coerceIn(0, 100)
                binding.viewerStatusActions.visibility = View.GONE
                binding.viewerStatusText.text =
                    if (progress in 1..99) "$stage  $progress%" else stage
            }
        }

        @JavascriptInterface
        fun onCameraState(zoomPercent: Int, orientation: String) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                updateZoomControls(zoomPercent.coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT))
                binding.orientationIndicator.text = orientation
                binding.orientationIndicator.contentDescription =
                    "$orientation orientation. Tap to choose another orientation."
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

        @JavascriptInterface
        fun onSurfaceAnchorsReady(modelIndex: Int, encodedAnchors: String) {
            if (modelIndex !in MODEL_CATALOG.indices || encodedAnchors.isBlank()) return
            val expectedCount = MODEL_CATALOG[modelIndex].parts.size
            val valid = runCatching {
                val anchors = JSONArray(encodedAnchors)
                anchors.length() == expectedCount &&
                    (0 until anchors.length()).any { anchors.optString(it).isNotBlank() }
            }.getOrDefault(false)
            if (!valid) return
            preferences().edit()
                .putString(surfaceAnchorsKey(modelIndex), encodedAnchors)
                .apply()
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
        const val PREFERENCE_BOOKMARKS = "bookmarked_parts"
        const val PREFERENCE_MODEL_BOOKMARKS = "bookmarked_models"
        const val PREFERENCE_TOTAL_XP = "total_xp"
        const val PREFERENCE_IDENTIFY_MODE = "identify_mode"
        const val PREFERENCE_SHOW_ALL_LABELS = "show_all_labels"
        const val PREFERENCE_READING_LEVEL = "reading_level"
        const val PREFERENCE_REDUCED_MOTION = "reduced_motion"
        const val PREFERENCE_HIGH_CONTRAST = "high_contrast"
        const val PREFERENCE_LARGER_LABELS = "larger_labels"
        const val PREFERENCE_SCREEN_READER = "screen_reader_mode"
        const val CONTENT_REVIEWED_DATE = "29 Jul 2026"
        const val OPENSTAX_EUKARYOTIC_CELLS =
            "https://openstax.org/books/biology-2e/pages/4-3-eukaryotic-cells"
        const val OPENSTAX_PROKARYOTIC_CELLS =
            "https://openstax.org/books/biology-2e/pages/4-2-prokaryotic-cells"
        const val MAX_RECENT_MODELS = 5
        const val MAX_QUIZ_QUESTIONS = 5
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
        val bodyTextColor: Int = Color.parseColor("#C7D4E7")
        val FILTERS =
            listOf(FILTER_ALL, FILTER_ORGANELLES, FILTER_PLANTS, FILTER_HUMAN, FILTER_MICROBIOLOGY)
        val ROTATION_SPEEDS = listOf(10, 18, 30)
        val ROTATION_SPEED_LABELS = listOf("0.5×", "1×", "1.5×")
        val CAMERA_PRESETS = listOf(
            CameraPreset("front", "FR", "Front"),
            CameraPreset("right", "RT", "Right"),
            CameraPreset("back", "BK", "Back"),
            CameraPreset("left", "LT", "Left"),
            CameraPreset("top", "TOP", "Top"),
            CameraPreset("bottom", "BTM", "Bottom")
        )
        val STARTER_MODEL_FILES = setOf(
            "Bacteriacell.glb",
            "Neuron.glb",
            "Vacuole.glb"
        )
        val MODEL_OVERVIEWS = mapOf(
            "Bacteriacell.glb" to
                "A bacterial cell is a compact prokaryotic system that carries out every life process without a membrane-bound nucleus.",
            "Cell Membrane.glb" to
                "The cell membrane is a dynamic, selectively permeable boundary that coordinates transport, signaling, and recognition.",
            "Chloroplast.glb" to
                "Chloroplasts convert light energy into chemical energy and provide the internal membranes needed for photosynthesis.",
            "epithelial microvilli.glb" to
                "Microvilli are microscopic surface projections that let epithelial cells absorb materials efficiently.",
            "Lysosome.glb" to
                "Lysosomes are membrane-bound recycling centers that digest worn components and biological macromolecules.",
            "Mitochondrion.glb" to
                "Mitochondria couple nutrient oxidation to ATP production using highly folded internal membranes.",
            "Neuron.glb" to
                "A neuron is a specialized signaling cell built to receive, integrate, and transmit information over distance.",
            "plant cell wall.glb" to
                "The plant cell wall is a layered extracellular structure that provides strength while permitting controlled growth.",
            "PlantCell.glb" to
                "A plant cell combines a cellulose wall, chloroplasts, and a large vacuole with the shared machinery of eukaryotic cells.",
            "Ribosomes.glb" to
                "Ribosomes are molecular machines that translate messenger RNA into ordered chains of amino acids.",
            "Rough Endoplasmic Reticulum.glb" to
                "The rough endoplasmic reticulum synthesizes and begins processing proteins destined for membranes or secretion.",
            "Smooth Endoplasmic Reticulum.glb" to
                "The smooth endoplasmic reticulum supports lipid synthesis, detoxification, and controlled calcium storage.",
            "Vacuole.glb" to
                "A vacuole is a membrane-bound storage compartment that is especially important for plant-cell water balance.",
            "WhiteBloodCell.glb" to
                "White blood cells are mobile immune cells that detect threats, coordinate defenses, and remove damaged material."
        )
        val MODEL_FACTS = mapOf(
            "Bacteriacell.glb" to
                "Bacterial ribosomes can begin translating an RNA molecule while it is still being transcribed.",
            "Cell Membrane.glb" to
                "Membrane lipids and many proteins move laterally, which is why the structure is called a fluid mosaic.",
            "Chloroplast.glb" to
                "Chloroplasts contain their own DNA, reflecting their evolutionary origin from ancient bacteria.",
            "epithelial microvilli.glb" to
                "A dense brush border can multiply a cell's absorptive surface area many times.",
            "Lysosome.glb" to
                "The lysosomal interior is kept acidic so its digestive enzymes work efficiently.",
            "Mitochondrion.glb" to
                "The number and shape of mitochondria change with a cell's energy demand.",
            "Neuron.glb" to
                "Some human axons extend more than a meter while remaining part of a single cell.",
            "plant cell wall.glb" to
                "Cellulose fibers have high tensile strength, while the surrounding matrix controls flexibility.",
            "PlantCell.glb" to
                "Water entering the central vacuole creates turgor pressure that helps support non-woody tissues.",
            "Ribosomes.glb" to
                "A ribosome reads messenger RNA three bases at a time, matching each codon to an amino acid.",
            "Rough Endoplasmic Reticulum.glb" to
                "Ribosomes attach to the rough ER only while making proteins that carry an ER targeting signal.",
            "Smooth Endoplasmic Reticulum.glb" to
                "In muscle cells, specialized smooth ER releases calcium to trigger contraction.",
            "Vacuole.glb" to
                "A mature plant cell's central vacuole can occupy most of the cell's internal volume.",
            "WhiteBloodCell.glb" to
                "White blood cells can squeeze between vessel-wall cells to reach infected tissue."
        )
        val BEGINNER_OVERVIEWS = mapOf(
            "Bacteriacell.glb" to "A bacterial cell is a tiny living cell without a nucleus. Its parts help it find energy, grow, and reproduce.",
            "Cell Membrane.glb" to "The cell membrane is a flexible boundary that controls what enters and leaves a cell.",
            "Chloroplast.glb" to "A chloroplast is the part of a plant cell that captures sunlight to help make food.",
            "epithelial microvilli.glb" to "Microvilli are tiny finger-like folds that help a cell absorb more material.",
            "Lysosome.glb" to "A lysosome breaks down waste and reuses useful materials inside a cell.",
            "Mitochondrion.glb" to "A mitochondrion releases usable energy from food for the cell.",
            "Neuron.glb" to "A neuron is a nerve cell that carries messages through the body.",
            "plant cell wall.glb" to "A plant cell wall is a strong outer layer that supports and protects the cell.",
            "PlantCell.glb" to "A plant cell uses sunlight, stores water, and has a firm wall for support.",
            "Ribosomes.glb" to "Ribosomes are tiny structures that build the proteins a cell needs.",
            "Rough Endoplasmic Reticulum.glb" to "The rough ER helps make, fold, and move proteins.",
            "Smooth Endoplasmic Reticulum.glb" to "The smooth ER makes fats, stores calcium, and helps remove harmful chemicals.",
            "Vacuole.glb" to "A vacuole stores water and other materials, especially in plant cells.",
            "WhiteBloodCell.glb" to "A white blood cell helps protect the body from infection and damaged cells."
        )
        val ADVANCED_CONTEXT = mapOf(
            "Bacteriacell.glb" to "Its lack of internal membrane compartments couples transcription, translation, transport, and energy metabolism within one cytoplasmic system.",
            "Cell Membrane.glb" to "Its asymmetric lipid bilayer and embedded proteins create electrochemical gradients and regulated signaling platforms.",
            "Chloroplast.glb" to "Photophosphorylation occurs across thylakoid membranes, while carbon fixation proceeds in the stroma.",
            "epithelial microvilli.glb" to "Actin-bundle organization and membrane transporters couple morphology to vectorial absorption.",
            "Lysosome.glb" to "V-type ATPases acidify the lumen, enabling hydrolases and autophagic recycling pathways.",
            "Mitochondrion.glb" to "Chemiosmotic coupling across the inner membrane drives oxidative phosphorylation and ATP synthesis.",
            "Neuron.glb" to "Membrane potentials, axonal conduction, and synaptic transmission coordinate rapid information processing.",
            "plant cell wall.glb" to "Cellulose microfibrils, hemicellulose, and pectin form a mechanically responsive extracellular matrix.",
            "PlantCell.glb" to "Compartmentalization integrates photosynthesis, respiration, turgor regulation, and cell-wall mechanics.",
            "Ribosomes.glb" to "Ribosomal RNA performs key structural and catalytic roles during codon-directed translation.",
            "Rough Endoplasmic Reticulum.glb" to "Co-translational translocation connects signal recognition, folding, glycosylation, and quality control.",
            "Smooth Endoplasmic Reticulum.glb" to "Its membrane enzymes coordinate lipid metabolism, xenobiotic processing, and calcium homeostasis.",
            "Vacuole.glb" to "Tonoplast transport establishes ion gradients that regulate turgor, pH, storage, and degradation.",
            "WhiteBloodCell.glb" to "Leukocyte subtype, receptor repertoire, and effector mechanisms determine innate or adaptive immune function."
        )
        val ADVANCED_PART_CONTEXT = mapOf(
            "membrane" to "Transport proteins and lipid composition regulate permeability and signaling.",
            "nucleus" to "Nuclear pores coordinate selective traffic between nucleoplasm and cytoplasm.",
            "nucleoid" to "DNA topology and nucleoid-associated proteins compact and regulate the chromosome.",
            "ribosome" to "Ribosomal RNA contributes directly to decoding and peptide-bond formation.",
            "mitochond" to "Electron transport establishes the proton-motive force used by ATP synthase.",
            "chloroplast" to "Thylakoid electron transport couples light capture to ATP and NADPH production.",
            "wall" to "Polymer composition determines tensile strength, porosity, and mechanical response.",
            "axon" to "Voltage-gated ion channels support regenerative action-potential propagation.",
            "dendrite" to "Branched geometry supports synaptic integration across many inputs.",
            "vacuole" to "Solute transport across the tonoplast controls osmotic potential and turgor.",
            "lumen" to "Its ionic and enzymatic conditions are maintained separately from the cytosol."
        )
        val GLOSSARY = linkedMapOf(
            "prokaryotic" to "Describes a cell whose DNA is not enclosed by a membrane-bound nucleus.",
            "eukaryotic" to "Describes a cell with a membrane-bound nucleus and internal organelles.",
            "organelle" to "A specialized structure inside a cell that performs particular functions.",
            "ATP" to "Adenosine triphosphate, the main immediately usable energy carrier in cells.",
            "DNA" to "Deoxyribonucleic acid, the molecule that stores hereditary information.",
            "RNA" to "Ribonucleic acid, a family of molecules involved in gene expression and protein synthesis.",
            "cytoplasm" to "The material inside the cell membrane, excluding the nucleus in eukaryotic cells.",
            "enzyme" to "A biological catalyst that speeds a chemical reaction without being consumed.",
            "photosynthesis" to "The process that uses light energy to build energy-rich organic molecules.",
            "protein" to "A folded chain of amino acids that performs structural, catalytic, or signaling roles.",
            "lipid" to "A water-insoluble or partly water-insoluble molecule used in membranes and energy storage.",
            "osmosis" to "The net movement of water across a selectively permeable membrane.",
            "turgor" to "Pressure of cell contents against a plant cell wall, helping support the tissue."
        )
        val MODEL_METADATA = mapOf(
            "Bacteriacell.glb" to ModelMetadata("Bacterial cell", "Prokaryotic cell (Domain Bacteria)", "bacterial cell", "OpenStax Biology 2e: Prokaryotic Cells", OPENSTAX_PROKARYOTIC_CELLS),
            "Cell Membrane.glb" to ModelMetadata("Cell membrane", "Plasma membrane", "plasma membrane", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Chloroplast.glb" to ModelMetadata("Chloroplast", "Chloroplast", "chloroplast", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "epithelial microvilli.glb" to ModelMetadata("Microvilli", "Epithelial microvilli", "epithelial microvilli", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Lysosome.glb" to ModelMetadata("Lysosome", "Lysosome", "lysosome", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Mitochondrion.glb" to ModelMetadata("Mitochondrion", "Mitochondrion", "mitochondrion", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Neuron.glb" to ModelMetadata("Nerve cell", "Neuron", "neuron", "OpenStax Biology 2e", "https://openstax.org/books/biology-2e/pages/35-2-how-neurons-communicate"),
            "plant cell wall.glb" to ModelMetadata("Plant cell wall", "Primary and secondary cell wall", "plant cell wall", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "PlantCell.glb" to ModelMetadata("Plant cell", "Plant eukaryotic cell", "plant cell", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Ribosomes.glb" to ModelMetadata("Ribosome", "Ribosome", "ribosome", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Rough Endoplasmic Reticulum.glb" to ModelMetadata("Rough ER", "Rough endoplasmic reticulum", "rough endoplasmic reticulum", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Smooth Endoplasmic Reticulum.glb" to ModelMetadata("Smooth ER", "Smooth endoplasmic reticulum", "smooth endoplasmic reticulum", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "Vacuole.glb" to ModelMetadata("Vacuole", "Central vacuole", "vacuole", "OpenStax Biology 2e: Eukaryotic Cells", OPENSTAX_EUKARYOTIC_CELLS),
            "WhiteBloodCell.glb" to ModelMetadata("White blood cell", "Leukocyte", "leukocyte", "OpenStax Biology 2e", "https://openstax.org/books/biology-2e/pages/42-1-innate-immune-response")
        )

        fun part(
            title: String,
            description: String,
            position: String,
            normal: String = "0 0 1"
        ): AnatomyPart {
            val semanticId = title.uppercase()
                .replace(Regex("[^A-Z0-9]+"), "_")
                .trim('_')
            return ModelPart(
                id = semanticId,
                nodeNames = listOf(title, semanticId, "BIO_$semanticId"),
                title = title,
                shortDescription = description,
                detailedDescription = description,
                parentPartId = when (semanticId) {
                    "CAPSULE", "CELL_WALL", "CELL_MEMBRANE" -> "CELL_ENVELOPE"
                    "RIBOSOMES", "NUCLEOID" -> "CYTOPLASM"
                    "FLAGELLUM", "PILI" -> "EXTERNAL_STRUCTURES"
                    else -> null
                },
                position = position,
                normal = normal,
                hitNodeNames = listOf("HIT_$semanticId")
            )
        }

        fun model(
            fileName: String,
            title: String,
            shortTitle: String,
            badge: String,
            parts: List<AnatomyPart>
        ): BiologyModel {
            val metadata = MODEL_METADATA[fileName]
            val category = when (fileName) {
                "Bacteriacell.glb" -> BiologyCategories.MICROBIOLOGY
                "PlantCell.glb" -> BiologyCategories.CELLS
                "Chloroplast.glb",
                "plant cell wall.glb",
                "Vacuole.glb" -> BiologyCategories.PLANTS
                "Neuron.glb",
                "WhiteBloodCell.glb",
                "epithelial microvilli.glb" -> BiologyCategories.HUMAN_BODY
                else -> BiologyCategories.ORGANELLES
            }
            return BiologyModel(
                fileName = fileName,
                title = title,
                shortTitle = shortTitle,
                badge = badge,
                parts = parts,
                alternativeNames = listOf(shortTitle, metadata?.commonName.orEmpty())
                    .filter(String::isNotBlank)
                    .distinct(),
                scientificName = metadata?.scientificName,
                description = MODEL_OVERVIEWS[fileName].orEmpty(),
                categoryId = category,
                tags = (title.split(" ") + category + "3D").distinct(),
                system = when (category) {
                    BiologyCategories.HUMAN_BODY -> "Human body"
                    BiologyCategories.PLANTS -> "Plant biology"
                    BiologyCategories.MICROBIOLOGY -> "Microbiology"
                    else -> "Cell biology"
                },
                supportsAr = false,
                supportsPartSelection = parts.isNotEmpty()
            )
        }

        val BUILT_IN_MODEL_CATALOG = listOf(
            model(
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
            model(
                "Cell Membrane.glb", "Cell Membrane", "Membrane", "CM",
                listOf(
                    part("Phospholipid heads", "Water-attracting heads face the fluid inside and outside the cell.", "-0.55 0.10 0.78"),
                    part("Fatty acid tails", "Water-repelling tails form the membrane's hydrophobic inner core.", "-0.15 -0.02 0.83"),
                    part("Channel protein", "Channel proteins provide selective pathways through the membrane.", "0.27 0.05 0.79"),
                    part("Glycoprotein", "Carbohydrate-tagged proteins support recognition and cell signaling.", "0.62 0.13 0.62")
                )
            ),
            model(
                "Chloroplast.glb", "Chloroplast", "Chloroplast", "CH",
                listOf(
                    part("Outer membrane", "The outer membrane forms the chloroplast's external boundary.", "-0.64 0.18 0.55"),
                    part("Stroma", "The stroma contains enzymes used to build sugars from carbon dioxide.", "-0.22 0.04 0.65"),
                    part("Granum", "A granum is a stack of thylakoids where light-dependent reactions occur.", "0.20 0.12 0.64"),
                    part("Stroma lamella", "Lamellae connect grana and organize the thylakoid membrane system.", "0.57 -0.08 0.50")
                )
            ),
            model(
                "epithelial microvilli.glb", "Epithelial Microvilli", "Microvilli", "MV",
                listOf(
                    part("Microvilli", "Finger-like projections increase the surface area available for absorption.", "-0.45 0.30 0.78"),
                    part("Actin core", "Parallel actin filaments support each microvillus.", "-0.05 0.12 0.88"),
                    part("Terminal web", "The terminal web anchors microvilli to the cell cortex.", "0.34 -0.08 0.80"),
                    part("Cell surface", "The apical membrane faces the lumen or external environment.", "0.62 -0.25 0.55")
                )
            ),
            model(
                "Lysosome.glb", "Lysosome", "Lysosome", "LY",
                listOf(
                    part("Lysosomal membrane", "A single membrane isolates powerful digestive enzymes from the cytoplasm.", "-0.57 0.22 0.72"),
                    part("Acidic lumen", "Proton pumps maintain an acidic interior where lysosomal enzymes work best.", "-0.10 0.04 0.92"),
                    part("Hydrolytic enzymes", "Enzymes break proteins, lipids, nucleic acids, and carbohydrates into reusable units.", "0.42 -0.15 0.78")
                )
            ),
            model(
                "Mitochondrion.glb", "Mitochondrion", "Mitochondrion", "MT",
                listOf(
                    part("Outer membrane", "The smooth outer membrane encloses the organelle.", "-0.67 0.08 0.38"),
                    part("Intermembrane space", "Protons accumulate here to power ATP synthase.", "-0.35 -0.02 0.48"),
                    part("Cristae", "Folds of the inner membrane provide a large surface for electron transport.", "0.08 0.06 0.51"),
                    part("Matrix", "The matrix contains enzymes for the citric acid cycle and mitochondrial DNA.", "0.48 -0.04 0.38")
                )
            ),
            model(
                "Neuron.glb", "Neuron", "Neuron", "NE",
                listOf(
                    part("Dendrites", "Dendrites receive signals from other neurons.", "-0.73 0.05 0.56"),
                    part("Cell body", "The soma contains the nucleus and maintains the neuron.", "-0.38 0.02 0.77"),
                    part("Axon", "The axon conducts electrical impulses away from the cell body.", "0.18 0.01 0.80"),
                    part("Axon terminals", "Terminals release neurotransmitters to communicate with target cells.", "0.76 -0.03 0.48")
                )
            ),
            model(
                "plant cell wall.glb", "Plant Cell Wall", "Cell Wall", "CW",
                listOf(
                    part("Primary wall", "A flexible cellulose-rich wall permits growth while providing support.", "-0.59 0.20 0.62"),
                    part("Middle lamella", "A pectin-rich layer glues adjacent plant cells together.", "-0.18 0.04 0.70"),
                    part("Secondary wall", "Some cells deposit a stronger secondary wall inside the primary wall.", "0.26 -0.04 0.67"),
                    part("Plasmodesmata", "Microscopic channels connect the cytoplasm of neighboring plant cells.", "0.62 -0.18 0.48")
                )
            ),
            model(
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
            model(
                "Ribosomes.glb", "Ribosomes", "Ribosomes", "RB",
                listOf(
                    part("Large subunit", "The large subunit catalyzes peptide-bond formation.", "-0.40 0.18 0.78"),
                    part("Small subunit", "The small subunit binds and reads messenger RNA.", "0.08 -0.05 0.90"),
                    part("mRNA channel", "Messenger RNA passes between the subunits during translation.", "0.48 -0.16 0.72")
                )
            ),
            model(
                "Rough Endoplasmic Reticulum.glb", "Rough Endoplasmic Reticulum", "Rough ER", "ER",
                listOf(
                    part("Cisternae", "Flattened membrane sacs create spaces for protein folding and processing.", "-0.55 0.20 0.66"),
                    part("Bound ribosomes", "Attached ribosomes synthesize proteins entering the secretory pathway.", "-0.12 0.10 0.82"),
                    part("ER lumen", "The lumen supports protein folding and quality control.", "0.28 -0.04 0.79"),
                    part("Transport vesicle", "Vesicles carry processed proteins from the ER toward the Golgi apparatus.", "0.63 -0.18 0.56")
                )
            ),
            model(
                "Smooth Endoplasmic Reticulum.glb", "Smooth Endoplasmic Reticulum", "Smooth ER", "SE",
                listOf(
                    part("Tubule network", "Interconnected membrane tubules provide a large reaction surface.", "-0.50 0.32 0.72"),
                    part("ER membrane", "Membrane enzymes synthesize lipids and help detoxify compounds.", "-0.05 0.08 0.92"),
                    part("ER lumen", "The interior stores calcium ions and transports newly made molecules.", "0.48 -0.25 0.68")
                )
            ),
            model(
                "Vacuole.glb", "Vacuole", "Vacuole", "VA",
                listOf(
                    part("Tonoplast", "The tonoplast is the vacuole's selective surrounding membrane.", "-0.52 0.20 0.56"),
                    part("Cell sap", "The internal solution stores water, ions, pigments, and other solutes.", "-0.05 0.02 0.65"),
                    part("Transport proteins", "Tonoplast proteins control movement between the vacuole and cytoplasm.", "0.50 -0.18 0.53")
                )
            ),
            model(
                "WhiteBloodCell.glb", "White Blood Cell", "White Blood Cell", "WB",
                listOf(
                    part("Cell membrane", "A flexible membrane lets the cell change shape and move through tissues.", "-0.57 0.18 0.76"),
                    part("Nucleus", "Nuclear shape helps distinguish different types of white blood cell.", "-0.12 0.05 0.93"),
                    part("Cytoplasm", "The cytoplasm contains machinery used for movement and immune responses.", "0.30 -0.08 0.88"),
                    part("Granules", "In granulocytes, enzyme-filled granules help destroy pathogens.", "0.62 -0.22 0.64")
                )
            )
        ).filter { it.fileName in STARTER_MODEL_FILES }
    }
}

private data class ModelMetadata(
    val commonName: String,
    val scientificName: String,
    val pronunciation: String,
    val sourceTitle: String,
    val sourceUrl: String
)

private data class QuizQuestion(
    val prompt: String,
    val choices: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val partIndex: Int
)

private enum class ExplorerMode(val title: String) {
    EXPLORE("Explore"),
    LEARN("Learn"),
    QUIZ("Quiz"),
    NOTES("Notes")
}

private enum class ReadingLevel {
    BEGINNER,
    STUDENT,
    ADVANCED
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
