package com.g22.orbitsoundkotlin.utils

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
import com.g22.orbitsoundkotlin.MainActivity
import com.g22.orbitsoundkotlin.R
import com.g22.orbitsoundkotlin.models.Achievement

/**
 * Helper class for showing achievement unlock notifications.
 * Handles notification channel creation and permission checks.
 */
class AchievementNotificationHelper(private val context: Context) {
    
    companion object {
        private const val CHANNEL_ID = "achievements_channel"
        private const val CHANNEL_NAME = "Achievements"
        private const val CHANNEL_DESCRIPTION = "Notifications for unlocked achievements"
        private const val NOTIFICATION_ACTION = "OPEN_ACHIEVEMENTS"
        
        @Volatile
        private var INSTANCE: AchievementNotificationHelper? = null
        
        fun getInstance(context: Context): AchievementNotificationHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AchievementNotificationHelper(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Create notification channel for Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permission not required for older versions
        }
    }
    
    /**
     * Show notification for unlocked achievement.
     * Tapping the notification navigates to AchievementsScreen.
     */
    fun showAchievementUnlocked(achievement: Achievement, userId: String) {
        android.util.Log.d("NotificationHelper", "Attempting to show notification for: ${achievement.name}")
        
        // Check permission
        if (!hasNotificationPermission()) {
            android.util.Log.w("NotificationHelper", "❌ Notification permission NOT granted")
            return
        }
        
        android.util.Log.d("NotificationHelper", "✅ Notification permission granted")
        
        // Create intent for notification tap
        val intent = Intent(context, MainActivity::class.java).apply {
            action = NOTIFICATION_ACTION
            putExtra("userId", userId)
            putExtra("achievementId", achievement.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            achievement.id.hashCode(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Trophy icon would be better
            .setContentTitle("🏆 Achievement Unlocked!")
            .setContentText(achievement.name)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("${achievement.name}\n${achievement.description}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        // Show notification
        try {
            NotificationManagerCompat.from(context).notify(
                achievement.id.hashCode(), // Unique notification ID
                notification
            )
            android.util.Log.d("NotificationHelper", "🔔 Notification shown successfully for: ${achievement.name}")
        } catch (e: SecurityException) {
            android.util.Log.e("NotificationHelper", "❌ SecurityException showing notification", e)
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "❌ Error showing notification", e)
        }
    }
    
    /**
     * Cancel a specific achievement notification.
     */
    fun cancelNotification(achievementId: String) {
        NotificationManagerCompat.from(context).cancel(achievementId.hashCode())
    }
    
    /**
     * Cancel all achievement notifications.
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}

