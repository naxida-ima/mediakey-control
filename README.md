# 媒体按键控制 (MediaKeyControl)

OPPO Find X2 · ColorOS 13.1 (Android 13) 专用：**劫持音量下键 + 劫持媒体控制栏**，实现全局媒体播放/暂停控制。

## 功能

| 功能 | 机制 | 说明 |
|---|---|---|
| 劫持音量下键 | 无障碍服务 `VolumeKeyService` | 按下音量下键 = 媒体播放/暂停切换，拦截按键（音量条不弹出、音量不变） |
| 劫持控制栏播放/暂停 | 前台服务 + `MediaSession` + MediaStyle 通知 | 常驻媒体通知/锁屏媒体卡上的播放/暂停按钮由本应用接管，转发给当前真实播放器 |

## 构建

```bash
# 本地（需 JDK 17 + Android SDK 33）
gradle assembleRelease
# 或推送到 GitHub，Actions 自动构建（Gradle 8.5 + AGP 8.2.2）
```

签名：固定 keystore（`keystore/mediakey.keystore`，alias `mediakey`，密码见 `gradle.properties`），覆盖安装不冲突。

## 安装与使用（ColorOS 13.1）

1. 安装 APK，打开「媒体按键控制」
2. **开启音量键劫持**：点「打开系统无障碍设置」→ 无障碍 → 已下载的应用 → 媒体按键控制 → 开启服务
   - 该服务即「劫持音量下键」的开关（应用内开关与系统无障碍开关联动）
3. **开启控制栏劫持**：在应用内打开「劫持控制栏媒体播放/暂停」开关
   - 首次会请求通知权限（Android 13 必需），授予后出现常驻媒体通知
   - 锁屏/通知栏的媒体卡上按「播放/暂停」即全局控制媒体
4. **防后台清理**（重要）：
   - 设置 → 应用管理 → 媒体按键控制 → 自启动：允许
   - 设置 → 电池 → 应用耗电管理 → 媒体按键控制 → 允许完全后台行为（不限制）
5. 播放任意音乐/听书 App，按音量下键即可播放/暂停

## 工作原理

- **音量键劫持**：`AccessibilityService.onKeyEvent()` 捕获 `KEYCODE_VOLUME_DOWN` 按下事件，触发 `MediaToggler.toggle()`（通过 `MediaSessionManager.getActiveSessions()` 找到当前媒体会话，向真实播放器发 play/pause 指令），返回 `true` 消费按键事件 → 系统音量条不出现。
- **控制栏劫持**：`ControlBarService` 注册活跃 `MediaSession` 并关联 `MediaStyle` 通知，控制栏/锁屏媒体卡的播放、暂停、播放暂停按钮回调到 `MediaSession.Callback`，再经 `MediaToggler` 转发给真实播放器；同时排除自身会话避免死循环。
- 兼容任何注册了 MediaSession 的播放器（网易云音乐、QQ音乐、汽水音乐、各类听书 App 等）。

## 已知限制

- 无障碍服务拦截音量键依赖系统行为，ColorOS 13.1 实测可拦截；若个别版本仍弹出音量条，可在系统设置中关闭「按键音/音量条」或反馈。
- 音量上键默认保留系统功能；想改成长按下一首等，改 `VolumeKeyService.onKeyEvent` 重新构建。
- 若真机测试发现转发目标选择不理想，可在 `MediaToggler` 中调整会话选择策略。
