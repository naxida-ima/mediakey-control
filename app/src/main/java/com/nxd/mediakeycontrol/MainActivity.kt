package com.nxd.mediakeycontrol

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    private lateinit var swVolumeKey: Switch
    private lateinit var swControlBar: Switch
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus = findViewById(R.id.tvStatus)
        swVolumeKey = findViewById(R.id.swVolumeKey)
        swControlBar = findViewById(R.id.swControlBar)
        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { refreshAll() }

        swVolumeKey.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked && !isAccessibilityOn()) {
                Toast.makeText(this, "请先在系统无障碍设置里开启「媒体按键控制」服务", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        swControlBar.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked) {
                requestNotifPermissionIfNeeded()
                startForegroundService(Intent(this, ControlBarService::class.java))
                Toast.makeText(this, "控制栏媒体卡接管已开启", Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, ControlBarService::class.java))
                Toast.makeText(this, "控制栏媒体卡接管已关闭", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAll()
    }

    private fun refreshAll() {
        // 无障碍状态
        val accOn = isAccessibilityOn()
        swVolumeKey.setOnCheckedChangeListener(null)
        swVolumeKey.isChecked = accOn
        swVolumeKey.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked && !isAccessibilityOn()) {
                Toast.makeText(this, "请先在系统无障碍设置里开启「媒体按键控制」服务", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        // 前台服务状态
        val svcOn = ControlBarService.isRunning()
        swControlBar.setOnCheckedChangeListener(null)
        swControlBar.isChecked = svcOn
        swControlBar.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            if (checked) {
                requestNotifPermissionIfNeeded()
                startForegroundService(Intent(this, ControlBarService::class.java))
                Toast.makeText(this, "控制栏媒体卡接管已开启", Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, ControlBarService::class.java))
                Toast.makeText(this, "控制栏媒体卡接管已关闭", Toast.LENGTH_SHORT).show()
            }
        }

        // 状态文本
        val sb = StringBuilder()
        sb.append("● 音量下键劫持：").append(if (accOn) "已启用" else "未启用").append("\n")
        sb.append("● 控制栏媒体卡接管：").append(if (svcOn) "已启用" else "未启用").append("\n")
        sb.append("● 当前媒体：")
        sb.append(if (MediaToggler.isAnyPlaying(this)) "有媒体正在播放" else "无媒体在播放")
        tvStatus.text = sb.toString()
    }

    private fun isAccessibilityOn(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val cn = ComponentName(this, VolumeKeyService::class.java)
        return enabled.any { it.resolveInfo.serviceInfo.packageName == cn.packageName && it.resolveInfo.serviceInfo.name == cn.className }
    }

    private fun requestNotifPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.areNotificationsEnabled()) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            val ok = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            Toast.makeText(this, if (ok) "通知权限已授予" else "通知权限被拒绝，控制栏卡片将无法显示", Toast.LENGTH_LONG).show()
        }
    }
}
