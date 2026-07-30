package com.indianservers.biology.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.indianservers.biology.R
import com.indianservers.biology.data.Infographic
import com.indianservers.biology.data.InfographicDownloadStatus
import com.indianservers.biology.databinding.ItemInfographicCardBinding
import java.io.File
import java.util.concurrent.Executors

class InfographicAdapter(
    private val thumbnailFile: (Infographic) -> File?,
    private val requestThumbnail: (Infographic, (File?) -> Unit) -> Unit,
    private val onOpen: (Infographic) -> Unit,
    private val onSaveOrRemove: (Infographic) -> Unit
) : ListAdapter<Infographic, InfographicAdapter.InfographicViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfographicViewHolder =
        InfographicViewHolder(
            ItemInfographicCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: InfographicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class InfographicViewHolder(
        private val binding: ItemInfographicCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(infographic: Infographic) {
            binding.infographicTitle.text = infographic.title
            binding.infographicSummary.text =
                infographic.summary.ifBlank { "Visual biology reference" }
            binding.infographicCategory.text = infographic.category.uppercase()
            binding.offlineBadge.visibility =
                if (infographic.isSaved) View.VISIBLE else View.GONE
            binding.infographicAction.text = actionLabel(infographic)
            binding.infographicAction.setTextColor(
                binding.root.context.getColor(
                    if (infographic.status == InfographicDownloadStatus.FAILED) {
                        R.color.model_state_error
                    } else {
                        R.color.white
                    }
                )
            )
            binding.root.contentDescription =
                "${infographic.title}. ${infographic.category}. ${actionLabel(infographic)}."
            binding.root.setOnClickListener { onOpen(infographic) }
            binding.infographicAction.setOnClickListener {
                if (
                    infographic.status != InfographicDownloadStatus.QUEUED &&
                    infographic.status != InfographicDownloadStatus.DOWNLOADING
                ) {
                    onSaveOrRemove(infographic)
                }
            }
            if (DeviceProfile.isTelevision(binding.root.context)) {
                binding.infographicTitle.textSize = 18f
                binding.infographicSummary.textSize = 14f
                binding.infographicCategory.textSize = 12f
                binding.infographicAction.textSize = 14f
                binding.infographicPreview.layoutParams =
                    binding.infographicPreview.layoutParams.apply {
                        height = (118 * binding.root.resources.displayMetrics.density).toInt()
                    }
                TvFocus.apply(binding.root)
                TvFocus.apply(binding.infographicAction, focusedScale = 1.03f)
            }
            loadThumbnail(infographic)
        }

        private fun loadThumbnail(infographic: Infographic) {
            binding.infographicThumbnail.setImageDrawable(null)
            binding.infographicThumbnail.tag = infographic.id
            binding.infographicPlaceholder.visibility = View.VISIBLE
            val localInfographic = infographic.localFilePath
                ?.let(::File)
                ?.takeIf(File::isFile)
            val thumbnail = thumbnailFile(infographic) ?: localInfographic
            if (thumbnail == null) {
                requestThumbnail(infographic) { downloaded ->
                    if (
                        downloaded?.isFile == true &&
                        binding.infographicThumbnail.tag == infographic.id
                    ) {
                        decode(infographic, downloaded)
                    }
                }
                return
            }
            decode(infographic, thumbnail)
        }

        private fun decode(infographic: Infographic, file: File) {
            IMAGE_EXECUTOR.execute {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                    ?: return@execute
                binding.infographicThumbnail.post {
                    if (binding.infographicThumbnail.tag == infographic.id) {
                        binding.infographicThumbnail.setImageBitmap(bitmap)
                        binding.infographicPlaceholder.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun actionLabel(infographic: Infographic): String =
        when (infographic.status) {
            InfographicDownloadStatus.QUEUED -> "Queued"
            InfographicDownloadStatus.DOWNLOADING ->
                "Saving ${(infographic.progress * 100).toInt()}%"
            InfographicDownloadStatus.SAVED -> "Remove download"
            InfographicDownloadStatus.UPDATE_AVAILABLE -> "Update offline"
            InfographicDownloadStatus.FAILED -> "Retry save"
            InfographicDownloadStatus.NOT_SAVED ->
                infographic.fileSizeBytes?.let {
                    "Save offline  |  ${BiologyModelAdapter.formatBytes(it)}"
                } ?: "Save offline"
        }

    companion object {
        private val IMAGE_EXECUTOR = Executors.newFixedThreadPool(2)
        private val DIFF = object : DiffUtil.ItemCallback<Infographic>() {
            override fun areItemsTheSame(oldItem: Infographic, newItem: Infographic) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Infographic, newItem: Infographic) =
                oldItem == newItem
        }
    }
}
