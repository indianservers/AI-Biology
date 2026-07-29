package com.indianservers.biology

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import com.indianservers.biology.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configureViewer()
        configureModelList()
    }

    override fun onDestroyView() {
        binding.modelWebView.destroy()
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureViewer() {
        binding.modelWebView.apply {
            webViewClient = WebViewClient()
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
            settings.cacheMode = WebSettings.LOAD_DEFAULT
        }
    }

    private fun configureModelList() {
        val adapter = SimpleAdapter(
            requireContext(),
            MODEL_CATALOG.map { mapOf("title" to it.title, "subtitle" to it.subtitle) },
            android.R.layout.simple_list_item_activated_2,
            arrayOf("title", "subtitle"),
            intArrayOf(android.R.id.text1, android.R.id.text2)
        )

        binding.modelList.adapter = adapter
        binding.modelList.setOnItemClickListener { _, _, position, _ ->
            loadModel(MODEL_CATALOG[position])
        }

        binding.modelList.setItemChecked(0, true)
        loadModel(MODEL_CATALOG.first())
    }

    private fun loadModel(model: BiologyModel) {
        binding.selectedModelText.text = getString(R.string.selected_model, model.title, model.subtitle)

        if (isBundledModelAvailable(model.fileName)) {
            binding.modelProgress.visibility = View.VISIBLE
            binding.modelWebView.loadUrl(
                "file:///android_asset/model_viewer.html?model=${Uri.encode(model.fileName)}"
            )
        } else {
            binding.modelProgress.visibility = View.GONE
            binding.modelWebView.loadUrl(
                "file:///android_asset/model_viewer.html?pending=${Uri.encode(model.fileName)}" +
                    "&title=${Uri.encode(model.title)}"
            )
        }
    }

    private fun isBundledModelAvailable(fileName: String): Boolean =
        requireContext().assets
            .list(MODEL_ASSET_DIRECTORY)
            ?.contains(fileName) == true

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "biology/3d"

        val MODEL_CATALOG = listOf(
            BiologyModel("Bacteriacell.glb", "Bacteria Cell", "prokaryotic cell structure"),
            BiologyModel("Cell Membrane.glb", "Cell Membrane", "selective barrier and transport"),
            BiologyModel("Chloroplast.glb", "Chloroplast", "photosynthesis organelle"),
            BiologyModel("epithelial microvilli.glb", "Epithelial Microvilli", "surface absorption structures"),
            BiologyModel("Lysosome.glb", "Lysosome", "cellular digestion vesicle"),
            BiologyModel("Mitochondrion.glb", "Mitochondrion", "cell energy organelle"),
            BiologyModel("Neuron.glb", "Neuron", "nerve impulse cell"),
            BiologyModel("plant cell wall.glb", "Plant Cell Wall", "rigid plant cell support"),
            BiologyModel("PlantCell.glb", "Plant Cell", "complete plant cell model"),
            BiologyModel("Ribosomes.glb", "Ribosomes", "protein synthesis machinery"),
            BiologyModel("Rough Endoplasmic Reticulum.glb", "Rough Endoplasmic Reticulum", "protein folding network"),
            BiologyModel("Smooth Endoplasmic Reticulum.glb", "Smooth Endoplasmic Reticulum", "lipid synthesis network"),
            BiologyModel("Vacuole.glb", "Vacuole", "storage and pressure compartment"),
            BiologyModel("WhiteBloodCell.glb", "White Blood Cell", "immune defense cell")
        )
    }
}

private data class BiologyModel(
    val fileName: String,
    val title: String,
    val subtitle: String
)
