package com.mgt.zalo.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.mgt.zalo.R
import com.mgt.zalo.base.BaseListAdapter
import com.mgt.zalo.base.BaseViewHolder
import com.mgt.zalo.data_model.message.*
import com.mgt.zalo.util.diff_callback.MessageDiffCallback
import com.mgt.zalo.util.diff_callback.RoomMemberDiffCallback
import com.mgt.zalo.util.smartLoad
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_message_receive.view.*
import kotlinx.android.synthetic.main.item_message_seen.view.*
import kotlinx.android.synthetic.main.item_message_send.view.*
import kotlinx.android.synthetic.main.part_message_call.view.*
import kotlinx.android.synthetic.main.part_message_date.view.*
import kotlinx.android.synthetic.main.part_message_file.view.*
import kotlinx.android.synthetic.main.part_message_image.view.*
import kotlinx.android.synthetic.main.part_message_padding.view.*
import kotlinx.android.synthetic.main.part_message_sticker.view.*
import kotlinx.android.synthetic.main.part_message_text.view.*
import kotlinx.android.synthetic.main.part_message_time.view.*
import kotlinx.android.synthetic.main.part_message_video.view.*
import java.util.*
import javax.inject.Inject


class ChatAdapter @Inject constructor(
        private val chatActivity: ChatActivity,
        diffCallback: MessageDiffCallback
) : BaseListAdapter<Message, BaseViewHolder>(diffCallback) {
    private val roomEnterTime = System.currentTimeMillis()

    override fun getItemViewType(position: Int): Int {
        return when {
            currentList[position].type == Message.TYPE_SEEN -> VIEW_TYPE_SEEN
            currentList[position].senderId == sessionManager.curUser!!.id -> VIEW_TYPE_SEND
            else -> VIEW_TYPE_RECEIVE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val holder = when (viewType) {
            VIEW_TYPE_SEND -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_send, parent, false)
                SendViewHolder(v)
            }
            VIEW_TYPE_RECEIVE -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_receive, parent, false)
                RecvViewHolder(v)
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_message_seen, parent, false)
                SeenViewHolder(v)
            }
        }

        return holder.apply {
            if (this is MessageViewHolder) {
                itemView.apply {
                    imageView.setOnClickListener(chatActivity)
                    downloadFileImgView.setOnClickListener(chatActivity)
                    callbackTV.setOnClickListener(chatActivity)
                    videoMessageLayout.setOnClickListener(chatActivity)

                    setOnLongClickListener(chatActivity)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onBindViewHolder(
            holder: BaseViewHolder,
            position: Int,
            payloads: ArrayList<*>
    ) {
        val curMessage = currentList[position]
        val nextMessage = getNextRealMessage(position)
        payloads.forEach {
            when (it) {
                Message.PAYLOAD_SEEN -> (holder as SeenViewHolder).bindSeenMembers(curMessage as SeenMessage)
                else -> {
                    holder as MessageViewHolder
                    when (it) {
                        Message.PAYLOAD_TIME -> holder.bindTime(curMessage, nextMessage)
                        Message.PAYLOAD_AVATAR -> {
                            when (holder.itemViewType) {
                                VIEW_TYPE_RECEIVE -> (holder as RecvViewHolder).apply {
                                    bindAvatar(curMessage, nextMessage, shouldBindTime(curMessage, nextMessage))
                                }
                            }
                        }
                        Message.PAYLOAD_UPLOAD_PROGRESS -> (holder as SendViewHolder).bindUploadProgress(curMessage as MediaMessage)
                        Message.PAYLOAD_SEND_STATUS -> (holder as SendViewHolder).bindSendStatus(curMessage, nextMessage)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        if (holder is MessageViewHolder) {
            holder.apply {
                itemView.apply {
                    textMessageLayout.visibility = View.GONE
                    timeLayout.visibility = View.GONE
                    sentImgView.visibility = View.GONE
                    dateTextView.visibility = View.GONE
                    fileMessageLayout.visibility = View.GONE
                    callMessageLayout.visibility = View.GONE
                    paddingView.visibility = View.GONE

                    videoMessageLayout.visibility = View.GONE
                    videoThumbImgView.setImageDrawable(null)
                    stickerAnimView.visibility = View.GONE
                    (stickerAnimView.drawable as LottieDrawable?)?.clearComposition()

                    imageView.visibility = View.GONE
                    imageView.setImageDrawable(null)

                    if (holder is SendViewHolder) {
                        uploadImageProgressBar?.visibility = View.GONE
                        uploadVideoProgressBar?.visibility = View.GONE
                        uploadFileProgressBar?.visibility = View.GONE
                    } else {
                        avatarLayout?.visibility = View.GONE
                        watchOwnerAvatarImgView?.setImageDrawable(null)
                        typingMessageLayout?.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun getPreviousRealMessage(position: Int): Message? {
        var i = position + 1
        while (i <= currentList.lastIndex) {
            if (currentList[i].type != Message.TYPE_TYPING && currentList[i].type != Message.TYPE_SEEN) {
                return currentList[i]
            }
            i++
        }
        return null
    }

    private fun getNextRealMessage(position: Int): Message? {
        var i = position - 1
        while (i >= 0) {
            if (currentList[i].type != Message.TYPE_TYPING && currentList[i].type != Message.TYPE_SEEN) {
                return currentList[i]
            }
            i--
        }
        return null
    }

    // view holder classes

    abstract inner class MessageViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bindTime(curMessage: Message, nextMessage: Message?): Boolean {
            /* if one of these is true:
            - current curMessage is last curMessage
            - next curMessage time - current curMessage time < 1 min
            - next curMessage date != current curMessage date
            => display curMessage time
            */
            if (shouldBindTime(curMessage, nextMessage)) {
                itemView.apply {
                    timeTextView.text = utils.getTimeFormat(curMessage.createdTime!!)
                    timeLayout.visibility = View.VISIBLE
                }
//                itemView as ConstraintLayout
//
//                val timeLayout = View.inflate(itemView.context, R.layout.part_message_time, null)
//
//                timeLayout.timeTextView.text = Utils.getTimeFormat(curMessage.createdTime!!.toDate())
//                itemView.addView(timeLayout, 0)
//
//                ConstraintSet().apply {
//                    clone(itemView)
//                    connect(itemView.imageView.id, ConstraintSet.BOTTOM, timeLayout.id, ConstraintSet.TOP, 4)
//                    connect(timeLayout.id, ConstraintSet.TOP, itemView.imageView.id, ConstraintSet.BOTTOM, 4)
//                    connect(timeLayout.id, ConstraintSet.BOTTOM, itemView.id, ConstraintSet.BOTTOM, 16)
//                    connect(timeLayout.id, ConstraintSet.END, itemView.id, ConstraintSet.END, 24)
//                    applyTo(itemView)
//                }

                return true
            } else if (itemView.timeLayout.visibility != View.GONE) {
                itemView.timeLayout.visibility = View.GONE
            }
            return false
        }

        fun bindDate(curMessage: Message, prevMessage: Message?) {
            /* if one of these is true:
                - current message is first message
                - previous message date != current message date
                => display date
                */
            if (prevMessage == null || utils.areInDifferentDay(curMessage.createdTime!!, prevMessage.createdTime!!)) {
                itemView.apply {
                    dateTextView.text = utils.getDateFormat(curMessage.createdTime!!)
                    dateTextView.visibility = View.VISIBLE
                }
            }
        }

        fun bindSticker(stickerMessage: StickerMessage) {
            itemView.apply {
                LottieCompositionFactory.fromUrl(context, stickerMessage.url).addListener {
                    stickerAnimView.setComposition(it)
                    if (stickerAnimView.isAttachedToWindow && (context as ChatActivity).chatRecyclerView.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        stickerAnimView.resumeAnimation()
                    }
                }
                stickerAnimView.visibility = View.VISIBLE
            }
        }

        fun bindText(textMessage: TextMessage) {
            itemView.apply {
                bindBackgroundColor(textMessageTV)

                textMessageTV.text = textMessage.content
//            textMessageTV.movementMethod = LinkMovementMethod.getInstance()

                textMessageLayout.visibility = View.VISIBLE
            }
        }

        private fun bindBackgroundColor(view: View) {
            if (itemViewType == VIEW_TYPE_SEND) {
                view.setBackgroundResource(R.color.sendMessageBackground)
            } else {
                view.setBackgroundResource(R.color.recvMessageBackground)
            }
        }

        fun shouldBindTime(curMessage: Message, nextMessage: Message?): Boolean {
            return nextMessage == null ||
                    utils.areInDifferentDay(curMessage.createdTime!!, nextMessage.createdTime!!) ||
                    utils.areInDifferentMin(curMessage.createdTime!!, nextMessage.createdTime!!)
        }

        fun bindImage(imageMessage: ImageMessage) {
            itemView.apply {
                setViewConstrainRatio(imageView, imageMessage.ratio!!)

                imageView.visibility = View.VISIBLE

                Picasso.get().smartLoad(imageMessage.url, resourceManager, imageView) {
                    it.fit().error(R.drawable.load_image_fail)
                }
            }
        }

        fun bindFile(fileMessage: FileMessage) {
            itemView.apply {
                bindBackgroundColor(fileMessageLayout2)

                fileMessageLayout.visibility = View.VISIBLE

                fileNameTextView.text = fileMessage.fileName
                        ?: context.getString(R.string.label_no_name)

                val extension = utils.getFileExtension(fileMessage.fileName).toUpperCase(Locale.US)
                fileExtensionImgView.setImageResource(utils.getResIdFromFileExtension(extension))

                val fileSizeFormat = if (fileMessage.size != -1L) utils.getFormatFileSize(fileMessage.size) else ""

                fileDescTextView.text =
                        if (extension != "" && fileSizeFormat != "") {
                            String.format("%s - %s", extension, fileSizeFormat)
                        } else if (extension != "") {
                            extension
                        } else {
                            fileSizeFormat
                        }
            }
        }

        fun bindCall(callMessage: CallMessage) {
            itemView.apply {
                bindBackgroundColor(callMessageLayout2)

                val context = itemView.context

                callMessageLayout.visibility = View.VISIBLE

                if (callMessage.isMissed) {
                    callTitleTV.setTextColor(ContextCompat.getColor(context, R.color.missedCall))
                    callTitleTV.text = context.getString(R.string.description_missed_call)

                    callTimeTV.text = context.getString(
                            if (callMessage.callType == CallMessage.CALL_TYPE_VOICE)
                                R.string.description_voice_call
                            else
                                R.string.description_video_call
                    )

                    callIconImgView.setImageResource(R.drawable.missed_call)
                } else {
                    callTimeTV.text = utils.getCallTimeFormat(callMessage.callTime)

                    if (callMessage.senderId == sessionManager.curUser!!.id) {
                        callTitleTV.text = context.getString(
                                if (callMessage.callType == CallMessage.CALL_TYPE_VOICE) {
                                    R.string.description_outgoing_voice_call
                                } else {
                                    R.string.description_outgoing_video_call
                                }
                        )

                        callIconImgView.setImageResource(
                                if (callMessage.isCanceled) {
                                    R.drawable.canceled_outgoing_call
                                } else {
                                    R.drawable.success_outgoing_call
                                }
                        )
                    } else {
                        callTitleTV.text = context.getString(
                                if (callMessage.callType == CallMessage.CALL_TYPE_VOICE) {
                                    R.string.description_incoming_voice_call
                                } else {
                                    R.string.description_incoming_video_call
                                }
                        )

                        callIconImgView.setImageResource(
                                if (callMessage.isCanceled) {
                                    R.drawable.canceled_incoming_call
                                } else {
                                    R.drawable.success_incoming_call
                                }
                        )
                    }
                }
            }
        }

        fun bindPadding(curMessage: Message, prevMessage: Message?) {
            itemView.apply {
                if (prevMessage != null && prevMessage.senderId != curMessage.senderId) {
                    paddingView.visibility = View.VISIBLE
                } else {
                    paddingView.visibility = View.GONE
                }
            }
        }

        fun bindVideo(videoMessage: VideoMessage) {
            itemView.apply {
                setViewConstrainRatio(videoMessageLayout, videoMessage.ratio!!)

                resourceManager.getVideoThumbUri(videoMessage.url) { uri ->
                    imageLoader.load(uri, videoThumbImgView) {
                        it.fit()
                    }
                }

                videoMessageLayout.visibility = View.VISIBLE

                videoDurationTV.text = utils.getVideoDurationFormat(videoMessage.duration)
            }
        }

        private fun setViewConstrainRatio(view: View, ratio: String) {
            val size = utils.getSize(ratio)

            val constraintLayout = view.parent as ConstraintLayout

            val set = ConstraintSet()
            set.clone(constraintLayout)
            set.constrainMaxWidth(view.id, size.width)
            set.constrainMaxHeight(view.id, size.height)
            set.setDimensionRatio(view.id, "H,$ratio")
            set.applyTo(constraintLayout)
        }
    }

    inner class SendViewHolder(itemView: View) : MessageViewHolder(itemView) {
//        //send status
//        val sentImgView: ImageView = itemView.findViewById(R.id.sentImgView)
//        //avatar layout
//        val avatarLayout: CardView? = itemView.findViewById(R.id.avatarLayout)
//        val avatarImgView: ImageView? = itemView.findViewById(R.id.avatarImgView)
//
//        val typingMessageLayout: CardView? = itemView.findViewById(R.id.typingMessageLayout)

        override fun bind(position: Int) {
            val curMessage = currentList[position]
            val nextMessage = getNextRealMessage(position)
            val prevMessage = getPreviousRealMessage(position)

            when (curMessage.type) {
                Message.TYPE_STICKER -> bindSticker(curMessage as StickerMessage)
                Message.TYPE_IMAGE -> bindImage(curMessage as ImageMessage)
                Message.TYPE_FILE -> bindFile(curMessage as FileMessage)
                Message.TYPE_CALL -> bindCall(curMessage as CallMessage)
                Message.TYPE_VIDEO -> bindVideo(curMessage as VideoMessage)
                else -> bindText(curMessage as TextMessage)
            }

            bindDate(curMessage, prevMessage)
            bindTime(curMessage, nextMessage)
            bindPadding(curMessage, prevMessage)
            if (curMessage is MediaMessage) {
                bindUploadProgress(curMessage)
            }

            //this is newest real message
            if (curMessage.createdTime!! > roomEnterTime) {
                bindSendStatus(curMessage, nextMessage)
            }
        }

        fun bindUploadProgress(mediaMessage: MediaMessage) {
            itemView.apply {
                val uploadProgressBar = when (mediaMessage.type) {
                    Message.TYPE_IMAGE -> uploadImageProgressBar
                    Message.TYPE_VIDEO -> uploadVideoProgressBar
                    else -> uploadFileProgressBar
                }
                if (mediaMessage.uploadProgress == null) {
                    uploadProgressBar!!.visibility = View.GONE
                    if (mediaMessage.type == Message.TYPE_FILE) {
                        itemView.downloadFileImgView.visibility = View.VISIBLE
                    }
                } else {
                    uploadProgressBar!!.progress = mediaMessage.uploadProgress!!
                    uploadProgressBar.visibility = View.VISIBLE
                    if (mediaMessage.type == Message.TYPE_FILE) {
                        itemView.downloadFileImgView.visibility = View.GONE
                    }
                }
            }
        }

        fun bindSendStatus(curMessage: Message, nextMessage: Message?) {
            if (shouldBindTime(curMessage, nextMessage)) {
                itemView.sentImgView.visibility = if (curMessage.isSent) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    inner class RecvViewHolder(itemView: View) : MessageViewHolder(itemView) {
        override fun bind(position: Int) {
            val curMessage = currentList[position]
            val nextMessage = getNextRealMessage(position)
            val prevMessage = getPreviousRealMessage(position)

            if (curMessage.type == Message.TYPE_TYPING) {
                bindTyping()
                showAvatar(curMessage)
            } else {
                when (curMessage.type) {
                    Message.TYPE_STICKER -> bindSticker(curMessage as StickerMessage)
                    Message.TYPE_IMAGE -> bindImage(curMessage as ImageMessage)
                    Message.TYPE_FILE -> bindFile(curMessage as FileMessage)
                    Message.TYPE_CALL -> bindCall(curMessage as CallMessage)
                    Message.TYPE_VIDEO -> bindVideo(curMessage as VideoMessage)
                    else -> bindText(curMessage as TextMessage)
                }

                bindDate(curMessage, prevMessage)
                bindPadding(curMessage, prevMessage)

                val isTimeBind = bindTime(curMessage, nextMessage)
                bindAvatar(curMessage, nextMessage, isTimeBind)
            }
        }

        private fun bindTyping() {
            itemView.typingMessageLayout.visibility = View.VISIBLE
        }

        fun bindAvatar(curMessage: Message, nextMessage: Message?, isTimeBind: Boolean) {
            /* if one of these is true:
            - curMessage time displayed
            - next curMessage sender != current curMessage sender
            => display avatarUrl
            */
            if (isTimeBind || nextMessage!!.senderId != curMessage.senderId) {
                showAvatar(curMessage)
            } else {
                itemView.avatarLayout.visibility = View.GONE
            }
        }

        private fun showAvatar(message: Message) {
            itemView.apply {
                avatarLayout.visibility = View.VISIBLE
                imageLoader.load(message.senderAvatarUrl, watchOwnerAvatarImgView) {
                            it.fit()
                                    .centerCrop()
                                    .placeholder(R.drawable.default_peer_avatar)
                        }
            }
        }
    }

    inner class SeenViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val adapter = RoomMemberAdapter(RoomMemberDiffCallback())

        override fun bind(position: Int) {
            itemView.seenRecyclerView.adapter = adapter

            val message = currentList[position] as SeenMessage

            bindSeenMembers(message)
        }

        fun bindSeenMembers(message: SeenMessage) {
            adapter.submitList(message.seenMembers)
        }
    }

    companion object {
        const val VIEW_TYPE_SEND = 0
        const val VIEW_TYPE_RECEIVE = 1
        const val VIEW_TYPE_SEEN = 2
    }
}