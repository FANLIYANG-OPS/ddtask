package com.ddtask.scheduler

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ddtask.scheduler.util.DingTalkLauncher

/**
 * 透明中转页：唤醒屏幕并打开钉钉，立即结束。
 */
class LaunchProxyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        launchDingTalk()
        finish()
    }

    private fun launchDingTalk() {
        if (!DingTalkLauncher.isInstalled(this)) {
            Toast.makeText(this, R.string.dingtalk_not_installed_toast, Toast.LENGTH_LONG).show()
            return
        }
        val launchIntent = packageManager.getLaunchIntentForPackage(DingTalkLauncher.PACKAGE_NAME)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, R.string.dingtalk_launch_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
