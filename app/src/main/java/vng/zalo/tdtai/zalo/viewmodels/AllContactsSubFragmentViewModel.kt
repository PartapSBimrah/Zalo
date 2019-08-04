package vng.zalo.tdtai.zalo.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import vng.zalo.tdtai.zalo.ZaloApplication
import vng.zalo.tdtai.zalo.models.RoomItem
import vng.zalo.tdtai.zalo.networks.Database
import vng.zalo.tdtai.zalo.utils.Constants
import java.util.*

class AllContactsSubFragmentViewModel : ViewModel() {

    val liveRoomItems: MutableLiveData<List<RoomItem>> = MutableLiveData(ArrayList())

    init {
        Database.getUserRooms(
                userPhone = ZaloApplication.currentUser!!.phone!!,
                roomType = Constants.ROOM_TYPE_PEER,
                fieldToOrder = "name"
        ) { liveRoomItems.value = it }
    }
}