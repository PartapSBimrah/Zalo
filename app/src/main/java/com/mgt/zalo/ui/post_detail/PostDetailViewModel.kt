package com.mgt.zalo.ui.post_detail

//class PostDetailViewModel @Inject constructor(private val userId: String) : BaseViewModel() {
//    val liveIsAnyStory: MutableLiveData<Boolean> = MutableLiveData(false)
//
//    val liveUser = MutableLiveData<User>()
//    val liveDiaries = MutableLiveData<ArrayList<Diary>>(ArrayList())
//    val liveStoryGroups: MutableLiveData<List<StoryGroup>> = MutableLiveData(ArrayList())
//
//    val liveSelectedDiary = MutableLiveData<Post>()
//
//    init {
//        refreshProfile()
//    }
//
//    private fun updateLiveDiaries(diaries: ArrayList<Diary> = liveDiaries.value!!, callback: (() -> Unit)?=null) {
//        if (liveUser.value != null && diaries.isNotEmpty()) {
//            diaries.forEach { diary ->
//                diary.ownerName = liveUser.value!!.name
//                diary.ownerAvatarUrl = liveUser.value!!.avatarUrl
//            }
//        }
//        liveDiaries.value = diaries
//        callback?.invoke()
//    }
//
//    fun listenForNewStory() {
//        listenerRegistrations.forEach { it.remove() }
//        listenerRegistrations.clear()
//        listenerRegistrations.add(
//                database.addUserLastStoryCreatedTimeListener(userId) {
//                    liveIsAnyStory.value = it != null && it > System.currentTimeMillis() - Constants.DEFAULT_STORY_LIVE_TIME
//                }
//        )
//    }
//
//    fun refreshProfile(callback:(()->Unit)?=null){
//        database.getUser(userId) { user ->
//            liveUser.value = user!!
//
//            listenerRegistrations.add(
//                    database.addUserStoryGroupsListener(user) { storyGroups ->
//                        liveStoryGroups.value = storyGroups
//                    }
//            )
//
//            updateLiveDiaries(callback = callback)
//        }
//
//        database.getUserRecentDiaries(userId, System.currentTimeMillis(), 15) { diaries ->
//            updateLiveDiaries(diaries)
//        }
//    }
//}