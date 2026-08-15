package com.nxd.mediakeycontrol

import android.service.notification.NotificationListenerService

/**
 * 空壳通知监听服务：仅用于获取 MediaSessionManager.getActiveSessions() 的调用权限。
 * Android 13 上 getActiveSessions(null) 对普通应用会抛 SecurityException，
 * 必须传入已启用的 NotificationListenerService 的 ComponentName。
 * 本服务不读取任何通知内容，只用于权限门控。
 */
class MediaNotificationListener : NotificationListenerService() {
    // 无需实现任何方法
}
