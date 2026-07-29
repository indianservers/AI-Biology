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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.indianservers.biology.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var modelFiles: List<String> = emptyList()

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
        modelFiles = requireContext().assets
            .list(MODEL_ASSET_DIRECTORY)
            ?.filter { it.endsWith(".glb", ignoreCase = true) }
            ?.sortedWith(String.CASE_INSENSITIVE_ORDER)
            .orEmpty()

        val modelNames = modelFiles.map(::displayName)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            modelNames
        ).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.modelSpinner.adapter = adapter
        binding.modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                loadModel(modelFiles[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        if (modelFiles.isEmpty()) {
            binding.selectedModelText.text = getString(R.string.selected_model, "No local models found")
        }
    }

    private fun loadModel(fileName: String) {
        binding.selectedModelText.text = getString(R.string.selected_model, displayName(fileName))
        binding.modelProgress.visibility = View.VISIBLE
        binding.modelWebView.loadUrl("file:///android_asset/model_viewer.html?model=${Uri.encode(fileName)}")
    }

    private fun displayName(fileName: String): String =
        fileName.substringBeforeLast('.')
            .replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")
            .replaceFirstChar { it.uppercase() }

    private companion object {
        const val MODEL_ASSET_DIRECTORY = "biology/3d"
    }
}
