package com.nxd.mediakeycontrol

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * 无障碍服务：劫持音量键。
 * onKeyEvent 里拦截 KEYCODE_VOLUME_DOWN（按下），
 * 触发媒体播放/暂停切换，并返回 true 吞掉按键事件（系统不降音量、音量条不弹出）。
 * 需要用户在系统设置→无障碍里手动开启本服务。
 */
class VolumeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    MediaToggler.toggle(this)
                    ControlBarService.notifyStateChanged()
                }
                // 吞掉事件：音量条不出现、音量不变化
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                // 音量上键保留系统功能（也可改为拦截，见 README）
                return super.onKeyEvent(event)
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要窗口内容，仅按键过滤
    }

    override fun onInterrupt() {
    }
}
