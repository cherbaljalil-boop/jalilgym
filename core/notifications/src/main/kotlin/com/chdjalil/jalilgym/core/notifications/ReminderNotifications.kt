package com.chdjalil.jalilgym.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.chdjalil.jalilgym.core.domain.reminders.WeeklySummary
import com.chdjalil.jalilgym.core.domain.units.formatNumber
import com.chdjalil.jalilgym.core.domain.units.fromKg
import com.chdjalil.jalilgym.core.model.UnitSystem
import java.util.Locale
import kotlin.math.roundToLong

internal object ReminderNotifications {
    private const val REMINDER_CHANNEL = "jalilgym-reminders"
    private const val SUMMARY_CHANNEL = "jalilgym-weekly-summary"
    private const val REMINDER_ID = 2001
    private const val SUMMARY_ID = 2002

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL,
                context.getString(R.string.core_notifications_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.core_notifications_channel_reminders_desc) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                SUMMARY_CHANNEL,
                context.getString(R.string.core_notifications_channel_summary),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = context.getString(R.string.core_notifications_channel_summary_desc) },
        )
    }

    fun showWorkoutReminder(context: Context, dayName: String?, programName: String?) {
        val text = if (dayName != null && programName != null) {
            context.getString(R.string.core_notifications_reminder_next_day, dayName, programName)
        } else {
            context.getString(R.string.core_notifications_reminder_generic)
        }
        post(context, REMINDER_ID, REMINDER_CHANNEL, context.getString(R.string.core_notifications_reminder_title), text)
    }

    fun showWeeklySummary(context: Context, summary: WeeklySummary, units: UnitSystem) {
        post(context, SUMMARY_ID, SUMMARY_CHANNEL, context.getString(R.string.core_notifications_summary_title), summaryText(context, summary, units))
    }

    fun summaryText(context: Context, summary: WeeklySummary, units: UnitSystem): String {
        val unit = if (units == UnitSystem.IMPERIAL) "lb" else "kg"
        val parts = buildList {
            add(context.getString(R.string.core_notifications_summary_workouts, summary.workouts, summary.weeklyTarget))
            if (summary.volumeKg > 0) {
                val volume = String.format(Locale.getDefault(), "%,d", units.fromKg(summary.volumeKg).roundToLong())
                add(context.getString(R.string.core_notifications_summary_volume, volume, unit))
            }
            add(context.getString(R.string.core_notifications_summary_logged, summary.daysLogged))
            summary.weightChangeKg?.let { change ->
                val value = units.fromKg(change)
                val signed = (
                    if (value > 0) {
                        "+"
                    } else if (value < 0) {
                        "−"
                    } else {
                        "±"
                    }
                    ) + formatNumber(kotlin.math.abs(value))
                add(context.getString(R.string.core_notifications_summary_weight, signed, unit))
            }
        }
        return parts.joinToString(context.getString(R.string.core_notifications_summary_separator))
    }

    private fun post(context: Context, id: Int, channel: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannels(context)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.core_notifications_ic)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .apply {
                launch?.let { setContentIntent(PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)) }
            }
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
