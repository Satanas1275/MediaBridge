package studio.rocknite.mediabridge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build

/**
 * Construit la notif ProgressStyle + requestPromotedOngoing = la capsule
 * Live Update d'Android 16.
 *
 * Le chip "collapsed" dans la status bar est minuscule : il ne peut afficher
 * qu'une icône (+ un court texte/chrono en option), pas le titre en entier.
 * Le titre complet, l'artiste et la pochette s'affichent quand on déroule
 * le shade ou sur le lockscreen -> on met la pochette comme icône du
 * "tracker" (celle qui se déplace le long de la barre de progression, donc
 * visible même en collapsed si le système la montre à la place du smallIcon
 * générique) et comme largeIcon pour la vue étendue.
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
        isPlaying: Boolean,
        albumArt: Bitmap? = null
    ) {
        ensureChannel(context)

        // Durée/position en secondes : l'unité de ProgressStyle est arbitraire
        // (juste "la même unité que Segment.getLength()"), les secondes sont
        // largement assez précises pour une barre de lecture.
        val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)
        val positionSec = (positionMs / 1000).toInt().coerceIn(0, durationSec)

        val albumArtIcon = albumArt?.let { Icon.createWithBitmap(it) }

        val remainingSec = (durationSec - positionSec).coerceAtLeast(0)
        val shortCriticalText = "%d:%02d".format(remainingSec / 60, remainingSec % 60)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play) // icône silhouette requise par le système, garde un fallback simple
            .setContentTitle(title)
            .setContentText(artist ?: "")
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setShortCriticalText(shortCriticalText) // seul contenu texte que le chip status bar peut réalistement afficher (max ~7 car.)
            .setExtras(android.os.Bundle().apply {
                // Constante Notification.EXTRA_REQUEST_PROMOTED_ONGOING pas encore
                // présente dans le compileSdk 36 stable dispo en CI (ajoutée dans une
                // QPR ultérieure côté API publique) -> clé brute, marche pareil au runtime.
                putBoolean("android.requestPromotedOngoing", true)
            })

        if (albumArtIcon != null) {
            builder.setLargeIcon(albumArt)
        }

        if (Build.VERSION.SDK_INT >= 36) { // Build.VERSION_CODES.BAKLAVA
            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgressSegments(
                    listOf(
                        Notification.ProgressStyle.Segment(durationSec)
                            .setColor(Color.parseColor("#1DB954")) // vert-ish, à remplacer par ton thème turquoise
                    )
                )
                .setProgress(positionSec)

            if (albumArtIcon != null) {
                progressStyle.setProgressTrackerIcon(albumArtIcon)
            }

            builder.style = progressStyle
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
