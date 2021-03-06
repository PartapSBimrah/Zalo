package com.mgt.zalo.base

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import com.mgt.zalo.ZaloApplication
import com.mgt.zalo.manager.*
import com.mgt.zalo.repository.Database
import com.mgt.zalo.repository.Storage
import com.mgt.zalo.service.NotificationService
import com.mgt.zalo.util.Utils
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

abstract class BaseViewModel: ViewModel() {
    protected var listenerRegistrations = ArrayList<ListenerRegistration>()
    protected var compositeDisposable = CompositeDisposable()

    @Inject lateinit var application: ZaloApplication
    @Inject lateinit var sharedPrefsManager: SharedPrefsManager
    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var externalIntentManager: ExternalIntentManager
    @Inject lateinit var messageManager: MessageManager
    @Inject lateinit var resourceManager: ResourceManager
    @Inject lateinit var permissionManager: PermissionManager
    @Inject lateinit var utils: Utils
    @Inject lateinit var database: Database
    @Inject lateinit var storage: Storage
    @Inject lateinit var callService: CallService
    @Inject lateinit var backgroundWorkManager: BackgroundWorkManager

    init {
        ZaloApplication.appComponent.inject(this)
    }

    override fun onCleared() {
        removeAllListeners()
        compositeDisposable.clear()
    }

    fun removeAllListeners() {
        listenerRegistrations.forEach { it.remove() }
    }
}