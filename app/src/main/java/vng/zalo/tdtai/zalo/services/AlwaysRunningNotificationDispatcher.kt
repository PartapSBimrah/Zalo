package vng.zalo.tdtai.zalo.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.util.set
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import vng.zalo.tdtai.zalo.R
import vng.zalo.tdtai.zalo.ZaloApplication
import vng.zalo.tdtai.zalo.abstracts.ResourceManager
import vng.zalo.tdtai.zalo.models.ChatNotification
import vng.zalo.tdtai.zalo.models.room.Room
import vng.zalo.tdtai.zalo.models.room.RoomItem
import vng.zalo.tdtai.zalo.models.room.RoomItemPeer
import vng.zalo.tdtai.zalo.storage.FirebaseDatabase
import vng.zalo.tdtai.zalo.utils.Constants
import vng.zalo.tdtai.zalo.utils.TAG
import vng.zalo.tdtai.zalo.utils.loadCompat
import vng.zalo.tdtai.zalo.views.activities.RoomActivity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class AlwaysRunningNotificationDispatcher private constructor(private val context: Context) {
    private var lastRoomItemMap: HashMap<String, RoomItem>? = null
    private val listenerRegistrations = ArrayList<ListenerRegistration>()
    private var notificationManager: NotificationManager
    private var doListening = false
    private val notificationMap = SparseArray<ChatNotification>()

    private var listeningThread: Thread? = null

    private fun getNewListeningThreadInstance(): Thread {
        return Thread {
            Log.d(TAG, "thread started")

            startListenForNewMessages()

            val obj = object {}
            while (doListening) {
                keepAlive(context)
                Thread.sleep(3000)

                synchronized(obj) {}
            }

            Log.d(TAG, "thread stopped")

            listeningThread = null
            synchronized(obj) {}
        }
    }

    private fun keepAlive(context: Context) {
        Handler(Looper.getMainLooper()).post {
            val toast = Toast(context)

            toast.view = View(context)
            toast.view.visibility = View.GONE
            toast.duration = Toast.LENGTH_LONG
//            val toast = Toast.makeText(context, "${Random().nextInt()}", Toast.LENGTH_LONG)

            toast.show()
        }
    }

    init {
        initNotificationChannels()

        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun onLogin() {
        Log.d(TAG, "onLogin")

        if (listeningThread != null) {
            Toast.makeText(context, "Already started", Toast.LENGTH_SHORT).show()
            return
        }

        doListening = true

        listeningThread = getNewListeningThreadInstance()
        listeningThread!!.start()
        Toast.makeText(context, "Started", Toast.LENGTH_SHORT).show()
    }

    fun onLogout() {
        Log.d(TAG, "onLogout")

        listenerRegistrations.forEach { it.remove() }

        if(!doListening){
            Toast.makeText(context, "Already stopped", Toast.LENGTH_SHORT).show()
            return
        }
        doListening = false
        val obj = object {}
        synchronized(obj) {}
        Toast.makeText(context, "Stopped", Toast.LENGTH_SHORT).show()
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            initChatNotificationChannel()
        }
    }

    private fun startListenForNewMessages() {
        val userRoomsListener = FirebaseDatabase.addUserRoomsListener(
                userPhone = ZaloApplication.curUser!!.phone!!,
                fieldToOrder = RoomItem.FIELD_LAST_MSG_TIME,
                orderDirection = Query.Direction.DESCENDING
        ) { roomItems ->
            //            val pushedNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                notificationManager.activeNotifications.toList()
//            } else {
//                ArrayList()
//            }

            lastRoomItemMap?.let { lastRoomItemMap ->
                roomItems.filter {
                    val lastRoomItem = lastRoomItemMap[it.roomId]

                    lastRoomItem != null &&
                            lastRoomItem.unseenMsgNum != it.unseenMsgNum &&
                            it.unseenMsgNum > 0 &&
                            it.roomId != ZaloApplication.currentRoomId
                }.forEach {
                    pushChatNotification(it)
                }
            }

            val map = HashMap<String, RoomItem>()
            roomItems.forEach {
                map[it.roomId!!] = it
            }
            lastRoomItemMap = map
        }
        listenerRegistrations.add(userRoomsListener)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun initChatNotificationChannel() {
        val chan = NotificationChannel(
                Constants.CHAT_NOTIFY_CHANNEL_ID,
                Constants.CHAT_NOTIFY_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        )
        chan.lightColor = context.getColor(R.color.lightPrimary)
        chan.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        val service = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
    }

    private fun pushChatNotification(roomItem: RoomItem) {
        val notificationId = getNotificationId(roomItem.roomId!!)

        var notification = notificationMap[notificationId]

        var isPrepareDone = true

        if (notification == null) {
            notification = ChatNotification(notificationId)
            notificationMap[notificationId] = notification

            isPrepareDone = false
        }

        if (notification.bitmapMap[roomItem.roomId!!] == null) {
            isPrepareDone = false

            if (notification.roomAvatarTarget == null) {
                notification.roomAvatarTarget = RoomAvatarTarget(roomItem)

                Picasso.get().loadCompat(roomItem.avatarUrl).placeholder(
                        if (roomItem.roomType == Room.TYPE_PEER)
                            R.drawable.default_peer_avatar
                        else
                            R.drawable.default_group_avatar
                ).into(notification.roomAvatarTarget!!)
            }
        }

        if (notification.bitmapMap[roomItem.lastSenderPhone] == null) {
            isPrepareDone = false

            if (notification.senderAvatarTargets[roomItem.lastSenderPhone] == null) {
                notification.senderAvatarTargets[roomItem.lastSenderPhone!!] = SenderAvatarTarget(roomItem)

                FirebaseDatabase.getUserAvatarUrl(roomItem.lastSenderPhone!!) { avatarUrl ->
                    Picasso.get().loadCompat(avatarUrl)
                            .placeholder(R.drawable.default_peer_avatar)
                            .into(notification.senderAvatarTargets[roomItem.lastSenderPhone!!]!!)
                }
            }
        }

        if (isPrepareDone) {
            pushChatNotificationInternal(roomItem)
        }
    }

    private fun pushChatNotificationInternal(roomItem: RoomItem) {
        val notificationId = getNotificationId(roomItem.roomId!!)
        val chatNotification = notificationMap[notificationId]

        // create the pending intent and add to the notification
        val notificationIntent = Intent(context, RoomActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            putExtra(Constants.ROOM_NAME, roomItem.name)
            putExtra(Constants.ROOM_AVATAR, roomItem.avatarUrl)
            putExtra(Constants.ROOM_ID, roomItem.roomId)

            if(roomItem is RoomItemPeer){
                putExtra(Constants.ROOM_PHONE, roomItem.phone)
            }
        }
        val notificationPendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val roomPerson = Person.Builder()
                .setKey(roomItem.roomId)
                .setName(roomItem.getDisplayName())
                .setIcon(IconCompat.createWithBitmap(chatNotification.bitmapMap[roomItem.roomId!!]))
                .build()

        val lastSenderPerson = Person.Builder()
                .setKey(roomItem.lastSenderPhone)
                .setName(ResourceManager.getNameFromPhone(roomItem.lastSenderPhone!!)?:roomItem.lastSenderName)
                .setIcon(IconCompat.createWithBitmap(chatNotification.bitmapMap[roomItem.lastSenderPhone!!]))
                .build()

        if (!chatNotification.containsMessage(roomItem.lastMsgTime!!.toDate().time)) {
            chatNotification.addMessage(ChatNotification.ChatNotificationMessage(
                    createdTime = roomItem.lastMsgTime!!.toDate().time,
                    content = roomItem.lastMsg!!,
                    senderPerson = lastSenderPerson
            ))
        }

        val notificationStyle = NotificationCompat.MessagingStyle(roomPerson)
                .let {
                    if (roomItem.roomType == Room.TYPE_PEER)
                        it.setGroupConversation(false)
                    else
                        it.setGroupConversation(true).setConversationTitle(roomItem.getDisplayName())
                }.also { messagingStyle ->
                    chatNotification.lastMessages.forEach {
                        messagingStyle.addMessage(it.content, it.createdTime, it.senderPerson)
                    }
                }

        val replyIntent = notificationIntent.apply { putExtra(Constants.SHOW_KEYBOARD, true) }

        val notification = NotificationCompat.Builder(context, Constants.CHAT_NOTIFY_CHANNEL_ID)
                .setColor(ContextCompat.getColor(context, R.color.lightPrimary))
                .setContentIntent(notificationPendingIntent)
                .setSmallIcon(R.drawable.app_icon_transparent)
//                .setLargeIcon(roomAvatarBitmap)
                .setStyle(notificationStyle)
                .addAction(R.drawable.like, context.getString(R.string.label_like), null)
                .addAction(R.drawable.reply, context.getString(R.string.label_reply),
                        PendingIntent.getActivity(context, getNotificationActionRequestCode(notificationId), replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                )
                .setAutoCancel(true)
                .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    inner class RoomAvatarTarget(private val roomItem: RoomItem) : Target {
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
//            placeHolderDrawable?.let {
//                val notification = notificationMap[getNotificationId(roomItem.roomId!!)]
//                notification.roomAvatarBitmap = it.toBitmap()
//
//                if(notification.lastSenderAvatarBitmap!=null){
//                    pushChatNotification(roomItem)
//
//                    notification.lastSenderAvatarBitmap = null
//                    notification.roomAvatarBitmap = null
//                }
//            }
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            errorDrawable?.let {
                checkPushChatNotification(it.toBitmap())
            }
        }

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            checkPushChatNotification(bitmap)
        }

        private fun checkPushChatNotification(bitmap: Bitmap) {
            val notification = notificationMap[getNotificationId(roomItem.roomId!!)]
            notification.bitmapMap[roomItem.roomId!!] = bitmap

            if (notification.bitmapMap[roomItem.lastSenderPhone] != null) {
                pushChatNotificationInternal(roomItem)
            }

            notification.roomAvatarTarget = null
        }
    }

    inner class SenderAvatarTarget(private val roomItem: RoomItem) : Target {
        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
//            placeHolderDrawable?.let {
//                val notification = notificationMap[getNotificationId(roomItem.roomId!!)]
//                notification.lastSenderAvatarBitmap = placeHolderDrawable.toBitmap()
//
//                if(notification.roomAvatarBitmap!=null){
//                    pushChatNotification(roomItem)
//
//                    notification.lastSenderAvatarBitmap = null
//                    notification.roomAvatarBitmap = null
//                }
//            }
        }

        override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
            errorDrawable?.let {
                checkPushChatNotification(it.toBitmap())
            }
        }

        override fun onBitmapLoaded(bitmap: Bitmap, from: Picasso.LoadedFrom?) {
            checkPushChatNotification(bitmap)
        }

        private fun checkPushChatNotification(bitmap: Bitmap) {
            val notification = notificationMap[getNotificationId(roomItem.roomId!!)]
            notification.bitmapMap[roomItem.lastSenderPhone!!] = bitmap

            if (notification.bitmapMap[roomItem.roomId!!] != null) {
                pushChatNotificationInternal(roomItem)
            }

            notification.senderAvatarTargets.remove(roomItem.lastSenderPhone!!)
        }
    }

    companion object {
        fun getInstance(context: Context): AlwaysRunningNotificationDispatcher {
            return AlwaysRunningNotificationDispatcher(context)
        }

        fun getNotificationId(roomId: String): Int {
            return roomId.hashCode()
        }

        fun getNotificationActionRequestCode(notificationId: Int): Int {
            return notificationId.shl(Constants.NOTIFICATION_REPLY_PENDING_INTENT)
        }
    }
}