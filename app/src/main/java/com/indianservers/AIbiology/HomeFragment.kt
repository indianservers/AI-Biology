package com.indianservers.AIbiology

import android.os.Bundle
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
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
import com.indianservers.AIbiology.data.RecentlyViewedItem
import com.indianservers.AIbiology.data.RecentlyViewedStore
import com.indianservers.AIbiology.databinding.FragmentHomeBinding
import com.indianservers.AIbiology.ui.BiologyModelAdapter
import com.indianservers.AIbiology.ui.DeviceProfile
import com.indianservers.AIbiology.ui.TvFocus
import java.time.Year

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
        updateRecentlyViewed()
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
        binding.homeShareButton.setOnClickListener { AppActions.shareApp(requireContext()) }
        binding.homeMenuButton.setOnClickListener { anchor -> showAppMenu(anchor) }
        if (isTelevision) TvFocus.apply(binding.homeKnowledgeCheck, focusedScale = 1.02f)
        if (isTelevision) TvFocus.apply(binding.homeLibraryButton, focusedScale = 1.02f)
        if (isTelevision) TvFocus.apply(binding.homeShareButton, focusedScale = 1.06f)
        if (isTelevision) TvFocus.apply(binding.homeMenuButton, focusedScale = 1.06f)
        if (!isTelevision) binding.homeScroll.isFocusable = false
    }

    private fun showAppMenu(anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_main, menu)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_about) {
                    showAbout()
                    true
                } else {
                    false
                }
            }
            show()
        }
    }

    private fun showAbout() {
        val message = buildString {
            appendLine(getString(R.string.about_description))
            appendLine()
            appendLine(getString(R.string.about_disclaimer_title))
            appendLine(getString(R.string.about_disclaimer))
            appendLine()
            appendLine(getString(R.string.about_copyright, Year.now().value))
            append(getString(R.string.about_contact))
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.about_title)
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showAppLibrary() {
        val repository = ModelRepository(requireContext())
        val downloaded = repository.records()
            .filter { it.status == ModelDownloadStatus.DOWNLOADED }
        repository.close()
        if (downloaded.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("App Library")
                .setMessage(
                    "No downloaded 3D models are using device storage. " +
                        "Open Models or Human Anatomy and download only the models you want."
                )
                .setPositiveButton("Done", null)
                .show()
            return
        }
        val labels = downloaded.map {
            "${displayModelName(it)}\n${BiologyModelAdapter.formatBytes(it.fileSizeBytes)}"
        }.toTypedArray()
        val keepModel = BooleanArray(downloaded.size) { true }
        AlertDialog.Builder(requireContext())
            .setTitle("Keep checked models")
            .setMultiChoiceItems(labels, keepModel) { _, index, isChecked ->
                keepModel[index] = isChecked
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save selection") { _, _ ->
                saveLibrarySelection(downloaded, keepModel)
            }
            .show()
    }

    private fun saveLibrarySelection(
        downloaded: List<ModelDownloadRecord>,
        keepModel: BooleanArray
    ) {
        val removed = downloaded.filterIndexed { index, _ -> !keepModel[index] }
        if (removed.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "All downloaded models were kept.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val repository = ModelRepository(requireContext())
        removed.forEach {
            repository.remove(it.modelId, requireExplicitConfirmation = false)
        }
        repository.close()
        updateLibrarySummary()
        val freedBytes = removed.sumOf(ModelDownloadRecord::fileSizeBytes)
        Toast.makeText(
            requireContext(),
            "Kept ${downloaded.size - removed.size} models. " +
                "Freed ${BiologyModelAdapter.formatBytes(freedBytes)}.",
            Toast.LENGTH_LONG
        ).show()
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

    private fun updateRecentlyViewed() {
        if (_binding == null) return
        val items = RecentlyViewedStore(requireContext()).items()
        binding.homeRecentList.removeAllViews()
        binding.homeRecentSection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        items.take(6).forEach { item ->
            binding.homeRecentList.addView(createRecentItem(item))
        }
    }

    private fun createRecentItem(item: RecentlyViewedItem): TextView =
        TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(170.dp, 58.dp).apply {
                marginEnd = 8.dp
            }
            background = requireContext().getDrawable(R.drawable.bg_part_row)
            isClickable = true
            isFocusable = true
            gravity = android.view.Gravity.CENTER_VERTICAL
            maxLines = 2
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(13.dp, 7.dp, 13.dp, 7.dp)
            text = item.title
            textSize = 13f
            setTextColor(requireContext().getColor(R.color.white))
            contentDescription = "Resume ${item.title}"
            setOnClickListener {
                val arguments = Bundle().apply {
                    putString(RecentlyViewedStore.ARG_MODEL_ID, item.modelId)
                }
                when (item.destination) {
                    RecentlyViewedStore.DESTINATION_ANATOMY ->
                        findNavController().navigate(
                            R.id.action_HomeFragment_to_FourthFragment,
                            arguments
                        )
                    else ->
                        findNavController().navigate(
                            R.id.action_HomeFragment_to_FirstFragment,
                            arguments
                        )
                }
            }
            if (DeviceProfile.isTelevision(requireContext())) TvFocus.apply(this, 1.03f)
        }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            updateLibrarySummary()
            updateRecentlyViewed()
        }
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
