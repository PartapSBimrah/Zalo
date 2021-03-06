package com.mgt.zalo.ui.comment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.mgt.zalo.R
import com.mgt.zalo.base.BaseListAdapter
import com.mgt.zalo.base.BaseOnEventListener
import com.mgt.zalo.base.BaseViewHolder
import com.mgt.zalo.data_model.Comment
import com.mgt.zalo.data_model.media.ImageMedia
import com.mgt.zalo.data_model.media.Media
import com.mgt.zalo.data_model.media.VideoMedia
import com.mgt.zalo.data_model.react.React
import com.mgt.zalo.manager.ResourceManager
import com.mgt.zalo.manager.SessionManager
import com.mgt.zalo.util.ImageLoader
import com.mgt.zalo.util.Utils
import com.mgt.zalo.util.diff_callback.CommentDiffCallback
import kotlinx.android.synthetic.main.item_comment.view.*
import javax.inject.Inject
import kotlin.math.min


class CommentAdapter @Inject constructor(
        private val eventListener: BaseOnEventListener,
        diffCallback: CommentDiffCallback
) : BaseListAdapter<Comment, CommentAdapter.CommentViewHolder>(diffCallback) {
    var isReply = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val holder = CommentViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false),
                eventListener, sessionManager, utils, resourceManager, imageLoader
        )
        return holder.apply { bindOnClick() }
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int, payloads: ArrayList<*>) {
        payloads.forEach {
            when (it) {
                Comment.PAYLOAD_METRICS -> holder.bindMetrics(currentList[position])
            }
        }
    }

    class CommentViewHolder(
            itemView: View,
            private val eventListener: BaseOnEventListener,
            private val sessionManager: SessionManager,
            private val utils: Utils,
            private val resourceManager: ResourceManager,
            private val imageLoader: ImageLoader
            ) : BaseViewHolder(itemView) {
        fun bind(comment: Comment) {
            bindOwner(comment)
            bindText(comment)
            bindMedia(comment)
            bindMetrics(comment)
            bindReplies(comment)
        }

        fun bindMetrics(comment: Comment) {
            itemView.apply {
                val react = comment.reacts[sessionManager.curUser!!.id!!]
                if (react != null) {
                    val reactColor = ContextCompat.getColor(context, React.getTextColorResId(react.type))
                    reactTextView.setTextColor(reactColor)

                    val reactText = context.getString(React.getText(react.type))
                    reactTextView.text = reactText
                } else {
                    val blackTextColor = ContextCompat.getColor(context, R.color.blackText)
                    reactTextView.setTextColor(blackTextColor)
                    reactTextView.text = context.getString(R.string.label_love)
                }

                if (comment.reactCount > 0) {
                    val top3reactTypes = comment.reacts.values.asSequence().groupBy { it.type }.toList()
                            .filter { it.second.isNotEmpty() }
                            .sortedByDescending { it.second.size }
                            .take(3)
                            .map { it.first }

                    when (top3reactTypes.size) {
                        1 -> {
                            reactIcon3.visibility = View.VISIBLE
                            reactIcon3.setImageResource(React.getDrawableResId(top3reactTypes[0]))
                            reactIcon2.visibility = View.GONE
                            reactIcon1.visibility = View.GONE
                        }
                        2 -> {
                            reactIcon3.visibility = View.VISIBLE
                            reactIcon3.setImageResource(React.getDrawableResId(top3reactTypes[0]))
                            reactIcon2.visibility = View.VISIBLE
                            reactIcon2.setImageResource(React.getDrawableResId(top3reactTypes[1]))
                            reactIcon1.visibility = View.GONE
                        }
                        else -> {
                            reactIcon3.visibility = View.VISIBLE
                            reactIcon3.setImageResource(React.getDrawableResId(top3reactTypes[0]))
                            reactIcon2.visibility = View.VISIBLE
                            reactIcon2.setImageResource(React.getDrawableResId(top3reactTypes[1]))
                            reactIcon1.visibility = View.VISIBLE
                            reactIcon1.setImageResource(React.getDrawableResId(top3reactTypes[2]))
                        }
                    }

                    reactCountTextView.text = utils.getMetricFormat(comment.reactCount)
                    reactLayout.visibility = View.VISIBLE
                } else {
                    reactLayout.visibility = View.GONE
                }
            }
        }

        fun bindOwner(comment: Comment) {
            itemView.apply {
                imageLoader.load(comment.ownerAvatarUrl, avatarImgView) {
                    it.fit().centerCrop()
                }

                timeTextView.text = utils.getTimeDiffFormat(comment.createdTime!!)
                nameTextView.text = comment.ownerName
            }
        }

        fun bindText(comment: Comment) {
            itemView.apply {
                descTextView.visibility = if (comment.text.isEmpty()) {
                    View.GONE
                } else {
                    descTextView.text = comment.text
                    View.VISIBLE
                }
            }
        }

        fun bindMedia(comment: Comment) {
            itemView.apply {
                if (comment.media != null) {
                    val media = comment.media!!
                    bindRatio(media)

                    when (media) {
                        is ImageMedia -> {
                            imageLoader.load(media.uri, imageView) {
                                it.fit().centerCrop()
                            }
                            playIcon.visibility = View.GONE
                        }
                        is VideoMedia -> {
                            resourceManager.getVideoThumbUri(media.uri!!) { uri ->
                                imageLoader.load(uri, imageView) {
                                    it.fit().centerCrop()
                                }
                            }
                            playIcon.visibility = View.VISIBLE
                        }
                    }
                    imageView.visibility = View.VISIBLE
                } else {
                    imageView.visibility = View.GONE
                }
            }
        }

        private fun bindRatio(media: Media) {
            itemView.apply {
                imageView.layoutParams = imageView.layoutParams.apply {
                    this as ConstraintLayout.LayoutParams
                    val adjustedRatio = utils.getAdjustedRatio(media.ratio!!)
                    val size = utils.getSize(adjustedRatio)
                    matchConstraintMaxWidth = size.width
                    matchConstraintMaxHeight = min(size.height, utils.dpToPx(200))
                    dimensionRatio = "H,$adjustedRatio"
                }
            }
        }

        fun bindReplies(comment: Comment) {
            itemView.apply {
                viewReplyTextView.visibility = if (comment.replyCount == 0) {
                    View.GONE
                } else {
                    viewReplyTextView.text = String.format(context.getString(R.string.description_view_replies), comment.replyCount)
                    View.VISIBLE
                }
            }
        }

        fun bindOnClick() {
            itemView.apply {
                setOnLongClickListener(eventListener)
                avatarImgView.setOnClickListener(eventListener)
                nameTextView.setOnClickListener(eventListener)
                viewReplyTextView.setOnClickListener(eventListener)
                reactTextView.setOnClickListener(eventListener)
                replyTextView.setOnClickListener(eventListener)
                reactLayout.setOnClickListener(eventListener)
                imageView.setOnClickListener(eventListener)
            }
        }
    }
}