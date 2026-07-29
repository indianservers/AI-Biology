package com.indianservers.biology

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.io.File
import java.io.FileInputStream

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var selectedModelIndex = 0
    private var selectedPartIndex = 0
    private lateinit var modelStorageDirectory: File

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        modelStorageDirectory = File(requireContext().filesDir, MODEL_ASSET_DIRECTORY)
        configureViewer()
        configurePartList()
        configureModelStrip()
    }

    override fun onDestroyView() {
        binding.modelWebView.destroy()
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureViewer() {
        binding.modelWebView.apply {
            setBackgroundColor(Color.TRANSPARENT)
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
                    val modelFile = findExternalModel(fileName) ?: return null
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
                    binding.modelProgress.visibility =
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
        }
    }

    private fun configureModelStrip() {
        binding.modelStrip.removeAllViews()

        availableModels().forEachIndexed { index, model ->
            binding.modelStrip.addView(createModelStripItem(index, model))
        }

        selectModel(0)
    }

    private fun configurePartList() {
        binding.partsList.removeAllViews()
        CELL_PARTS.forEachIndexed { index, part ->
            binding.partsList.addView(createPartRow(index, part))
        }
        binding.cellPartsPanel.setOnClickListener {
            binding.partsList.visibility = collapseOrExpand(binding.partsList.visibility)
        }
        binding.toolRail.setOnClickListener {
            binding.toolRail.visibility = View.GONE
        }
        binding.infoPanel.setOnClickListener {
            binding.actionRow.visibility = collapseOrExpand(binding.actionRow.visibility)
        }
        binding.modelStripContainer.setOnClickListener {
            binding.modelStrip.visibility = collapseOrExpand(binding.modelStrip.visibility)
        }
        selectPart(0)
    }

    private fun createModelStripItem(index: Int, model: BiologyModel): View {
        val item = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(4.dp, 0, 4.dp, 0)
            layoutParams = LinearLayout.LayoutParams(154.dp, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener { selectModel(index) }
        }

        val badge = TextView(requireContext()).apply {
            width = 132.dp
            height = 78.dp
            gravity = Gravity.CENTER
            text = model.badge
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            background = requireContext().getDrawable(
                if (index == selectedModelIndex) R.drawable.bg_thumbnail_selected else R.drawable.bg_thumbnail
            )
        }

        val label = TextView(requireContext()).apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.WRAP_CONTENT
            gravity = Gravity.CENTER
            maxLines = 2
            text = model.shortTitle
            setTextColor(if (index == selectedModelIndex) selectedTextColor else inactiveTextColor)
            textSize = 12f
        }

        item.addView(badge)
        item.addView(label)
        return item
    }

    private fun createPartRow(index: Int, part: CellPart): View {
        val row = TextView(requireContext()).apply {
            height = 48.dp
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10.dp, 0, 8.dp, 0)
            text = "${part.badge}  ${part.title}     ${index + 1}"
            setTextColor(Color.WHITE)
            textSize = 14f
            maxLines = 1
            background = requireContext().getDrawable(
                if (index == selectedPartIndex) R.drawable.bg_part_row_selected else R.drawable.bg_part_row
            )
            setOnClickListener { selectPart(index) }
        }
        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            48.dp
        ).apply {
            bottomMargin = 6.dp
        }
        return row
    }

    private fun selectModel(index: Int) {
        selectedModelIndex = index
        val models = availableModels()

        models.forEachIndexed { childIndex, _ ->
            val item = binding.modelStrip.getChildAt(childIndex) as? LinearLayout
            val badge = item?.getChildAt(0) as? TextView
            val label = item?.getChildAt(1) as? TextView

            badge?.background = requireContext().getDrawable(
                if (childIndex == selectedModelIndex) R.drawable.bg_thumbnail_selected else R.drawable.bg_thumbnail
            )
            label?.setTextColor(if (childIndex == selectedModelIndex) selectedTextColor else inactiveTextColor)
        }

        loadModel(models[index])
    }

    private fun selectPart(index: Int) {
        selectedPartIndex = index

        CELL_PARTS.forEachIndexed { childIndex, _ ->
            binding.partsList.getChildAt(childIndex)?.background = requireContext().getDrawable(
                if (childIndex == selectedPartIndex) R.drawable.bg_part_row_selected else R.drawable.bg_part_row
            )
        }

        val part = CELL_PARTS[index]
        binding.featureNumber.text = (index + 1).toString()
        binding.featureTitle.text = part.title
        binding.featureDescription.text = part.description
        binding.partCallout.text = "${index + 1}  ${part.title}"
    }

    private fun loadModel(model: BiologyModel) {
        binding.modelTitle.text = model.title
        binding.featureThumb.text = model.badge

        val localModelUri = findExternalModel(model.fileName)?.let {
            "https://$MODEL_HOST/models/${Uri.encode(model.fileName)}"
        }

        if (localModelUri != null) {
            binding.modelProgress.visibility = View.VISIBLE
            binding.modelWebView.loadUrl(
                "file:///android_asset/model_viewer.html?src=${Uri.encode(localModelUri)}" +
                    "&title=${Uri.encode(model.title)}"
            )
        } else if (isBundledModelAvailable(model.fileName)) {
            binding.modelProgress.visibility = View.VISIBLE
            binding.modelWebView.loadUrl(
                "file:///android_asset/model_viewer.html?model=${Uri.encode(model.fileName)}"
            )
        } else {
            binding.modelProgress.visibility = View.GONE
            binding.modelWebView.loadUrl(
                "file:///android_asset/model_viewer.html?fallback=${Uri.encode(model.fileName)}" +
                    "&title=${Uri.encode(model.title)}"
            )
        }
    }

    private fun collapseOrExpand(currentVisibility: Int): Int =
        if (currentVisibility == View.VISIBLE) View.GONE else View.VISIBLE

    private fun findExternalModel(fileName: String): File? {
        val modelFile = File(modelStorageDirectory, fileName)
        return modelFile.takeIf { it.exists() && it.length() > 0 }
    }

    private fun availableModels(): List<BiologyModel> {
        val available = MODEL_CATALOG.filter {
            findExternalModel(it.fileName) != null || isBundledModelAvailable(it.fileName)
        }

        return available.ifEmpty { MODEL_CATALOG }
    }

    private fun isBundledModelAvailable(fileName: String): Boolean =
        requireContext().assets
            .list(MODEL_ASSET_DIRECTORY)
            ?.contains(fileName) == true

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "biology/3d"
        const val MODEL_HOST = "biology.local"
        const val GLB_MIME_TYPE = "model/gltf-binary"

        val selectedTextColor: Int = Color.parseColor("#AFA3FF")
        val inactiveTextColor: Int = Color.parseColor("#98A8C2")

        val CELL_PARTS = listOf(
            CellPart("Nucleus", "NU", "The nucleus is the control center of the cell. It contains genetic material and regulates cell activities."),
            CellPart("Rough ER", "ER", "Rough endoplasmic reticulum supports protein folding and transport through the cell."),
            CellPart("Golgi Apparatus", "GA", "The Golgi apparatus modifies, sorts, and packages proteins for delivery."),
            CellPart("Mitochondria", "MT", "Mitochondria generate ATP, the usable energy that powers many cell processes."),
            CellPart("Lysosomes", "LY", "Lysosomes break down waste and recycle worn cellular components."),
            CellPart("Ribosomes", "RB", "Ribosomes assemble amino acids into proteins using genetic instructions."),
            CellPart("Cell Membrane", "CM", "The membrane controls what enters and leaves the cell."),
            CellPart("Cytoplasm", "CY", "Cytoplasm suspends organelles and hosts many cell reactions.")
        )

        val MODEL_CATALOG = listOf(
            BiologyModel("Bacteriacell.glb", "Bacteria Cell", "Bacteria", "BC", "Prokaryote", "A compact prokaryotic model showing the cell envelope, cytoplasm, and internal organization."),
            BiologyModel("Cell Membrane.glb", "Cell Membrane", "Membrane", "CM", "Selective barrier", "The membrane controls movement in and out of the cell through a flexible phospholipid boundary."),
            BiologyModel("Chloroplast.glb", "Chloroplast", "Chloroplast", "CH", "Photosynthesis", "Chloroplasts capture light energy and convert it into chemical energy in plant cells."),
            BiologyModel("epithelial microvilli.glb", "Epithelial Microvilli", "Microvilli", "MV", "Absorption", "Microvilli increase surface area so epithelial cells can absorb nutrients efficiently."),
            BiologyModel("Lysosome.glb", "Lysosome", "Lysosome", "LY", "Cell recycling", "Lysosomes break down waste, worn cell parts, and foreign material using digestive enzymes."),
            BiologyModel("Mitochondrion.glb", "Mitochondrion", "Mitochondria", "MT", "Cell energy", "Mitochondria generate ATP, the usable energy that powers many cell processes."),
            BiologyModel("Neuron.glb", "Neuron", "Neuron", "NE", "Nerve signals", "Neurons transmit electrical and chemical signals across the nervous system."),
            BiologyModel("plant cell wall.glb", "Plant Cell Wall", "Cell Wall", "CW", "Plant support", "The cell wall gives plant cells structure, protection, and mechanical strength."),
            BiologyModel("PlantCell.glb", "Plant Cell", "Plant Cell", "PC", "Complete cell", "A full plant cell model with major organelles arranged inside a rigid boundary."),
            BiologyModel("Ribosomes.glb", "Ribosomes", "Ribosomes", "RB", "Protein synthesis", "Ribosomes read genetic instructions and assemble amino acids into proteins."),
            BiologyModel("Rough Endoplasmic Reticulum.glb", "Rough Endoplasmic Reticulum", "Rough ER", "ER", "Protein network", "Rough ER helps fold and process proteins before they move through the cell."),
            BiologyModel("Smooth Endoplasmic Reticulum.glb", "Smooth Endoplasmic Reticulum", "Smooth ER", "SE", "Lipid network", "Smooth ER supports lipid synthesis, detoxification, and calcium storage."),
            BiologyModel("Vacuole.glb", "Vacuole", "Vacuole", "VA", "Storage", "Vacuoles store water and solutes while helping maintain pressure in plant cells."),
            BiologyModel("WhiteBloodCell.glb", "White Blood Cell", "WBC", "WB", "Immune defense", "White blood cells identify threats and help protect the body from infection.")
        )
    }
}

private data class BiologyModel(
    val fileName: String,
    val title: String,
    val shortTitle: String,
    val badge: String,
    val featureTitle: String,
    val description: String
)

private data class CellPart(
    val title: String,
    val badge: String,
    val description: String
)

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
