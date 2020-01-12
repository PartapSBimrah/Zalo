package vng.zalo.tdtai.zalo.models.message

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import vng.zalo.tdtai.zalo.R
import java.util.*

data class VideoMessage(
        override var id: String? = null,
        override var createdTime: Timestamp? = null,
        override var senderPhone: String? = null,
        override var senderAvatarUrl: String? = null,
        override var type: Int? = null,
        override var url: String = "unknown",
        override var uploadProgress: Int? = null,
        override var size:Long=-1,
        var ratio: String? = null,
        var duration: Int = 0//secs
) : ResourceMessage(id, createdTime, senderPhone, senderAvatarUrl, type, url, uploadProgress, size) {
    override fun toMap(): HashMap<String, Any> {
        return super.toMap().apply {
            ratio?.let { put(FIELD_RATIO, it) }
            put(FIELD_DURATION, duration)
        }
    }

    override fun applyDoc(doc: DocumentSnapshot) {
        super.applyDoc(doc)
        doc.getString(FIELD_RATIO)?.let { ratio = it }
        doc.getLong(FIELD_DURATION)?.let { duration = it.toInt() }
    }

    override fun getPreviewContent(context: Context): String {
        return "[${context.getString(R.string.description_video)}]"
    }

    companion object {
        const val FIELD_RATIO = "ratio"
        const val FIELD_DURATION = "duration"
    }
}