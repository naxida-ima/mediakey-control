package com.nxd.mediakeycontrol

import android.content.Context
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState

/**
 * 跨应用媒体控制核心：
 * 通过 MediaSessionManager 拿到所有活跃媒体会话（Android 8.0+ 允许），
 * 向真实播放器（网易云/QQ音乐/听书App等）发送 play/pause 指令。
 * 注意：getActiveSessions() 返回的是 MediaController 列表，直接用其
 * playbackState / transportControls 属性即可，没有 controller 属性。
 */
object MediaToggler {

    private val PLAYING_STATES = setOf(
        PlaybackState.STATE_PLAYING,
        PlaybackState.STATE_BUFFERING,
        PlaybackState.STATE_FAST_FORWARDING,
        PlaybackState.STATE_REWINDING,
        PlaybackState.STATE_CONNECTING
    )

    private val PAUSED_STATES = setOf(
        PlaybackState.STATE_PAUSED,
        PlaybackState.STATE_STOPPED,
        PlaybackState.STATE_NONE,
        PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
        PlaybackState.STATE_SKIPPING_TO_NEXT
    )

    /** 播放：找暂停中的会话发 play */
    fun play(context: Context): Boolean {
        for (s in activeSessions(context)) {
            val st = s.playbackState?.state
            if (st == null || st in PAUSED_STATES) {
                s.transportControls.play()
                return true
            }
        }
        return false
    }

    /** 暂停：找播放中的会话发 pause */
    fun pause(context: Context): Boolean {
        for (s in activeSessions(context)) {
            val st = s.playbackState?.state
            if (st != null && st in PLAYING_STATES) {
                s.transportControls.pause()
                return true
            }
        }
        return false
    }

    /** 播放/暂停切换：有播放中的就暂停，否则播放暂停中的 */
    fun toggle(context: Context): Boolean {
        if (pause(context)) return true
        return play(context)
    }

    /** 下一首 */
    fun next(context: Context): Boolean {
        for (s in activeSessions(context)) {
            val st = s.playbackState?.state
            if (st != null && (st in PLAYING_STATES || st in PAUSED_STATES)) {
                s.transportControls.skipToNext()
                return true
            }
        }
        return false
    }

    /** 是否有正在播放的媒体（供 UI 显示） */
    fun isAnyPlaying(context: Context): Boolean {
        for (s in activeSessions(context)) {
            val st = s.playbackState?.state
            if (st != null && st in PLAYING_STATES) return true
        }
        return false
    }

    private fun activeSessions(context: Context): List<android.media.session.MediaController> {
        val msm = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        // Android 13+ 必须传入已启用的 NotificationListenerService 的 ComponentName，
        // 否则 getActiveSessions(null) 抛 SecurityException: Missing permission to control media
        val cn = ComponentName(context, MediaNotificationListener::class.java)
        return try {
            val sessions = msm.getActiveSessions(cn)
            val own = ControlBarService.sessionToken
            sessions.filter { it.sessionToken != own }
        } catch (e: SecurityException) {
            Log.w("MediaToggler", "通知监听未启用，无法获取媒体会话", e)
            emptyList()
        }
    }
}
