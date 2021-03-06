package com.mgt.zalo.ui.post_detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.mgt.zalo.R
import com.mgt.zalo.base.BaseListAdapter
import com.mgt.zalo.base.BaseViewHolder
import com.mgt.zalo.data_model.media.ImageMedia
import com.mgt.zalo.data_model.media.Media
import com.mgt.zalo.data_model.media.VideoMedia
import com.mgt.zalo.util.diff_callback.MediaDiffCallback
import com.mgt.zalo.util.smartLoad
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.item_post_media.view.*
import kotlinx.android.synthetic.main.part_post_actions.view.*
import javax.inject.Inject


class PostMediaAdapter @Inject constructor(
        private val postDetailFragment: PostDetailFragment,
        diffCallback: MediaDiffCallback
) : BaseListAdapter<Media, PostMediaAdapter.MediaViewHolder>(diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val holder = MediaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_post_media, parent, false))
        return holder.apply { bindOnClick() }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class MediaViewHolder(itemView: View) : BaseViewHolder(itemView) {
        override fun bind(position: Int) {
            val media = currentList[position]

            bindMetrics(media)
            bindRatio(media)
            when (media) {
                is ImageMedia -> bindImage(media)
                is VideoMedia -> bindVideo(media)
            }
        }

        private fun bindRatio(media: Media){
            itemView.imageView.layoutParams = itemView.imageView.layoutParams.apply {
                this as ConstraintLayout.LayoutParams
                dimensionRatio = utils.getAdjustedRatio(media.ratio?:"1:1")
            }
        }

        private fun bindMetrics(media: Media) {
            itemView.apply {
                shareCountTextView.text = utils.getMetricFormat(media.shareCount)
                reactCountTextView.text = utils.getMetricFormat(media.reactCount)
                commentCountTextView.text = utils.getMetricFormat(media.commentCount)
            }
        }

        private fun bindImage(media: ImageMedia) {
            itemView.apply {
                Picasso.get().smartLoad(media.uri!!, resourceManager, imageView) {
                    it.fit().centerCrop()
                }

                playImgView.visibility = View.GONE
                videoDurationTV.visibility = View.GONE
            }
        }

        private fun bindVideo(media: VideoMedia) {
            itemView.apply {
                resourceManager.getVideoThumbUri(media.uri!!) { uri ->
                    Picasso.get().smartLoad(uri, resourceManager, imageView) {
                        it.fit().centerCrop()
                    }
                }

                playImgView.visibility = View.VISIBLE
                videoDurationTV.text = utils.getVideoDurationFormat(media.duration)
                videoDurationTV.visibility = View.VISIBLE
            }
        }

        fun bindOnClick() {
            itemView.apply {
                imageView.setOnClickListener(postDetailFragment)
                reactImgView.setOnClickListener(postDetailFragment)
                commentImgView.setOnClickListener(postDetailFragment)
                shareImgView.setOnClickListener(postDetailFragment)
            }
        }
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        holder.itemView.apply {
            imageView.setImageDrawable(null)
        }
    }
}