package com.ddtask.scheduler.util

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ddtask.scheduler.R

/** 汇总应用运行所需的权限项，并提供跳转到对应系统设置页的入口。 */
object PermissionSetupHelper {

    const val REQUEST_POST_NOTIFICATIONS = ActivityRequestCodes.POST_NOTIFICATIONS

    data class Step(
        val id: String,
        val titleRes: Int,
        val messageRes: Int,
        val isGranted: (Context) -> Boolean
    )

    fun requiredSteps(context: Context): List<Step> {
        val steps = mutableListOf<Step>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            steps.add(
                Step(
                    id = "post_notifications",
                    titleRes = R.string.permission_post_notifications_title,
                    messageRes = R.string.permission_post_notifications_message,
                    isGranted = { ctx ->
                        ContextCompat.checkSelfPermission(
                            ctx,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                )
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            steps.add(
                Step(
                    id = "exact_alarm",
                    titleRes = R.string.exact_alarm_title,
                    messageRes = R.string.exact_alarm_message,
                    isGranted = { ctx ->
                        ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                    }
                )
            )
        }

        steps.add(
            Step(
                id = "write_settings",
                titleRes = R.string.write_settings_title,
                messageRes = R.string.permission_write_settings_setup_message,
                isGranted = { ctx -> BrightnessController.canWriteSettings(ctx) }
            )
        )

        steps.add(
            Step(
                id = "overlay",
                titleRes = R.string.grant_overlay_permission,
                messageRes = R.string.permission_overlay_message,
                isGranted = { ctx -> KeepScreenOverlay(ctx).canDrawOverlay() }
            )
        )

        steps.add(
            Step(
                id = "notification_listener",
                titleRes = R.string.grant_notification_access,
                messageRes = R.string.notification_access_required_message,
                isGranted = { ctx -> NotificationAccess.isEnabled(ctx) }
            )
        )

        return steps
    }

    fun allGranted(context: Context): Boolean {
        return requiredSteps(context).all { it.isGranted(context) }
    }

    fun grantedCount(context: Context): Int {
        return requiredSteps(context).count { it.isGranted(context) }
    }

    fun totalCount(context: Context): Int = requiredSteps(context).size

    fun openStepSettings(activity: Activity, stepId: String) {
        when (stepId) {
            "post_notifications" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_POST_NOTIFICATIONS
                    )
                }
            }
            "exact_alarm" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    activity.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    )
                }
            }
            "write_settings" -> {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${activity.packageName}")
                    }
                )
            }
            "overlay" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    activity.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:${activity.packageName}")
                        }
                    )
                }
            }
            "notification_listener" -> {
                NotificationAccess.openSettings(activity)
            }
        }
    }
}
