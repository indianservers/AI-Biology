package com.indianservers.biology.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.indianservers.biology.R
import com.indianservers.biology.data.BiologyModel
import com.indianservers.biology.data.ModelDownloadRecord
import com.indianservers.biology.data.ModelDownloadStatus
import com.indianservers.biology.databinding.ItemModelCardBinding
import java.io.File
import java.util.concurrent.Executors

class BiologyModelAdapter(
    private val isAvailable: (BiologyModel) -> Boolean,
    private val isFavourite: (BiologyModel) -> Boolean,
    private val downloadRecord: (BiologyModel) -> ModelDownloadRecord?,
    private val thumbnailFile: (BiologyModel) -> File?,
    private val onSelected: (BiologyModel) -> Unit,
    private val onFavourite: (BiologyModel) -> Unit
) : ListAdapter<BiologyModel, BiologyModelAdapter.ModelViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModelViewHolder =
        ModelViewHolder(
            ItemModelCardBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ModelViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ModelViewHolder(
        private val binding: ItemModelCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: BiologyModel) {
            val available = isAvailable(model)
            val favourite = isFavourite(model)
            val record = downloadRecord(model)
            binding.modelCardTitle.text = model.title
            binding.modelCardCategory.text =
                listOfNotNull(model.categoryId, model.scientificName)
                    .joinToString("  |  ")
            binding.modelPlaceholder.text = model.badge
            binding.badgeAr.visibility = if (model.supportsAr) View.VISIBLE else View.GONE
            binding.badgePremium.visibility = if (model.isPremium) View.VISIBLE else View.GONE
            binding.favouriteButton.setImageResource(
                if (favourite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.favouriteButton.contentDescription =
                if (favourite) "Remove ${model.title} from favourites"
                else "Add ${model.title} to favourites"
            binding.modelLearningProgress.progress =
                (model.learningProgress.coerceIn(0f, 1f) * 100).toInt()
            binding.modelDownloadState.text = stateLabel(model, available, record)
            binding.modelDownloadState.setTextColor(
                binding.root.context.getColor(
                    when {
                        available || record?.status == ModelDownloadStatus.DOWNLOADED ->
                            R.color.model_state_ready
                        record?.status == ModelDownloadStatus.FAILED ->
                            R.color.model_state_error
                        else -> R.color.model_state_pending
                    }
                )
            )
            binding.root.contentDescription =
                "${model.title}. ${model.categoryId}. ${binding.modelDownloadState.text}."
            binding.root.setOnClickListener { onSelected(model) }
            binding.favouriteButton.setOnClickListener { onFavourite(model) }
            loadThumbnail(model)
        }

        private fun loadThumbnail(model: BiologyModel) {
            binding.modelThumbnail.setImageDrawable(null)
            binding.modelThumbnail.tag = model.id
            binding.modelPlaceholder.visibility = View.VISIBLE
            val file = thumbnailFile(model)?.takeIf(File::isFile) ?: return
            THUMBNAIL_EXECUTOR.execute {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@execute
                binding.modelThumbnail.post {
                    if (binding.modelThumbnail.tag == model.id) {
                        binding.modelThumbnail.setImageBitmap(bitmap)
                        binding.modelPlaceholder.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun stateLabel(
        model: BiologyModel,
        available: Boolean,
        record: ModelDownloadRecord?
    ): String {
        if (available) {
            return if (record?.status == ModelDownloadStatus.DOWNLOADED) {
                "DOWNLOADED"
            } else {
                "READY"
            }
        }
        return when (record?.status) {
            ModelDownloadStatus.QUEUED -> "QUEUED"
            ModelDownloadStatus.DOWNLOADING ->
                "DOWNLOADING ${(record.progress * 100).toInt()}%"
            ModelDownloadStatus.UPDATE_AVAILABLE -> "UPDATE AVAILABLE"
            ModelDownloadStatus.FAILED -> "RETRY DOWNLOAD"
            ModelDownloadStatus.DOWNLOADED -> "DOWNLOADED"
            else -> model.fileSizeBytes?.let {
                "${formatBytes(it)}  |  DOWNLOAD"
            } ?: if (model.glbUrl.isNullOrBlank()) "ONLINE SOURCE PENDING" else "DOWNLOAD"
        }
    }

    companion object {
        private val THUMBNAIL_EXECUTOR = Executors.newFixedThreadPool(2)
        private val DIFF = object : DiffUtil.ItemCallback<BiologyModel>() {
            override fun areItemsTheSame(oldItem: BiologyModel, newItem: BiologyModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BiologyModel, newItem: BiologyModel) =
                oldItem == newItem
        }

        fun formatBytes(bytes: Long): String =
            when {
                bytes >= 1024L * 1024L * 1024L ->
                    "%.1f GB".format(bytes / (1024f * 1024f * 1024f))
                bytes >= 1024L * 1024L ->
                    "%.1f MB".format(bytes / (1024f * 1024f))
                else -> "%.0f KB".format(bytes / 1024f)
            }
    }
}

