package com.indianservers.AIbiology.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.indianservers.AIbiology.R
import com.indianservers.AIbiology.data.MicroscopySlide
import com.indianservers.AIbiology.databinding.ItemMicroscopySlideBinding
import java.io.File

class MicroscopySlideAdapter(
    private val thumbnailFile: (MicroscopySlide) -> File?,
    private val requestThumbnail: (MicroscopySlide, (File?) -> Unit) -> Unit,
    private val onSelected: (MicroscopySlide) -> Unit
) : ListAdapter<MicroscopySlide, MicroscopySlideAdapter.SlideHolder>(DIFF) {
    var selectedId: String? = null
        set(value) {
            val previous = currentList.indexOfFirst { it.id == field }
            field = value
            val current = currentList.indexOfFirst { it.id == value }
            if (previous >= 0) notifyItemChanged(previous)
            if (current >= 0) notifyItemChanged(current)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SlideHolder(
        ItemMicroscopySlideBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: SlideHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SlideHolder(
        private val binding: ItemMicroscopySlideBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(slide: MicroscopySlide) {
            binding.slideCardTitle.text = slide.title
            binding.slideCardMeta.text = listOfNotNull(slide.stain, slide.magnification)
                .joinToString(" · ")
                .ifBlank { slide.category }
            binding.slideMonogram.text = slide.title
                .split(Regex("\\s+"))
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercase() }
                .joinToString("")
            binding.root.setBackgroundResource(
                if (slide.id == selectedId) R.drawable.bg_microscopy_selected
                else R.drawable.bg_part_row
            )
            binding.root.contentDescription = buildString {
                append(slide.title)
                slide.stain?.let { append(", stain $it") }
                slide.magnification?.let { append(", $it") }
                if (slide.id == selectedId) append(", selected")
            }
            binding.root.setOnClickListener { onSelected(slide) }
            if (DeviceProfile.isTelevision(binding.root.context)) TvFocus.apply(binding.root)
            loadThumbnail(slide)
        }

        private fun loadThumbnail(slide: MicroscopySlide) {
            binding.slideThumbnail.setImageDrawable(null)
            binding.slideThumbnail.tag = slide.id
            binding.slideMonogram.visibility = android.view.View.VISIBLE
            val available = thumbnailFile(slide)
            if (available != null) {
                showThumbnail(slide.id, available)
            } else {
                requestThumbnail(slide) { file ->
                    if (file != null) showThumbnail(slide.id, file)
                }
            }
        }

        private fun showThumbnail(expectedId: String, file: File) {
            if (binding.slideThumbnail.tag != expectedId) return
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            binding.slideThumbnail.setImageBitmap(bitmap)
            binding.slideMonogram.visibility = android.view.View.GONE
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MicroscopySlide>() {
            override fun areItemsTheSame(oldItem: MicroscopySlide, newItem: MicroscopySlide) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MicroscopySlide, newItem: MicroscopySlide) =
                oldItem == newItem
        }
    }
}
