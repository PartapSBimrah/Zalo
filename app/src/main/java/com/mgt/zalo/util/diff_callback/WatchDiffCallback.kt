package com.mgt.zalo.util.diff_callback

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.mgt.zalo.data_model.post.Watch
import javax.inject.Inject

class WatchDiffCallback @Inject constructor() : DiffUtil.ItemCallback<Watch>() {

    override fun areItemsTheSame(oldItem: Watch, newItem: Watch): Boolean {
        return oldItem.id == newItem.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Watch, newItem: Watch): Boolean {
        return oldItem == newItem
    }

//    override fun getChangePayload(oldItem: Watch, newItem: Watch): Any? {
//        val res = ArrayList<Int?>()
//        if(oldItem is SeenMessage){
//            if (oldItem.seenMembers != (newItem as SeenMessage).seenMembers) {
//                res.add(Message.PAYLOAD_SEEN)
//            }
//        }else{
//            if (oldItem.senderAvatarUrl != newItem.senderAvatarUrl) {
//                res.add(Message.PAYLOAD_AVATAR)
//            }
//            if (oldItem is ResourceMessage && oldItem.uploadProgress != (newItem as ResourceMessage).uploadProgress) {
//                res.add(Message.PAYLOAD_UPLOAD_PROGRESS)
//            }
//            if (oldItem.isSent != newItem.isSent) {
//                res.add(Message.PAYLOAD_SEND_STATUS)
//            }
//        }
//        return res
//    }
}