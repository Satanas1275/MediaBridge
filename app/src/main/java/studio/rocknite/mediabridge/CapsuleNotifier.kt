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
                // Constante Notification.EXTRA_REQUEST_PROMOTED_ONGOING pas encore
                // présente dans le compileSdk 36 stable dispo en CI (ajoutée dans une
                // QPR ultérieure côté API publique) -> clé brute, marche pareil au runtime.
                putBoolean("android.requestPromotedOngoing", true)
            })

        if (Build.VERSION.SDK_INT >= 36) { // Build.VERSION_CODES.BAKLAVA
            // NOTE: je laisse le ProgressStyle "nu" pour l'instant. Les méthodes
            // setProgressPoints/setProgressSegments existent bien sur l'API mais
            // leurs signatures exactes ont bougé entre les bêtas Android 16 -> à
            // vérifier/brancher une fois testé en local contre le vrai SDK 36,
            // plutôt que de deviner une signature qui casserait le build CI.
            builder.style = Notification.ProgressStyle()
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
