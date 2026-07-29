package com.indianservers.biology

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.indianservers.biology.databinding.FragmentFirstBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var selectedModelIndex = 0
    private var selectedPartIndex = 0
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
        configureViewer()
        configureExpanders()
        configureModelStrip()
    }

    override fun onDestroyView() {
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
                    _binding?.modelProgress?.visibility =
                        if (newProgress >= 100) View.GONE else View.VISIBLE
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

    private fun configureExpanders() {
        binding.modelSelectorHeader.setOnClickListener {
            binding.modelStripContainer.visibility =
                toggledVisibility(binding.modelStripContainer.visibility)
            binding.modelSelectorHeader.text =
                if (binding.modelStripContainer.visibility == View.VISIBLE) {
                    "Models  -  tap to collapse"
                } else {
                    "Models  +  tap to expand"
                }
        }

        binding.partsHeader.setOnClickListener {
            val nextVisibility = toggledVisibility(binding.partsList.visibility)
            binding.partsList.visibility = nextVisibility
            binding.infoPanel.visibility = nextVisibility
            binding.partsHeader.text =
                if (nextVisibility == View.VISIBLE) {
                    "Identify parts  -  tap to collapse"
                } else {
                    "Identify parts  +  tap to expand"
                }
        }
    }

    private fun configureModelStrip() {
        binding.modelStrip.removeAllViews()
        MODEL_CATALOG.forEachIndexed { index, model ->
            binding.modelStrip.addView(createModelStripItem(index, model))
        }
        selectModel(0)
    }

    private fun createModelStripItem(index: Int, model: BiologyModel): View {
        val available = isModelAvailable(model.fileName)
        val item = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            alpha = if (available) 1f else 0.58f
            contentDescription =
                if (available) model.title else "${model.title}, download required"
            setPadding(4.dp, 0, 4.dp, 0)
            layoutParams = LinearLayout.LayoutParams(132.dp, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { selectModel(index) }
        }

        val badge = TextView(requireContext()).apply {
            width = 112.dp
            height = 68.dp
            gravity = Gravity.CENTER
            text = model.badge
            setTextColor(Color.WHITE)
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            background = requireContext().getDrawable(
                if (index == selectedModelIndex) {
                    R.drawable.bg_thumbnail_selected
                } else {
                    R.drawable.bg_thumbnail
                }
            )
        }

        val label = TextView(requireContext()).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
            maxLines = 2
            text = model.shortTitle
            setTextColor(if (index == selectedModelIndex) selectedTextColor else inactiveTextColor)
            textSize = 11f
        }

        item.addView(badge)
        item.addView(label)
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
            setOnClickListener { selectPart(index, updateViewer = true) }
        }
    }

    private fun selectModel(index: Int) {
        selectedModelIndex = index
        val model = MODEL_CATALOG[index]

        MODEL_CATALOG.forEachIndexed { childIndex, _ ->
            val item = binding.modelStrip.getChildAt(childIndex) as? LinearLayout
            val badge = item?.getChildAt(0) as? TextView
            val label = item?.getChildAt(1) as? TextView
            badge?.background = requireContext().getDrawable(
                if (childIndex == selectedModelIndex) {
                    R.drawable.bg_thumbnail_selected
                } else {
                    R.drawable.bg_thumbnail
                }
            )
            label?.setTextColor(
                if (childIndex == selectedModelIndex) selectedTextColor else inactiveTextColor
            )
        }

        configurePartList(model)
        selectPart(0, updateViewer = false)
        loadModel(model)
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
            binding.modelWebView.evaluateJavascript("window.selectPart($index)", null)
        }
    }

    private fun loadModel(model: BiologyModel) {
        binding.modelTitle.text = model.title
        val storedModel = findStoredModel(model.fileName)
        val bundled = isBundledModelAvailable(model.fileName)
        val available = storedModel != null || bundled
        binding.modelType.text =
            if (available) "Interactive 3D anatomy" else "Model download required"

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

        binding.modelProgress.visibility = if (available) View.VISIBLE else View.GONE
        val query = buildString {
            append("file:///android_asset/model_viewer.html?")
            if (source != null) {
                append("src=${Uri.encode(source)}")
            } else {
                append("missing=1")
            }
            append("&title=${Uri.encode(model.title)}")
            append("&parts=${Uri.encode(partsJson)}")
        }
        binding.modelWebView.loadUrl(query)
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
                if (_binding != null) selectPart(index, updateViewer = false)
            }
        }
    }

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "biology/3d"
        const val MODEL_HOST = "biology.local"
        const val GLB_MIME_TYPE = "model/gltf-binary"
        const val BRIDGE_NAME = "BiologyBridge"

        val selectedTextColor: Int = Color.parseColor("#AFA3FF")
        val inactiveTextColor: Int = Color.parseColor("#98A8C2")

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

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
