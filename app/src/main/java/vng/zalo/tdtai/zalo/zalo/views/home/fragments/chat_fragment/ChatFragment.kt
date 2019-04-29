package vng.zalo.tdtai.zalo.zalo.views.home.fragments.chat_fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_chat.*
import vng.zalo.tdtai.zalo.R
import vng.zalo.tdtai.zalo.zalo.dependency_factories.ViewModelFactory
import vng.zalo.tdtai.zalo.zalo.shared_adapters.RoomItemAdapter
import vng.zalo.tdtai.zalo.zalo.utils.Constants.*
import vng.zalo.tdtai.zalo.zalo.utils.RoomItemDiffCallback
import vng.zalo.tdtai.zalo.zalo.viewmodels.ChatFragmentViewModel
import vng.zalo.tdtai.zalo.zalo.views.home.fragments.chat_fragment.room_activity.RoomActivity

class ChatFragment : Fragment(), View.OnClickListener {
    private lateinit var viewModel: ChatFragmentViewModel
    private lateinit var adapter: RoomItemAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProviders.of(this, ViewModelFactory()).get(ChatFragmentViewModel::class.java)

        adapter = RoomItemAdapter(this, RoomItemDiffCallback())

        with(recyclerView) {
            adapter = this@ChatFragment.adapter
            layoutManager = LinearLayoutManager(activity)
            setHasFixedSize(true)
        }

        viewModel.liveRoomItems.observe(viewLifecycleOwner, Observer { rooms ->
            adapter.submitList(rooms) { Log.d(TAG, viewModel.liveRoomItems.value.toString()) }
            Log.d(TAG, "onChanged livedata")
        })
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.itemRoomRootLayout -> {
                val position = recyclerView.getChildLayoutPosition(v)
                val intent = Intent(activity, RoomActivity::class.java)
                //room not created in database
                if (adapter.currentList[position].roomId != null)
                    intent.putExtra(ROOM_ID, adapter.currentList[position].roomId)

                intent.putExtra(ROOM_NAME, adapter.currentList[position].name)
                intent.putExtra(ROOM_AVATAR, adapter.currentList[position].avatar)
                startActivity(intent)
            }
        }
    }

    companion object {
        private val TAG = ChatFragment::class.java.simpleName
    }
}