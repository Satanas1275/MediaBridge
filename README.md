# MediaBridge

Relaie la session média active de **n'importe quelle app** (YouTube Music,
Spotify, VLC, etc.) vers la capsule **Live Update** d'Android 16
(`Notification.ProgressStyle` + `requestPromotedOngoing`).

## Comment ça marche

1. `MediaListenerService` est un `NotificationListenerService`. Une fois la
   permission "Accès aux notifications" accordée par l'utilisateur, il a le
   droit d'appeler `MediaSessionManager.getActiveSessions()`.
2. Cet appel renvoie un `MediaController` par app qui expose une session
   média — sans que l'app source doive rien faire de spécial.
3. Le service écoute les changements de métadonnées / état de lecture sur
   chaque contrôleur, choisit le "meilleur" (celui qui joue vraiment) et
   transmet titre / artiste / position à `CapsuleNotifier`.
4. `CapsuleNotifier` construit une notif `ProgressStyle` marquée
   `requestPromotedOngoing`, ce qui la fait apparaître en capsule dans la
   status bar.

## État actuel — TODO

- [ ] Vérifier l'API finale de `Notification.ProgressStyle` sur Android 16
      stable (elle a bougé entre les bêtas — `setProgress`,
      `setProgressPoints`/`setProgressSegments` à confirmer)
- [ ] Icône de l'app + icône de notif propre (actuellement icône système
      placeholder)
- [ ] Pochette d'album en `setLargeIcon`
- [ ] Gérer proprement le cas 0 session active (déjà fait : `clear()`)
- [ ] Tester le comportement avec plusieurs apps musique simultanées
- [ ] Ajouter `POST_PROMOTED_NOTIFICATIONS` au runtime check
      (`canPostPromotedNotifications()`) avant de tenter la promotion

## Permissions requises

- Accès aux notifications (`BIND_NOTIFICATION_LISTENER_SERVICE`) — à accorder
  manuellement dans les réglages, l'app renvoie directement vers l'écran
  concerné.
- `POST_NOTIFICATIONS`, `POST_PROMOTED_NOTIFICATIONS` — demandées via le
  manifest.

Projet **rocknite-studio**.
