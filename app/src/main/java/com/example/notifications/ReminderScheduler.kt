package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

object ReminderChannels {
    const val CARE_REMINDERS_CHANNEL = "care_reminders"
    const val SOS_ALERTS_CHANNEL = "sos_alerts"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val careChannel = NotificationChannel(
                CARE_REMINDERS_CHANNEL,
                "Care & Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Scheduled medicine, task, and appointment reminders"
            }
            val sosChannel = NotificationChannel(
                SOS_ALERTS_CHANNEL,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent SOS updates from linked elders"
            }
            manager.createNotificationChannel(careChannel)
            manager.createNotificationChannel(sosChannel)
        }
    }
}

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderChannels.ensureChannels(context)
        val title = intent.getStringExtra("title") ?: "Care Connect Reminder"
        val body = intent.getStringExtra("body") ?: "Please check your Care Connect schedule."
        val channelId = intent.getStringExtra("channelId") ?: ReminderChannels.CARE_REMINDERS_CHANNEL
        val notificationId = intent.getIntExtra("notificationId", 1001)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                Log.w("ReminderReceiver", "POST_NOTIFICATIONS not granted; skipping notification.")
                return
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.w("ReminderReceiver", "Notification permission denied", e)
        }
    }
}

class ReminderScheduler(private val context: Context) {

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun scheduleDailyMedicineReminder(
        medicineId: String,
        medicineName: String,
        dosage: String,
        timeMinutes: Int
    ) {
        ReminderChannels.ensureChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val hour = (timeMinutes / 60).coerceIn(0, 23)
        val minute = (timeMinutes % 60).coerceIn(0, 59)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val reqCode = medicineId.hashCode()
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra("title", "Medicine Reminder: $medicineName")
            putExtra("body", "Scheduled dose: $dosage")
            putExtra("channelId", ReminderChannels.CARE_REMINDERS_CHANNEL)
            putExtra("notificationId", reqCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Use setAndAllowWhileIdle so exact-alarm restrictions never crash the app
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            pendingIntent
        )
    }

    fun showImmediateNotification(
        id: Int,
        title: String,
        body: String,
        isSos: Boolean = false
    ) {
        ReminderChannels.ensureChannels(context)
        if (!hasNotificationPermission()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channel = if (isSos) {
            ReminderChannels.SOS_ALERTS_CHANNEL
        } else {
            ReminderChannels.CARE_REMINDERS_CHANNEL
        }

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "Notification blocked by permission", e)
        }
    }
}
