package studio.rocknite.mediabridge

import android.content.ComponentName
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * En étant un NotificationListenerService actif, on a le droit d'appeler
 * MediaSessionManager.getActiveSessions() : ça donne un MediaController pour
 * CHAQUE app qui expose une session média (peu importe laquelle), sans que
 * l'app source ait quoi que ce soit à faire de son côté.
 */
class MediaListenerService : NotificationListenerService() {

    private lateinit var sessionManager: MediaSessionManager
    private lateinit var componentName: ComponentName
    private val callbacks = mutableMapOf<MediaController, MediaController.Callback>()

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            rebindControllers(controllers ?: emptyList())
        }

    override fun onListenerConnected() {
        super.onListenerConnected()
        componentName = ComponentName(this, MediaListenerService::class.java)
        sessionManager = getSystemService(MediaSessionManager::class.java)

        sessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, componentName)
        rebindControllers(sessionManager.getActiveSessions(componentName))
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        sessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        callbacks.keys.forEach { it.unregisterCallback(callbacks.getValue(it)) }
        callbacks.clear()
        CapsuleNotifier.clear(this)
    }

    private fun rebindControllers(controllers: List<MediaController>) {
        // Nettoie les anciens callbacks avant de rebind sur la nouvelle liste
        callbacks.keys.forEach { it.unregisterCallback(callbacks.getValue(it)) }
        callbacks.clear()

        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                    refreshFromBestController()
                }

                override fun onPlaybackStateChanged(state: PlaybackState?) {
                    refreshFromBestController()
                }

                override fun onSessionDestroyed() {
                    refreshFromBestController()
                }
            }
            controller.registerCallback(callback)
            callbacks[controller] = callback
        }

        refreshFromBestController()
    }

    /** Si plusieurs apps ont une session active, on privilégie celle qui joue vraiment. */
    private fun refreshFromBestController() {
        val best = callbacks.keys
            .filter { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            .maxByOrNull { it.playbackState?.lastPositionUpdateTime ?: 0L }
            ?: callbacks.keys.firstOrNull { it.playbackState != null }

        if (best == null) {
            CapsuleNotifier.clear(this)
            return
        }

        CapsuleNotifier.update(
            context = this,
            title = best.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE)
                ?: "Lecture en cours",
            artist = best.metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST),
            durationMs = best.metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            positionMs = best.playbackState?.position ?: 0L,
            isPlaying = best.playbackState?.state == PlaybackState.STATE_PLAYING
        )
    }

    // Requis par NotificationListenerService mais pas utilisé ici : on ne lit
    // pas le contenu des notifications, seulement les MediaSession.
    override fun onNotificationPosted(sbn: StatusBarNotification?) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}
