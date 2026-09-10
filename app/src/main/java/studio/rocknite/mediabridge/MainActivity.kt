package studio.rocknite.mediabridge

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * MediaBridge n'a besoin que d'un seul écran : expliquer le principe et
 * envoyer l'utilisateur vers les réglages "Accès aux notifications", seule
 * permission requise pour lire les MediaSession actives (MediaSessionManager).
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val explanation = TextView(this).apply {
            text = "MediaBridge relaie la musique en cours de n'importe quelle app " +
                "(YouTube Music, Spotify, etc.) vers la capsule Live Update d'Android 16.\n\n" +
                "Étape 1 : autorise l'accès aux notifications ci-dessous.\n" +
                "Étape 2 : lance ta musique, la capsule apparaît automatiquement."
            textSize = 16f
        }

        val grantAccessButton = Button(this).apply {
            text = "Ouvrir l'accès aux notifications"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(explanation)
        layout.addView(grantAccessButton)
        setContentView(layout)
    }
}
