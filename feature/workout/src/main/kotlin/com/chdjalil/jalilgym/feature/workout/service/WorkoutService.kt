package com.chdjalil.jalilgym.feature.workout.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.chdjalil.jalilgym.core.domain.repository.RestTimer
import com.chdjalil.jalilgym.core.domain.repository.RestTimerState
import com.chdjalil.jalilgym.core.domain.repository.UserPreferencesRepository
import com.chdjalil.jalilgym.core.domain.repository.WorkoutRepository
import com.chdjalil.jalilgym.core.domain.workout.CompleteSetUseCase
import com.chdjalil.jalilgym.core.domain.workout.nextIncompleteSet
import com.chdjalil.jalilgym.core.model.WorkoutSession
import com.chdjalil.jalilgym.core.wear.WearWorkoutState
import com.chdjalil.jalilgym.core.wear.wearWorkoutState
import com.chdjalil.jalilgym.feature.workout.R
import com.chdjalil.jalilgym.feature.workout.navigation.WORKOUT_DEEP_LINK
import com.chdjalil.jalilgym.feature.workout.wear.WearPublisher
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Runs while a workout is in progress: an ongoing notification with the rest
 * countdown and lock-screen actions, and a vibration + alert when rest is over.
 * It stops itself as soon as there's no active workout.
 */
@AndroidEntryPoint
class WorkoutService : Service() {

    @Inject lateinit var workoutRepository: WorkoutRepository

    @Inject lateinit var restTimer: RestTimer

    @Inject lateinit var completeSet: CompleteSetUseCase

    @Inject lateinit var preferencesRepository: UserPreferencesRepository

    @Inject lateinit var wearPublisher: WearPublisher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground promptly after startForegroundService().
        ServiceCompat.startForeground(
            this,
            ONGOING_ID,
            baseNotification().setContentTitle(getString(R.string.feature_workout_notification_title)).build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        when (intent?.action) {
            ACTION_COMPLETE_SET -> scope.launch {
                completeSet(labelFor = { name, set, total -> getString(R.string.feature_workout_rest_label, name, set, total) })
            }
            ACTION_ADD_REST -> restTimer.add(EXTRA_REST_SECONDS)
            ACTION_SKIP_REST -> restTimer.skip()
        }
        if (!started) {
            started = true
            observeWorkout()
        }
        return START_STICKY
    }

    private fun observeWorkout() {
        scope.launch {
            // The watch app shows the next set and the rest countdown.
            combine(workoutRepository.observeActiveSession(), restTimer.state, preferencesRepository.userPreferences) { session, rest, prefs ->
                wearWorkoutState(session, rest, prefs.units)
            }.collect(wearPublisher::publish)
        }
        scope.launch {
            combine(workoutRepository.observeActiveSession(), restTimer.state) { session, rest -> session to rest }
                .collectLatest { (session, rest) ->
                    if (session == null) {
                        wearPublisher.publish(WearWorkoutState())
                        ServiceCompat.stopForeground(this@WorkoutService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                        return@collectLatest
                    }
                    notify(ONGOING_ID, ongoingNotification(session, rest))
                    if (rest != null) {
                        // collectLatest cancels this wait if the timer changes (±15 s, skip, new rest).
                        delay(rest.remainingMillis(System.currentTimeMillis()))
                        restTimer.skip()
                        alertRestOver(session)
                    }
                }
        }
    }

    private fun ongoingNotification(session: WorkoutSession, rest: RestTimerState?): Notification {
        val next = session.nextIncompleteSet()
        val nextText = next?.let { (exercise, set) ->
            getString(
                R.string.feature_workout_rest_label,
                exercise.exercise.name,
                exercise.sets.sortedBy { it.position }.indexOf(set) + 1,
                exercise.sets.size,
            )
        } ?: getString(R.string.feature_workout_notification_all_done)

        val builder = baseNotification()
            .setContentTitle(session.name)
            .setContentText(if (rest != null) getString(R.string.feature_workout_notification_resting, nextText) else nextText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        if (rest != null) {
            builder.setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setWhen(rest.endsAtMillis)
                .setShowWhen(true)
                .addAction(0, getString(R.string.feature_workout_plus_seconds, EXTRA_REST_SECONDS), action(ACTION_ADD_REST))
                .addAction(0, getString(R.string.feature_workout_skip_rest), action(ACTION_SKIP_REST))
        } else {
            builder.setUsesChronometer(true).setWhen(session.startedAt).setShowWhen(true)
        }
        if (next != null) {
            builder.addAction(0, getString(R.string.feature_workout_complete_set), action(ACTION_COMPLETE_SET))
        }
        return builder.build()
    }

    private fun alertRestOver(session: WorkoutSession) {
        vibrate()
        val next = session.nextIncompleteSet()
        val text = next?.let { (exercise, _) -> getString(R.string.feature_workout_rest_over_next, exercise.exercise.name) }
            ?: getString(R.string.feature_workout_rest_over_body)
        notify(
            ALERT_ID,
            NotificationCompat.Builder(this, ALERT_CHANNEL)
                .setSmallIcon(R.drawable.feature_workout_ic_notification)
                .setContentTitle(getString(R.string.feature_workout_rest_over))
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(openWorkoutIntent())
                .setAutoCancel(true)
                .setTimeoutAfter(ALERT_TIMEOUT_MILLIS)
                .build(),
        )
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getSystemService(this, VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ContextCompat.getSystemService(this, Vibrator::class.java)
        } ?: return
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 150, 400), -1))
    }

    // Permission is checked via areNotificationsEnabled(); without it notifications are simply skipped.
    @SuppressLint("MissingPermission")
    private fun notify(id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(this)
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    private fun baseNotification() = NotificationCompat.Builder(this, ONGOING_CHANNEL)
        .setSmallIcon(R.drawable.feature_workout_ic_notification)
        .setContentIntent(openWorkoutIntent())
        .setSilent(true)

    private fun openWorkoutIntent(): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(WORKOUT_DEEP_LINK)).setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun action(name: String): PendingIntent = PendingIntent.getService(
        this,
        name.hashCode(),
        Intent(this, WorkoutService::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ONGOING_CHANNEL = "workout"
        private const val ALERT_CHANNEL = "rest_alerts"
        private const val ONGOING_ID = 1001
        private const val ALERT_ID = 1002
        private const val ALERT_TIMEOUT_MILLIS = 60_000L
        private const val EXTRA_REST_SECONDS = 30

        private const val ACTION_COMPLETE_SET = "com.chdjalil.jalilgym.workout.COMPLETE_SET"
        private const val ACTION_ADD_REST = "com.chdjalil.jalilgym.workout.ADD_REST"
        private const val ACTION_SKIP_REST = "com.chdjalil.jalilgym.workout.SKIP_REST"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        }

        private fun createChannels(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            manager.createNotificationChannel(
                NotificationChannel(
                    ONGOING_CHANNEL,
                    context.getString(R.string.feature_workout_channel_workout),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply { description = context.getString(R.string.feature_workout_channel_workout_desc) },
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL,
                    context.getString(R.string.feature_workout_channel_alerts),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.feature_workout_channel_alerts_desc)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 400, 150, 400)
                },
            )
        }
    }
}
