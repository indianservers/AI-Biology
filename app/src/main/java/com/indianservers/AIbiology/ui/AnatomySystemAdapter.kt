package com.indianservers.AIbiology.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.indianservers.AIbiology.R
import com.indianservers.AIbiology.data.BiologyModel
import com.indianservers.AIbiology.data.ModelDownloadRecord
import com.indianservers.AIbiology.data.ModelDownloadStatus
import com.indianservers.AIbiology.databinding.ItemAnatomySystemBinding
import java.io.File
import java.util.concurrent.Executors

class AnatomySystemAdapter(
    private val record: (BiologyModel) -> ModelDownloadRecord?,
    private val thumbnailFile: (BiologyModel) -> File?,
    private val requestThumbnail: (BiologyModel, (File?) -> Unit) -> Unit,
    private val onSelected: (BiologyModel) -> Unit,
    private val onAction: (BiologyModel) -> Unit
) : ListAdapter<BiologyModel, AnatomySystemAdapter.SystemHolder>(DIFF) {
    var selectedId: String? = null
        set(value) {
            val previous = currentList.indexOfFirst { it.id == field }
            field = value
            val current = currentList.indexOfFirst { it.id == value }
            if (previous >= 0) notifyItemChanged(previous)
            if (current >= 0) notifyItemChanged(current)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SystemHolder {
        val binding = ItemAnatomySystemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        if (
            DeviceProfile.isTelevision(parent.context) ||
            parent.resources.configuration.smallestScreenWidthDp >= 600
        ) {
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        return SystemHolder(binding)
    }

    override fun onBindViewHolder(holder: SystemHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SystemHolder(
        private val binding: ItemAnatomySystemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(model: BiologyModel) {
            val download = record(model)
            binding.anatomySystemTitle.text = model.title
            binding.anatomySystemStatus.text = status(model, download)
            binding.anatomyDownloadButton.text = actionLabel(download)
            val downloading = download?.status == ModelDownloadStatus.DOWNLOADING ||
                download?.status == ModelDownloadStatus.QUEUED ||
                download?.status == ModelDownloadStatus.PAUSED
            binding.anatomyDownloadProgress.visibility =
                if (downloading) View.VISIBLE else View.GONE
            binding.anatomyDownloadProgress.isIndeterminate =
                download?.status == ModelDownloadStatus.QUEUED
            binding.anatomyDownloadProgress.progress =
                ((download?.progress ?: 0f).coerceIn(0f, 1f) * 100).toInt()
            binding.anatomySystemStatus.setTextColor(
                binding.root.context.getColor(
                    when (download?.status) {
                        ModelDownloadStatus.DOWNLOADED -> R.color.model_state_ready
                        ModelDownloadStatus.FAILED -> R.color.model_state_error
                        else -> R.color.model_state_pending
                    }
                )
            )
            binding.anatomyMonogram.text = model.badge
            binding.root.setBackgroundResource(
                if (model.id == selectedId) R.drawable.bg_microscopy_selected
                else R.drawable.bg_part_row
            )
            binding.root.contentDescription =
                "${model.title}. ${binding.anatomySystemStatus.text}."
            binding.root.setOnClickListener { onSelected(model) }
            binding.anatomyDownloadButton.setOnClickListener { onAction(model) }
            binding.anatomyDownloadButton.contentDescription =
                "${binding.anatomyDownloadButton.text} ${model.title}"
            if (DeviceProfile.isTelevision(binding.root.context)) TvFocus.apply(binding.root)
            loadThumbnail(model)
        }

        private fun loadThumbnail(model: BiologyModel) {
            binding.anatomyThumbnail.setImageDrawable(null)
            binding.anatomyThumbnail.tag = model.id
            binding.anatomyMonogram.visibility = View.VISIBLE
            val available = thumbnailFile(model)
            if (available != null) {
                decode(model.id, available)
            } else {
                requestThumbnail(model) { file ->
                    if (file != null) decode(model.id, file)
                }
            }
        }

        private fun decode(expectedId: String, file: File) {
            THUMBNAIL_EXECUTOR.execute {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@execute
                binding.anatomyThumbnail.post {
                    if (binding.anatomyThumbnail.tag == expectedId) {
                        binding.anatomyThumbnail.setImageBitmap(bitmap)
                        binding.anatomyMonogram.visibility = View.GONE
                    }
                }
            }
        }

        private fun status(
            model: BiologyModel,
            download: ModelDownloadRecord?
        ): String = when (download?.status) {
            ModelDownloadStatus.DOWNLOADED -> "AVAILABLE OFFLINE"
            ModelDownloadStatus.QUEUED -> "WAITING TO DOWNLOAD"
            ModelDownloadStatus.DOWNLOADING ->
                "DOWNLOADING ${(download.progress * 100).toInt()}%"
            ModelDownloadStatus.PAUSED ->
                "PAUSED ${(download.progress * 100).toInt()}%"
            ModelDownloadStatus.FAILED -> "DOWNLOAD FAILED"
            ModelDownloadStatus.UPDATE_AVAILABLE -> "UPDATE READY"
            else -> model.packageSizeBytes?.let {
                BiologyModelAdapter.formatBytes(it)
            } ?: "ONLINE MODEL"
        }

        private fun actionLabel(download: ModelDownloadRecord?): String =
            when (download?.status) {
                ModelDownloadStatus.DOWNLOADED -> "Open"
                ModelDownloadStatus.QUEUED -> "Queued"
                ModelDownloadStatus.DOWNLOADING ->
                    "Pause ${(download.progress * 100).toInt()}%"
                ModelDownloadStatus.PAUSED ->
                    "Resume ${(download.progress * 100).toInt()}%"
                ModelDownloadStatus.FAILED -> "Retry"
                ModelDownloadStatus.UPDATE_AVAILABLE -> "Update"
                else -> "Download"
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
    }
}
