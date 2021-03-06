package com.mgt.zalo.ui.home.watch

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.mgt.zalo.R
import com.mgt.zalo.base.BaseListAdapter
import com.mgt.zalo.base.BaseViewHolder
import com.mgt.zalo.data_model.post.Watch
import com.mgt.zalo.util.TAG
import com.mgt.zalo.util.diff_callback.WatchDiffCallback
import kotlinx.android.synthetic.main.item_watch.view.*
import javax.inject.Inject


class WatchAdapter @Inject constructor(
        private val watchFragment: WatchFragment,
        diffCallback: WatchDiffCallback
) : BaseListAdapter<Watch, WatchAdapter.WatchViewHolder>(diffCallback) {
    private val objectAnimator = ObjectAnimator().apply {
        setValues(PropertyValuesHolder.ofFloat("rotation", 0f, 360f))
        duration = 6000
        repeatCount = Animation.INFINITE
        interpolator = LinearInterpolator()
    }

    private var isAnimatorStarted = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchViewHolder {
        return WatchViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_watch, parent, false)).apply {
            bindListeners()
        }
    }

    override fun onBindViewHolder(holder: WatchViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onBindViewHolder(
            holder: WatchViewHolder,
            position: Int,
            payloads: ArrayList<*>
    ) {
        payloads.forEach {
            when (it) {
                Watch.PAYLOAD_FULLSCREEN -> holder.bindFullscreen()
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: WatchViewHolder) {
        Log.d(TAG, "detach")
        onWatchNotFocused(holder.itemView)
    }

    fun onWatchResume(itemView: View) {
        Log.d(TAG, "resume")
        itemView.apply {
            playPauseImgView.visibility = View.GONE
            musicNameTextView.isSelected = true
            if (isAnimatorStarted) {
                objectAnimator.resume()
            } else {
                objectAnimator.start()
                isAnimatorStarted = true
            }
        }
        playbackManager.resume()
    }

    fun onWatchPause(itemView: View, displayPauseIcon: Boolean = true) {
        Log.d(TAG, "pause")
        itemView.apply {
            playbackManager.pause()
            if (displayPauseIcon) {
                playPauseImgView.visibility = View.VISIBLE
            }
            musicNameTextView.isSelected = false
            objectAnimator.pause()
        }
    }

    private val onLostFocus = {
        watchFragment.getCurrentItemView()?.let{onWatchNotFocused(it)}
    }

    fun onWatchFocused(itemView: View) {
        Log.d(TAG, "focus")
        itemView.apply {
            objectAnimator.target = musicOwnerAvatarImgView
            objectAnimator.start()

            playbackManager.prepare(
                    currentList[watchFragment.getCurrentItem()].videoMedia!!.uri!!,
                    true,
                    onReady = {
                        previewImgView.visibility = View.GONE
                        onWatchResume(this)
                    }, onLostFocus = onLostFocus)
            playerView.player = playbackManager.exoPlayer
        }

        watchFragment.isPaused = false
    }

    fun onWatchNotFocused(itemView: View) {
        Log.d(TAG, "lost focus")
        itemView.apply {
            playerView.player = null

            previewImgView.visibility = View.VISIBLE
            playPauseImgView.visibility = View.GONE
            musicNameTextView.isSelected = false
            musicOwnerAvatarImgView.rotation = 0f
        }
    }

    inner class WatchViewHolder(itemView: View) : BaseViewHolder(itemView) {
        fun bindListeners() {
            itemView.apply {
                musicIcon.setOnClickListener(watchFragment)
                musicOwnerAvatarImgView.setOnClickListener(watchFragment)
                musicNameTextView.setOnClickListener(watchFragment)
                shareImgView.setOnClickListener(watchFragment)
                commentImgView.setOnClickListener(watchFragment)
                reactImgView.setOnClickListener(watchFragment)
                watchOwnerAvatarImgView.setOnClickListener(watchFragment)
                nameTextView.setOnClickListener(watchFragment)

                playerView.videoSurfaceView?.setOnClickListener {
                    if (watchFragment.isPaused) {
//                        onWatchResume(this)
                        watchFragment.startOrResumeCurrentWatch()
                    } else {
                        onWatchPause(this)
                    }
                    watchFragment.isPaused = !watchFragment.isPaused
                }
            }
        }

        override fun bind(position: Int) {
            Log.d(TAG, "bind")
            val watch = currentList[position]
            itemView.apply {
//                setViewConstrainRatio(videoMessageLayout, videoMessage.ratio!!)
                imageLoader.load(watch.ownerAvatarUrl, watchOwnerAvatarImgView) {
                    it.fit().centerCrop()
                }

//                Picasso.get().smartLoad(watch.musicOwnerAvatarUrl, resourceManager, musicOwnerAvatarImgView) {
//                    it.fit().centerCrop()
//                }

                bindFullscreen()
                bindVideoThumb(watch)

                shareCountTextView.text = utils.getMetricFormat(watch.shareCount)
                reactCountTextView.text = utils.getMetricFormat(watch.reactCount)
                commentCountTextView.text = utils.getMetricFormat(watch.commentCount)

                nameTextView.text = String.format("@ %s", watch.ownerName)
//                musicNameTextView.text = watch.musicName

                if (watch.text.isEmpty()) {
                    descTextView.visibility = View.GONE
                } else {
                    descTextView.text = watch.text
                    descTextView.visibility = View.VISIBLE
                }
            }
        }

        fun bindFullscreen() {
            Log.d(TAG, "bindFullscreen")
            itemView.apply {
                if (sharedPrefsManager.isWatchTabFullScreen()) {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    previewImgView.scaleType = ImageView.ScaleType.CENTER_CROP
                } else {
                    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    previewImgView.scaleType = ImageView.ScaleType.FIT_CENTER
                }
            }
        }

        private fun bindVideoThumb(watch: Watch) {
            itemView.apply {
                resourceManager.getVideoThumbUri(watch.videoMedia!!.uri!!) { uri ->
                    imageLoader.load(uri, previewImgView) {
                        it.fit().centerInside()
                    }
                }

                previewImgView.visibility = View.VISIBLE
            }
        }
    }
}