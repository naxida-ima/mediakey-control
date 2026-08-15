package com.nxd.mediakeycontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.IBinder

/**
 * 前台服务 + MediaSession：劫持媒体控制栏。
 *
 * 机制：注册一个始终活跃的 MediaSession 并关联 MediaStyle 通知，
 * 系统控制栏/锁屏/通知媒体卡上出现的播放、暂停按钮回调到本服务，
 * 本服务把指令转发给真实播放器（MediaToggler），实现"劫持控制栏媒体播放/暂停"。
 */
class ControlBarService : Service() {

    companion object {
        private const val CHANNEL_ID = "media_key_control"
        private const val NOTIF_ID = 1001
        private const val ACTION_PLAY_PAUSE = "com.nxd.mediakeycontrol.PLAY_PAUSE"
        private const val ACTION_STOP = "com.nxd.mediakeycontrol.STOP"

        @Volatile
        var instance: ControlBarService? = null
            private set

        /** 本服务自己的 session token，供 MediaToggler 排除自身 */
        @Volatile
        var sessionToken: MediaSession.Token? = null
            private set

        /** UI 显示状态：true = 显示"正在播放/暂停按钮"，false = 显示"播放按钮" */
        @Volatile
        var showingPlaying: Boolean = false
            private set

        /** 音量键触发后，同步刷新我们的卡片状态 */
        fun notifyStateChanged() {
            instance?.refreshState()
        }

        fun isRunning(): Boolean = instance != null
    }

    private lateinit var session: MediaSession
    private lateinit var nm: NotificationManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createChannel()
        session = MediaSession(this, "MediaKeyControl")
        session.setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                MediaToggler.play(this@ControlBarService)
                setShowing(true)
            }

            override fun onPause() {
                MediaToggler.pause(this@ControlBarService)
                setShowing(false)
            }

            override fun onSkipToNext() {
                MediaToggler.next(this@ControlBarService)
            }
        })
        session.isActive = true
        sessionToken = session.sessionToken
        // 初始状态：看当前是否有媒体在播
        setShowing(MediaToggler.isAnyPlaying(this))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                MediaToggler.toggle(this)
                setShowing(!showingPlaying)
            }
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        session.isActive = false
        session.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** 刷新展示状态（音量键 toggle 后调用） */
    fun refreshState() {
        setShowing(MediaToggler.isAnyPlaying(this))
    }

    private fun setShowing(playing: Boolean) {
        showingPlaying = playing
        val state = if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val ps = PlaybackState.Builder()
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, if (playing) 1.0f else 0.0f)
            .setActions(
                PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
                        or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_SKIP_TO_NEXT
            )
            .build()
        session.setPlaybackState(ps)
        val meta = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, if (playing) "媒体播放中" else "媒体已暂停")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "MediaKeyControl")
            .putLong(MediaMetadata.METADATA_KEY_DURATION, 0)
            .build()
        session.setMetadata(meta)
        nm.notify(NOTIF_ID, buildNotification(playing))
    }

    private fun buildNotification(playing: Boolean): Notification {
        val ppIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ControlBarService::class.java).setAction(ACTION_PLAY_PAUSE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ControlBarService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val icon = if (playing) R.drawable.ic_pause else R.drawable.ic_play
        val title = if (playing) "媒体播放中" else "媒体已暂停"
        val action = Notification.Action.Builder(
            icon, if (playing) "暂停" else "播放", ppIntent
        ).build()
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("媒体按键控制")
            .setContentText(title)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(0)
            )
            .addAction(action)
            .addAction(
                Notification.Action.Builder(
                    R.drawable.ic_stop, "退出", stopIntent
                ).build()
            )
            .build()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "媒体按键控制", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "媒体控制栏劫持状态"
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }
}
