package vng.zalo.tdtai.zalo.repository

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.IntDef
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import vng.zalo.tdtai.zalo.manager.BackgroundWorkManager
import vng.zalo.tdtai.zalo.manager.ResourceManager
import vng.zalo.tdtai.zalo.manager.SessionManager
import vng.zalo.tdtai.zalo.data_model.message.Message
import vng.zalo.tdtai.zalo.data_model.room.Room
import vng.zalo.tdtai.zalo.data_model.story.Story
import vng.zalo.tdtai.zalo.util.Constants
import vng.zalo.tdtai.zalo.util.TAG
import vng.zalo.tdtai.zalo.util.Utils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class Storage @Inject constructor(
        private val resourceManager: ResourceManager,
        private val utils: Utils,
        private val sessionManager: SessionManager,
        private val backgroundWorkManager: BackgroundWorkManager
) {
    private val firebaseStorage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    private fun getReference(path: String): StorageReference {
        return firebaseStorage.reference.child(path)
    }

    fun addFileAndGetDownloadUrl(localPath: String, storagePath: String, @FileType fileType: Int, fileSize: Long = 0, onProgressChange: ((progress: Int) -> Unit)? = null, onComplete: ((url: String) -> Unit)? = null) {
        var resourceSize = fileSize

        backgroundWorkManager.single({
            when (fileType) {
                FILE_TYPE_IMAGE -> {
                    var bitmap = getScaledDownBitmap(
                            resourceManager.getImageBitmap(localPath)
                    )

                    val rotationDegree = resourceManager.getImageRotation(localPath)
                    if (rotationDegree != 0) {
                        bitmap = utils.rotateImage(bitmap, rotationDegree)
                    }

                    //transfer to input stream
                    val bos = ByteArrayOutputStream()
                    bitmap.compress(Constants.IMAGE_COMPRESS_FORMAT, Constants.IMAGE_COMPRESS_QUALITY, bos)
                    val bitmapData: ByteArray = bos.toByteArray()

                    resourceSize = bitmapData.size.toLong()

                    ByteArrayInputStream(bitmapData)
                }
                else -> utils.getInputStream(localPath)
            }
        }, onSuccess = { inputStream ->
            val fileStorageRef = firebaseStorage.reference.child(storagePath)

            fileStorageRef.putStream(inputStream)
                    .addOnProgressListener {
                        if (resourceSize != 0L) {
                            onProgressChange?.invoke((it.bytesTransferred * 100 / resourceSize).toInt())
                        }
                    }.continueWithTask {
                        var isSuccess = false
                        utils.assertTaskSuccess(it, TAG, "addFileAndGetDownloadUrl") {
                            isSuccess = true
                        }
                        if (isSuccess) {
                            getReference(storagePath).downloadUrl
                        } else {
                            null
                        }
                    }.addOnCompleteListener { task ->
                        utils.assertTaskSuccessAndResultNotNull(task, TAG, "addFile") { uri ->
                            onComplete?.invoke(uri.toString())
                        }
                    }
        })
    }

    private fun getScaledDownBitmap(bitmap: Bitmap): Bitmap {
        val scaleRatio = max(min(bitmap.width, bitmap.height) / 1000f, 1f)
        val dstWidth = (bitmap.width / scaleRatio).toInt()
        val dstHeight = (bitmap.height / scaleRatio).toInt()

        return Bitmap.createScaledBitmap(bitmap, dstWidth, dstHeight, false)
    }

    fun getStickerSetUrls(bucketName: String, callback: ((List<String>) -> Unit)? = null) {
        Log.d(TAG, firebaseStorage.reference.child("${FOLDER_STICKER_SETS}/$bucketName/").path)
        firebaseStorage.reference.child("${FOLDER_STICKER_SETS}/$bucketName/").listAll().addOnCompleteListener { task ->
            utils.assertTaskSuccessAndResultNotNull(task, TAG, "getStickerSetUrls.listAll") { listResult ->
                val stickerUrlsTasks = ArrayList<Task<Uri>>()
                listResult.items.forEach {
                    stickerUrlsTasks.add(it.downloadUrl)
                }

                Tasks.whenAll(stickerUrlsTasks).addOnCompleteListener {
                    val stickerUrls = ArrayList<String>()
                    stickerUrlsTasks.forEachIndexed { index, task ->
                        utils.assertTaskSuccessAndResultNotNull(task, TAG, "getStickerSetUrls.task[$index]") { uri ->
                            stickerUrls.add(uri.toString())
                        }
                    }

                    callback?.invoke(stickerUrls)
                }
            }
        }
    }

    fun addStickerSet(name: String, localPaths: List<String>, callback: ((bucketName: String) -> Unit)? = null) {
        /*
         */
        val bucketName = utils.getStickerBucketNameFromName(name)
        callback?.invoke(bucketName)
    }

    fun deleteAttachedDataFromStorage(roomId: String, onSuccess: (() -> Unit)? = null) {
        val fileStoragePath = getRoomDataStoragePath(roomId)

        firebaseStorage.reference.child(fileStoragePath).delete().addOnCompleteListener {
            utils.assertTaskSuccess(it, TAG, "deleteAttachedDataFromStorage") { onSuccess?.invoke() }
        }
    }

//    fun generateNewStoragePath(context: Context, room: Room, localUriString: String, createdTime: Long):String{
//        val extension = if (Utils.isContentUri(localUriString)) {
//            val localUri = Uri.parse(localUriString)
//            MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(localUri))
//        } else {
//            Utils.getFileExtension(localUriString)
//        }
//
//        return getMessageDataStoragePath(room.id!!,createdTime)
//    }

    fun deleteRoomAvatarAndData(room: Room): List<Task<Void>> {
        val tasks = ArrayList<Task<Void>>()
        if (room.type == Room.TYPE_GROUP) {
            val avatarStoragePath = getRoomAvatarStoragePath(room.id!!)
            tasks.add(getReference(avatarStoragePath).delete())
        }

        val dataStoragePath = getRoomDataStoragePath(room.id!!)
        getReference(dataStoragePath).delete()

        return tasks
    }

    fun getRoomAvatarStoragePath(roomId: String): String {
        return "${FOLDER_ROOM_AVATARS}/$roomId"
    }

    private fun getRoomDataStoragePath(roomId: String): String {
        return "${FOLDER_ROOM_DATA}/${roomId}"
    }

    fun getMessageDataStoragePath(roomId: String, message: Message): String {
        return "${getRoomDataStoragePath(roomId)}/file_${message.createdTime!!}"
    }

    fun getStoryStoragePath(story: Story): String {
        return "$FOLDER_STORIES/${sessionManager.curUser!!.id}/${story.createdTime}"
    }

    fun getStoryGroupAvatarStoragePath(storyGroupId: String): String {
        return "$FOLDER_STORY_GROUPS/${sessionManager.curUser!!.id}/${storyGroupId}"
    }

    companion object {
        private const val FOLDER_USER_AVATARS = "user_avatars"
        private const val FOLDER_ROOM_AVATARS = "room_avatars"
        private const val FOLDER_ROOM_DATA = "room_data"
        private const val FOLDER_STICKER_SETS = "sticker_sets"
        private const val FOLDER_STORIES = "stories"
        private const val FOLDER_STORY_GROUPS = "story_groups"

        const val FILE_TYPE_IMAGE = 0
        const val FILE_TYPE_VIDEO = 1
    }

    @IntDef(FILE_TYPE_IMAGE, FILE_TYPE_VIDEO)
    @Retention(AnnotationRetention.SOURCE)
    annotation class FileType
}