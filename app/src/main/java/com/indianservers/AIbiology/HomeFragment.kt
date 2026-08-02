package com.indianservers.AIbiology

import android.os.Bundle
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.indianservers.AIbiology.data.CatalogRefreshCoordinator
import com.indianservers.AIbiology.data.ModelDownloadRecord
import com.indianservers.AIbiology.data.ModelDownloadStatus
import com.indianservers.AIbiology.data.ModelRepository
import com.indianservers.AIbiology.databinding.FragmentHomeBinding
import com.indianservers.AIbiology.ui.BiologyModelAdapter
import com.indianservers.AIbiology.ui.DeviceProfile
import com.indianservers.AIbiology.ui.TvFocus

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val isTelevision = DeviceProfile.isTelevision(requireContext())
        configureInsets(isTelevision)
        configureLayout(isTelevision)
        configureActions(isTelevision)
        updateLibrarySummary()
        binding.homeVersion.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
        CatalogRefreshCoordinator.status.observe(viewLifecycleOwner) { status ->
            binding.homeLibraryStatus.text = status.label
        }
    }

    private fun configureInsets(isTelevision: Boolean) {
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val baseHeight = if (landscape) 64.dp else 88.dp
        val horizontal = if (isTelevision) 38.dp else 22.dp
        ViewCompat.setOnApplyWindowInsetsListener(binding.homeTopBar) { topBar, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            topBar.setPadding(
                horizontal + system.left,
                system.top,
                horizontal + system.right,
                0
            )
            topBar.layoutParams = topBar.layoutParams.apply {
                height = baseHeight + system.top
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun configureLayout(isTelevision: Boolean) {
        val screenWidthDp = resources.configuration.screenWidthDp
        val landscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val twoColumns = screenWidthDp >= 900 || (landscape && screenWidthDp >= 600)
        val cardHeight = if (landscape) 100.dp else 116.dp
        val maximumContentWidth =
            if (twoColumns) 1400.dp
            else if (screenWidthDp >= 600) 760.dp
            else Int.MAX_VALUE
        binding.homeContent.layoutParams = binding.homeContent.layoutParams.apply {
            width = if (maximumContentWidth != Int.MAX_VALUE) {
                minOf(
                    resources.displayMetrics.widthPixels - 48.dp,
                    maximumContentWidth
                )
            } else {
                ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        val cards = listOf(
            binding.exploreModelsModule,
            binding.humanAnatomyModule,
            binding.microscopeModule,
            binding.infographicsModule
        )
        cards.forEach { (it.parent as? ViewGroup)?.removeView(it) }
        binding.homeModuleGrid.removeAllViews()
        if (twoColumns) {
            if (landscape) {
                binding.homeContent.setPadding(18.dp, 10.dp, 18.dp, 18.dp)
            }
            cards.chunked(2).forEach { pair ->
                binding.homeModuleGrid.addView(
                    LinearLayout(requireContext()).apply {
                        orientation = LinearLayout.HORIZONTAL
                        isBaselineAligned = false
                        pair.forEach { card ->
                            addView(
                                card,
                                LinearLayout.LayoutParams(
                                    0,
                                    cardHeight,
                                    1f
                                ).apply {
                                    setMargins(6.dp, 6.dp, 6.dp, 6.dp)
                                }
                            )
                        }
                    },
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                )
            }
        } else {
            cards.forEach { card ->
                binding.homeModuleGrid.addView(
                    card,
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        cardHeight
                    ).apply { setMargins(6.dp, 6.dp, 6.dp, 6.dp) }
                )
            }
        }
        if (isTelevision) {
            binding.homeTitle.textSize = 26f
            cards.forEach { TvFocus.apply(it, focusedScale = 1.02f) }
            binding.exploreModelsModule.post { binding.exploreModelsModule.requestFocus() }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (_binding != null) {
            configureInsets(DeviceProfile.isTelevision(requireContext()))
            configureLayout(DeviceProfile.isTelevision(requireContext()))
        }
    }

    private fun configureActions(isTelevision: Boolean) {
        binding.exploreModelsModule.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_FirstFragment)
        }
        binding.humanAnatomyModule.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_FourthFragment)
        }
        binding.microscopeModule.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_ThirdFragment)
        }
        binding.infographicsModule.setOnClickListener {
            findNavController().navigate(R.id.action_HomeFragment_to_SecondFragment)
        }
        binding.homeKnowledgeCheck.setOnClickListener {
            findNavController().navigate(
                R.id.action_HomeFragment_to_FirstFragment,
                Bundle().apply { putBoolean(ARG_OPEN_KNOWLEDGE_CHECK, true) }
            )
        }
        binding.homeLibraryButton.setOnClickListener { showAppLibrary() }
        if (isTelevision) TvFocus.apply(binding.homeKnowledgeCheck, focusedScale = 1.02f)
        if (isTelevision) TvFocus.apply(binding.homeLibraryButton, focusedScale = 1.02f)
        if (!isTelevision) binding.homeScroll.isFocusable = false
    }

    private fun showAppLibrary() {
        val repository = ModelRepository(requireContext())
        val downloaded = repository.records()
            .filter { it.status == ModelDownloadStatus.DOWNLOADED }
        repository.close()
        if (downloaded.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("App Library")
                .setMessage("No downloaded 3D models are using device storage.")
                .setPositiveButton("Done", null)
                .show()
            return
        }
        val labels = downloaded.map {
            "${displayModelName(it)}\n${BiologyModelAdapter.formatBytes(it.fileSizeBytes)}"
        }.toTypedArray()
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("App Library")
            .setMessage(
                "${downloaded.size} downloaded models. Select one to remove it, " +
                    "or clear all downloaded models."
            )
            .setItems(labels) { _, index -> confirmModelRemoval(downloaded[index]) }
            .setNegativeButton("Close", null)
            .setNeutralButton("Clear all", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                confirmClearLibrary(dialog)
            }
        }
        dialog.show()
    }

    private fun confirmModelRemoval(record: ModelDownloadRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remove ${displayModelName(record)}?")
            .setMessage(
                "The downloaded model will be deleted from this device. " +
                    "You can download it again from its catalogue."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove") { _, _ ->
                val repository = ModelRepository(requireContext())
                repository.remove(record.modelId, requireExplicitConfirmation = false)
                repository.close()
                updateLibrarySummary()
                Toast.makeText(requireContext(), "Model removed.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmClearLibrary(parent: AlertDialog) {
        AlertDialog.Builder(requireContext())
            .setTitle("Clear all downloads?")
            .setMessage(
                "All downloaded 3D models will be removed to free space. " +
                    "Catalogue entries and online models remain available."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Clear downloads") { _, _ ->
                val repository = ModelRepository(requireContext())
                val (count, bytes) = repository.removeAllDownloaded()
                repository.close()
                parent.dismiss()
                updateLibrarySummary()
                Toast.makeText(
                    requireContext(),
                    "Removed $count models and freed ${BiologyModelAdapter.formatBytes(bytes)}.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun displayModelName(record: ModelDownloadRecord): String =
        record.modelId
            .substringAfterLast('/')
            .replace(Regex("^(ANATOMY_|MODEL_)", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .split(Regex("\\s+"))
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar(Char::titlecase)
            }

    private fun updateLibrarySummary() {
        val repository = ModelRepository(requireContext())
        val (count, bytes) = repository.storageSummary()
        binding.homeStorageSummary.text =
            if (count == 0) {
                "Models download on demand"
            } else {
                "$count saved  |  ${BiologyModelAdapter.formatBytes(bytes)}"
            }
        repository.close()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) updateLibrarySummary()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()

    private companion object {
        const val ARG_OPEN_KNOWLEDGE_CHECK = "open_knowledge_check"
    }
}
