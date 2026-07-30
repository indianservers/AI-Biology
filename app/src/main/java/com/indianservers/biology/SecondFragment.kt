package com.indianservers.biology

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.indianservers.biology.data.Infographic
import com.indianservers.biology.data.InfographicDownloadStatus
import com.indianservers.biology.data.InfographicRepository
import com.indianservers.biology.databinding.DialogInfographicViewerBinding
import com.indianservers.biology.databinding.FragmentSecondBinding
import com.indianservers.biology.ui.InfographicAdapter
import com.indianservers.biology.ui.DeviceProfile
import com.indianservers.biology.ui.TvFocus
import java.io.File
import java.util.concurrent.Executors

class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: InfographicRepository
    private lateinit var adapter: InfographicAdapter
    private var catalogue = emptyList<Infographic>()
    private var query = ""
    private var showSavedOnly = false
    private var isTelevision = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = InfographicRepository(
            requireContext(),
            BuildConfig.BIOLOGY_INFOGRAPHIC_CATALOG_URL
        )
        isTelevision = DeviceProfile.isTelevision(requireContext())
        configureInsets()
        configureGrid()
        configureActions()
        loadCatalogue()
    }

    private fun configureInsets() {
        val baseHeight = 82.dp
        val start = if (isTelevision) 36.dp else binding.libraryTopBar.paddingStart
        val end = if (isTelevision) 36.dp else binding.libraryTopBar.paddingEnd
        ViewCompat.setOnApplyWindowInsetsListener(binding.libraryTopBar) { view, insets ->
            val system = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(start + system.left, system.top, end + system.right, 0)
            view.layoutParams = view.layoutParams.apply { height = baseHeight + system.top }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun configureGrid() {
        val columns = when {
            isTelevision -> 5
            resources.configuration.smallestScreenWidthDp >= 840 -> 4
            resources.configuration.smallestScreenWidthDp >= 600 -> 3
            else -> 2
        }
        adapter = InfographicAdapter(
            thumbnailFile = repository::thumbnailFile,
            requestThumbnail = repository::loadThumbnail,
            onOpen = ::openInfographic,
            onSaveOrRemove = ::saveOrRemove
        )
        binding.infographicGrid.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.infographicGrid.adapter = adapter
        if (isTelevision) {
            binding.infographicGrid.setPadding(26.dp, 8.dp, 26.dp, 24.dp)
            binding.libraryTitle.textSize = 24f
            binding.librarySubtitle.textSize = 14f
            binding.infographicSearch.textSize = 16f
            binding.allInfographicsFilter.textSize = 15f
            binding.savedInfographicsFilter.textSize = 15f
            listOf(
                binding.libraryBack,
                binding.libraryRefresh,
                binding.infographicSearch,
                binding.allInfographicsFilter,
                binding.savedInfographicsFilter
            ).forEach { TvFocus.apply(it) }
            binding.allInfographicsFilter.post {
                if (_binding != null && adapter.itemCount == 0) {
                    binding.allInfographicsFilter.requestFocus()
                }
            }
        }
    }

    private fun configureActions() {
        binding.libraryBack.setOnClickListener {
            if (!findNavController().popBackStack()) {
                findNavController().navigate(R.id.FirstFragment)
            }
        }
        binding.libraryRefresh.setOnClickListener { loadCatalogue() }
        binding.infographicSearch.doAfterTextChanged {
            query = it?.toString().orEmpty().trim()
            render()
        }
        binding.allInfographicsFilter.setOnClickListener {
            showSavedOnly = false
            updateFilters()
            render()
        }
        binding.savedInfographicsFilter.setOnClickListener {
            showSavedOnly = true
            updateFilters()
            render()
        }
    }

    private fun loadCatalogue() {
        showLoading()
        repository.refresh { result ->
            if (_binding == null) return@refresh
            catalogue = result.infographics
            render()
            result.warning?.takeIf { catalogue.isNotEmpty() }?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun render() {
        val terms = query.lowercase().split(Regex("\\s+")).filter(String::isNotBlank)
        val visible = catalogue.filter { infographic ->
            (!showSavedOnly || infographic.isSaved) &&
                terms.all { term ->
                    listOf(
                        infographic.title,
                        infographic.summary,
                        infographic.category,
                        infographic.tags.joinToString(" ")
                    ).any { value -> term in value.lowercase() }
                }
        }
        adapter.submitList(visible)
        binding.infographicCount.text =
            resources.getQuantityString(R.plurals.infographic_count, visible.size, visible.size)
        binding.infographicGrid.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        binding.libraryEmptyState.visibility =
            if (visible.isEmpty()) View.VISIBLE else View.GONE
        binding.libraryLoading.visibility = View.GONE
        if (visible.isEmpty()) {
            when {
                showSavedOnly -> {
                    binding.libraryEmptyTitle.text = "No offline infographics"
                    binding.libraryEmptyMessage.text =
                        "Save a visual guide and it will appear here for offline study."
                }
                query.isNotBlank() -> {
                    binding.libraryEmptyTitle.text = "No matching topics"
                    binding.libraryEmptyMessage.text =
                        "Try a broader biological term or clear the search."
                }
                BuildConfig.BIOLOGY_INFOGRAPHIC_CATALOG_URL.isBlank() -> {
                    binding.libraryEmptyTitle.text = "Catalogue ready for connection"
                    binding.libraryEmptyMessage.text =
                        "The infographic library will populate when the website catalogue URL is configured."
                }
                else -> {
                    binding.libraryEmptyTitle.text = "Library unavailable"
                    binding.libraryEmptyMessage.text =
                        "Check the connection and refresh the catalogue."
                }
            }
        }
    }

    private fun showLoading() {
        binding.infographicGrid.visibility = View.GONE
        binding.libraryEmptyState.visibility = View.VISIBLE
        binding.libraryLoading.visibility = View.VISIBLE
        binding.libraryEmptyTitle.text = "Loading visual library"
        binding.libraryEmptyMessage.text = "Checking online details and saved items."
    }

    private fun updateFilters() {
        binding.allInfographicsFilter.setBackgroundResource(
            if (showSavedOnly) R.drawable.bg_filter_chip
            else R.drawable.bg_filter_chip_selected
        )
        binding.savedInfographicsFilter.setBackgroundResource(
            if (showSavedOnly) R.drawable.bg_filter_chip_selected
            else R.drawable.bg_filter_chip
        )
        binding.allInfographicsFilter.setTextColor(
            requireContext().getColor(
                if (showSavedOnly) R.color.model_state_pending else R.color.white
            )
        )
        binding.savedInfographicsFilter.setTextColor(
            requireContext().getColor(
                if (showSavedOnly) R.color.white else R.color.model_state_pending
            )
        )
    }

    private fun saveOrRemove(infographic: Infographic) {
        if (infographic.status == InfographicDownloadStatus.SAVED) {
            AlertDialog.Builder(requireContext())
                .setTitle("Remove offline copy?")
                .setMessage("${infographic.title} will remain available in the online catalogue.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove") { _, _ ->
                    updateItem(repository.remove(infographic))
                }
                .show()
            return
        }
        repository.save(infographic) { updated ->
            if (_binding == null) return@save
            updateItem(updated)
            when (updated.status) {
                InfographicDownloadStatus.SAVED ->
                    Toast.makeText(
                        requireContext(),
                        "${updated.title} saved for offline study.",
                        Toast.LENGTH_SHORT
                    ).show()
                InfographicDownloadStatus.FAILED ->
                    Toast.makeText(
                        requireContext(),
                        updated.errorMessage ?: "Could not save infographic.",
                        Toast.LENGTH_LONG
                    ).show()
                else -> Unit
            }
        }
    }

    private fun updateItem(updated: Infographic) {
        catalogue = catalogue.map { if (it.id == updated.id) updated else it }
        render()
    }

    private fun openInfographic(infographic: Infographic) {
        val file = infographic.localFilePath?.let(::File)?.takeIf(File::isFile)
        if (file != null) {
            showFullScreen(infographic, file)
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(infographic.title)
            .setMessage(
                listOfNotNull(
                    infographic.summary.takeIf(String::isNotBlank),
                    infographic.sourceTitle?.let { "Source: $it" },
                    infographic.reviewedAt?.let { "Reviewed: $it" }
                ).joinToString("\n\n")
            )
            .setNegativeButton("Close", null)
            .setPositiveButton("Save offline") { _, _ -> saveOrRemove(infographic) }
            .show()
    }

    private fun showFullScreen(infographic: Infographic, file: File) {
        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val viewer = DialogInfographicViewerBinding.inflate(layoutInflater)
        dialog.setContentView(viewer.root)
        viewer.fullInfographicTitle.text = infographic.title
        viewer.closeInfographicViewer.setOnClickListener { dialog.dismiss() }
        if (isTelevision) {
            TvFocus.apply(viewer.closeInfographicViewer)
            viewer.closeInfographicViewer.requestFocus()
        }
        dialog.setOnDismissListener {
            (viewer.fullInfographicImage.drawable as? android.graphics.drawable.BitmapDrawable)
                ?.bitmap
                ?.takeIf { !it.isRecycled }
                ?.recycle()
        }
        dialog.show()
        IMAGE_EXECUTOR.execute {
            val bitmap = decodeForScreen(file)
            viewer.fullInfographicImage.post {
                if (!dialog.isShowing) {
                    bitmap?.recycle()
                } else {
                    viewer.fullInfographicLoading.visibility = View.GONE
                    viewer.fullInfographicImage.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decodeForScreen(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val maximum = maxOf(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        ) * 2
        var sample = 1
        while (bounds.outWidth / sample > maximum || bounds.outHeight / sample > maximum) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        )
    }

    override fun onDestroyView() {
        repository.close()
        binding.infographicGrid.adapter = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private val IMAGE_EXECUTOR = Executors.newSingleThreadExecutor()
    }
}

private val Int.dp: Int
    get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).toInt()
