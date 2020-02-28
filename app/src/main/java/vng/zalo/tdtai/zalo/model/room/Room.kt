package vng.zalo.tdtai.zalo.model.room

import com.google.firebase.firestore.DocumentSnapshot
import vng.zalo.tdtai.zalo.managers.ResourceManager
import vng.zalo.tdtai.zalo.model.RoomMember

abstract class Room(
        open var id: String? = null,
        open var avatarUrl: String? = null,
        open var createdTime: Long? = null,
        open var name: String? = null,
        open var memberMap: Map<String, RoomMember>? = null,
        open var type: Int = TYPE_PEER) {
    open fun toMap(): HashMap<String, Any> {
        return HashMap<String, Any>().apply {
            createdTime?.let { put(FIELD_CREATED_TIME, it) }
            put(FIELD_TYPE, type)
        }
    }

    open fun applyDoc(doc: DocumentSnapshot) {
        id = doc.id
        doc.getLong(FIELD_CREATED_TIME)?.let { createdTime = it }
        doc.getLong(FIELD_TYPE)?.let { type = it.toInt() }
    }

    fun getDisplayName(resourceManager: ResourceManager): String {
        return if (this is RoomPeer) {
            resourceManager.getNameFromPhone(this.phone!!) ?: name!!
        } else {
            name!!
        }
    }

    companion object {
        const val FIELD_CREATED_TIME = "createdTime"
        const val FIELD_TYPE = "type"
        const val FIELD_TYPING_MEMBERS = "typingMembers"
        const val FIELD_SEEN_MEMBERS_PHONE = "seenMembersPhone"

        const val TYPE_PEER = 0
        const val TYPE_GROUP = 1
    }
}