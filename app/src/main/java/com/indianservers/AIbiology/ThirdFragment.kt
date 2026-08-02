package com.indianservers.AIbiology

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
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
import com.indianservers.AIbiology.data.MicroscopyAnnotation
import com.indianservers.AIbiology.data.MicroscopyRepository
import com.indianservers.AIbiology.data.NetworkAvailability
import com.indianservers.AIbiology.data.MicroscopySlide
import com.indianservers.AIbiology.databinding.FragmentThirdBinding
import com.indianservers.AIbiology.ui.DeviceProfile
import com.indianservers.AIbiology.ui.MicroscopySlideAdapter
import com.indianservers.AIbiology.ui.TvFocus
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.hypot

class ThirdFragment : Fragment() {
    private var _binding: FragmentThirdBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: MicroscopyRepository
    private lateinit var adapter: MicroscopySlideAdapter
    private var slides = emptyList<MicroscopySlide>()
    private var selectedSlide: MicroscopySlide? = null
    private var viewerInitialized = false
    private var labelsVisible = true
    private var isTelevision = false
    private var challengeQuestions = emptyList<MicroscopyAnnotation>()
    private var challengeIndex = 0
    private var challengeScore = 0
    private var challengeAnswered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentThirdBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = MicroscopyRepository(
            requireContext(),
            BuildConfig.BIOLOGY_MICROSCOPY_CATALOG_URL
        )
        isTelevision = DeviceProfile.isTelevision(requireContext())
        configureInsets()
        configureLayout()
        configureViewer()
        configureActions()
        configureBackHandling()
        loadCatalogue(refreshRemote = false)
    }

    private fun configureInsets() {
        val baseHeight = 82.dp
        val start = if (isTelevision) 36.dp else binding.microscopyTopBar.paddingStart
        val end = if (isTelevision) 36.dp else binding.microscopyTopBar.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(binding.microscopyTopBar) { topBar, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            topBar.setPadding(start + system.left, system.top, end + system.right, 0)
            topBar.layoutParams = topBar.layoutParams.apply { height = baseHeight + system.top }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun configureLayout() {
        val useTwoPane =
            isTelevision || resources.configuration.smallestScreenWidthDp >= 600
        binding.microscopyWorkspace.orientation =
            if (useTwoPane) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        binding.slideRail.layoutParams = LinearLayout.LayoutParams(
            if (useTwoPane) (if (isTelevision) 320 else 250).dp
            else ViewGroup.LayoutParams.MATCH_PARENT,
            if (useTwoPane) ViewGroup.LayoutParams.MATCH_PARENT else 154.dp
        )
        binding.microscopyViewerPane.layoutParams = LinearLayout.LayoutParams(
            if (useTwoPane) 0 else ViewGroup.LayoutParams.MATCH_PARENT,
            if (useTwoPane) ViewGroup.LayoutParams.MATCH_PARENT else 0,
            1f
        )
        val orientation =
            if (useTwoPane) LinearLayoutManager.VERTICAL else LinearLayoutManager.HORIZONTAL
        binding.slideList.layoutManager = LinearLayoutManager(requireContext(), orientation, false)
        adapter = MicroscopySlideAdapter(
            repository::thumbnailFile,
            repository::loadThumbnail,
            ::selectSlide
        )
        binding.slideList.adapter = adapter
        if (isTelevision) {
            binding.microscopyTitle.textSize = 24f
            listOf(
                binding.microscopyBack,
                binding.microscopyRefresh,
                binding.zoomIn,
                binding.zoomOut,
                binding.resetSlide,
                binding.toggleLabels,
                binding.startChallenge,
                binding.endChallenge,
                binding.nextChallenge
            ).forEach { TvFocus.apply(it, 1.03f) }
            TvFocus.apply(binding.microscopyWebView, 1.01f)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureViewer() {
        binding.microscopyWebView.apply {
            setBackgroundColor(Color.TRANSPARENT)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = false
            @Suppress("DEPRECATION")
            settings.allowFileAccessFromFileURLs = true
            @Suppress("DEPRECATION")
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowContentAccess = false
            settings.mediaPlaybackRequiresUserGesture = true
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            addJavascriptInterface(MicroscopyBridge(), "MicroscopyBridge")
            loadUrl("file:///android_asset/microscopy_viewer.html")
            setOnKeyListener { _, keyCode, event ->
                if (!isTelevision || event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> pan(-0.12, 0.0)
                    KeyEvent.KEYCODE_DPAD_RIGHT -> pan(0.12, 0.0)
                    KeyEvent.KEYCODE_DPAD_UP -> pan(0.0, -0.12)
                    KeyEvent.KEYCODE_DPAD_DOWN -> pan(0.0, 0.12)
                    else -> return@setOnKeyListener false
                }
                true
            }
        }
    }

    private fun configureActions() {
        binding.microscopyBack.setOnClickListener { navigateBack() }
        binding.microscopyRefresh.setOnClickListener { loadCatalogue(refreshRemote = true) }
        binding.zoomIn.setOnClickListener { evaluate("zoomBy(1.45)") }
        binding.zoomOut.setOnClickListener { evaluate("zoomBy(0.69)") }
        binding.resetSlide.setOnClickListener { evaluate("resetView()") }
        binding.toggleLabels.setOnClickListener {
            labelsVisible = !labelsVisible
            updateLabelButton()
            evaluate("setLabelsVisible($labelsVisible)")
        }
        binding.startChallenge.setOnClickListener { startChallenge() }
        binding.endChallenge.setOnClickListener { finishChallenge(save = challengeIndex > 0) }
        binding.nextChallenge.setOnClickListener { showNextQuestion() }
        binding.selectedSlideTitle.setOnClickListener {
            selectedSlide?.let(::showSlideOverview)
        }
    }

    private fun configureBackHandling() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    when {
                        binding.microscopyWebView.hasFocus() && isTelevision -> {
                            binding.microscopyWebView.clearFocus()
                            binding.zoomIn.requestFocus()
                        }
                        binding.challengePanel.visibility == View.VISIBLE ->
                            finishChallenge(save = challengeIndex > 0)
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
        showEmpty("Loading virtual lab", "Checking the tissue slide catalogue.", loading = true)
        val onLoaded: (com.indianservers.AIbiology.data.MicroscopyCatalogResult) -> Unit =
            onLoaded@{ result ->
            if (_binding == null) return@onLoaded
            slides = result.slides
            adapter.submitList(slides) {
                if (selectedSlide == null && slides.isNotEmpty()) selectSlide(slides.first())
            }
            binding.slideCount.text = "${slides.size} ${if (slides.size == 1) "slide" else "slides"}"
            if (slides.isEmpty()) {
                val configured = BuildConfig.BIOLOGY_MICROSCOPY_CATALOG_URL.isNotBlank()
                showEmpty(
                    if (configured) "No tissue slides available" else "Microscopy is ready",
                    if (configured) {
                        "Check the catalogue connection and refresh."
                    } else {
                        "Add the microscopy catalogue URL to publish tissue slides without bundling media."
                    },
                    loading = false
                )
            }
            result.warning?.takeIf { slides.isNotEmpty() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
            }
        if (refreshRemote && !NetworkAvailability.isInternetAvailable(requireContext())) {
            Toast.makeText(
                requireContext(),
                NetworkAvailability.CATALOG_WARNING,
                Toast.LENGTH_LONG
            ).show()
            repository.loadCached(onLoaded)
        } else if (refreshRemote) {
            repository.refresh(onLoaded)
        } else {
            repository.loadCached(onLoaded)
        }
    }

    private fun selectSlide(slide: MicroscopySlide) {
        finishChallenge(save = false)
        selectedSlide = slide
        adapter.selectedId = slide.id
        binding.selectedSlideTitle.text = slide.title
        binding.selectedSlideMeta.text = listOfNotNull(
            slide.tissue,
            slide.stain,
            slide.magnification
        ).joinToString(" · ").ifBlank { slide.category }
        binding.slideInfo.visibility = View.VISIBLE
        binding.microscopyControls.visibility = View.VISIBLE
        binding.microscopyActionBar.visibility = View.VISIBLE
        binding.startChallenge.isEnabled = slide.annotations.isNotEmpty()
        binding.startChallenge.alpha = if (slide.annotations.isNotEmpty()) 1f else 0.5f
        updateProgress(slide)
        showEmpty("Preparing ${slide.title}", "Loading the zoomable tissue image.", loading = true)
        if (viewerInitialized) openSlide(slide)
    }

    private fun openSlide(slide: MicroscopySlide) {
        val annotations = JSONArray().apply {
            slide.annotations.forEach { annotation ->
                put(JSONObject().apply {
                    put("id", annotation.id)
                    put("label", annotation.label)
                    put("x", annotation.x)
                    put("y", annotation.y)
                })
            }
        }
        val source = slide.source
        evaluate(
            "openSlide(" +
                "${JSONObject.quote(source.type.name.lowercase())}," +
                "${JSONObject.quote(source.url)}," +
                "${source.width ?: 0},${source.height ?: 0}," +
                "${JSONObject.quote(annotations.toString())})"
        )
    }

    private fun startChallenge() {
        val slide = selectedSlide ?: return
        if (slide.annotations.isEmpty()) return
        challengeQuestions = slide.annotations.shuffled()
        challengeIndex = 0
        challengeScore = 0
        challengeAnswered = false
        binding.microscopyActionBar.visibility = View.GONE
        binding.challengePanel.visibility = View.VISIBLE
        labelsVisible = false
        updateLabelButton()
        evaluate("setLabelsVisible(false)")
        renderQuestion()
    }

    private fun renderQuestion() {
        val question = challengeQuestions.getOrNull(challengeIndex) ?: run {
            finishChallenge(save = true)
            return
        }
        challengeAnswered = false
        binding.challengeEyebrow.text =
            "IDENTIFY ${challengeIndex + 1} OF ${challengeQuestions.size}"
        binding.challengePrompt.text = question.challengePrompt
        binding.challengeFeedback.visibility = View.GONE
        binding.nextChallenge.isEnabled = false
        binding.nextChallenge.alpha = 0.5f
        binding.nextChallenge.text =
            if (challengeIndex == challengeQuestions.lastIndex) "Finish" else "Next"
    }

    private fun handleSlideTap(x: Double, y: Double) {
        if (binding.challengePanel.visibility != View.VISIBLE || challengeAnswered) return
        val target = challengeQuestions.getOrNull(challengeIndex) ?: return
        val distance = hypot(x - target.x, y - target.y)
        val correct = distance <= target.radius
        binding.challengeFeedback.visibility = View.VISIBLE
        if (correct) {
            challengeAnswered = true
            challengeScore += 1
            binding.challengeFeedback.setTextColor(requireContext().getColor(R.color.model_state_ready))
            binding.challengeFeedback.text = listOf(
                "Correct: ${target.label}.",
                target.description
            ).filter(String::isNotBlank).joinToString(" ")
            binding.nextChallenge.isEnabled = true
            binding.nextChallenge.alpha = 1f
        } else {
            binding.challengeFeedback.setTextColor(requireContext().getColor(R.color.model_state_error))
            binding.challengeFeedback.text = "Not quite. Examine the tissue pattern and try again."
        }
    }

    private fun showNextQuestion() {
        if (!challengeAnswered) return
        challengeIndex += 1
        renderQuestion()
    }

    private fun finishChallenge(save: Boolean) {
        if (_binding == null || binding.challengePanel.visibility != View.VISIBLE) return
        val slide = selectedSlide
        if (save && slide != null && challengeQuestions.isNotEmpty()) {
            repository.recordAttempt(slide.id, challengeScore, challengeQuestions.size)
            Toast.makeText(
                requireContext(),
                "Challenge complete: $challengeScore/${challengeQuestions.size}",
                Toast.LENGTH_SHORT
            ).show()
            updateProgress(slide)
        }
        binding.challengePanel.visibility = View.GONE
        binding.microscopyActionBar.visibility =
            if (selectedSlide == null) View.GONE else View.VISIBLE
        labelsVisible = true
        updateLabelButton()
        evaluate("setLabelsVisible(true)")
        challengeQuestions = emptyList()
    }

    private fun updateProgress(slide: MicroscopySlide) {
        val progress = repository.progress(slide.id)
        binding.challengeProgress.text =
            if (progress.attempts == 0) "Not tried"
            else "Best ${progress.bestScore}/${progress.questionCount}"
    }

    private fun updateLabelButton() {
        binding.toggleLabels.text = if (labelsVisible) "Labels on" else "Labels off"
        binding.toggleLabels.setBackgroundResource(
            if (labelsVisible) R.drawable.bg_filter_chip_selected
            else R.drawable.bg_filter_chip
        )
    }

    private fun showSlideOverview(slide: MicroscopySlide) {
        AlertDialog.Builder(requireContext())
            .setTitle(slide.title)
            .setMessage(
                listOfNotNull(
                    slide.summary.takeIf(String::isNotBlank),
                    slide.scientificName?.let { "Scientific name: $it" },
                    slide.species?.let { "Species: $it" },
                    slide.attribution?.let { "Source: $it" },
                    slide.reviewedAt?.let { "Reviewed: $it" }
                ).joinToString("\n\n")
            )
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showEmpty(title: String, message: String, loading: Boolean) {
        binding.viewerEmptyState.visibility = View.VISIBLE
        binding.microscopyLoading.visibility = if (loading) View.VISIBLE else View.GONE
        binding.microscopyEmptyTitle.text = title
        binding.microscopyEmptyMessage.text = message
        if (selectedSlide == null) {
            binding.slideInfo.visibility = View.GONE
            binding.microscopyActionBar.visibility = View.GONE
        }
    }

    private fun pan(x: Double, y: Double) {
        evaluate("panBy($x,$y)")
    }

    private fun evaluate(script: String) {
        _binding?.microscopyWebView?.evaluateJavascript(script, null)
    }

    private inner class MicroscopyBridge {
        @JavascriptInterface
        fun onViewerInitialized() {
            activity?.runOnUiThread {
                viewerInitialized = true
                selectedSlide?.let(::openSlide)
            }
        }

        @JavascriptInterface
        fun onReady() {
            activity?.runOnUiThread {
                _binding?.viewerEmptyState?.visibility = View.GONE
            }
        }

        @JavascriptInterface
        fun onError(message: String) {
            activity?.runOnUiThread {
                if (_binding == null) return@runOnUiThread
                showEmpty(
                    "Slide could not be opened",
                    "$message Check the slide URL and CDN access settings.",
                    loading = false
                )
            }
        }

        @JavascriptInterface
        fun onZoomChanged(zoom: String) = Unit

        @JavascriptInterface
        fun onSlideTap(x: Double, y: Double) {
            activity?.runOnUiThread { handleSlideTap(x, y) }
        }
    }

    override fun onDestroyView() {
        repository.close()
        binding.slideList.adapter = null
        binding.microscopyWebView.apply {
            removeJavascriptInterface("MicroscopyBridge")
            stopLoading()
            loadUrl("about:blank")
            destroy()
        }
        super.onDestroyView()
        _binding = null
    }
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
