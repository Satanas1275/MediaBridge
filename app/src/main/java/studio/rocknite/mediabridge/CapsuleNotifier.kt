package studio.rocknite.mediabridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Construit la notif ProgressStyle + requestPromotedOngoing = la capsule
 * Live Update d'Android 16. Le titre du morceau change rarement (pas de
 * risque de rate-limiting), la progression suit la position de lecture.
 */
object CapsuleNotifier {

    private const val CHANNEL_ID = "media_bridge_capsule"
    private const val NOTIF_ID = 1

    fun update(
        context: Context,
        title: String,
        artist: String?,
        durationMs: Long,
        positionMs: Long,
        isPlaying: Boolean
    ) {
        ensureChannel(context)

        val progressMax = if (durationMs > 0) (durationMs / 1000).toInt() else 100
        val progressCurrent = (positionMs / 1000).toInt().coerceIn(0, progressMax)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // TODO: remplacer par une icône MediaBridge
            .setContentTitle(title)
            .setContentText(artist ?: "")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setExtras(android.os.Bundle().apply {
                putBoolean(Notification.EXTRA_REQUEST_PROMOTED_ONGOING, true)
            })

        if (Build.VERSION.SDK_INT >= 36) { // Build.VERSION_CODES.BAKLAVA
            val progressStyle = Notification.ProgressStyle()
                .setProgress(progressCurrent)
            builder.style = progressStyle
            // setProgress(max, current, indeterminate) côté ProgressStyle attend
            // qu'on configure aussi le "track" total via setProgressMax si dispo
            // sur l'API finale — à vérifier contre la doc au moment du build,
            // l'API a bougé plusieurs fois entre les bêtas Android 16.
        }

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIF_ID, builder.build())
    }

    fun clear(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Capsule lecture en cours",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }
}
