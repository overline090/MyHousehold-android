# MyHousehold — Application Android

Client Android de [MyHousehold](https://github.com/overline090/MyHousehold), un
assistant de gestion du quotidien pour un foyer : tâches récurrentes, tâches de fond,
rendez-vous et synchronisation des agendas personnels.

> ⚠️ **v0.1, utilisable seule.** On installe l'app, on crée son foyer, on ajoute des
> tâches et on les coche — sans serveur. Restent à écrire : la connexion à un
> serveur, la synchronisation, les agendas externes, les notifications et les mises
> à jour hors magasin. Voir
> [la feuille de route du serveur](https://github.com/overline090/MyHousehold/blob/main/docs/roadmap.md).

## L'app marche sans serveur

On l'installe, on s'en sert. Le serveur sert à **partager** un foyer, pas à
l'utiliser : tâches, compteurs, rendez-vous et rappels fonctionnent sur le téléphone
seul. Voir [`docs/standalone.md`](docs/standalone.md).

Quand un serveur arrive, l'app lui transmet ce qu'elle a déjà — on ne ressaisit rien.

## Ce que l'app doit faire

- Afficher **ce qui compte aujourd'hui** : mes tâches, ce qui est en retard, et les
  rendez-vous **de la semaine**.
- Cocher une tâche en deux tapes, depuis la liste, **sans ouvrir de détail**.
- Refaire une tâche de fond en un geste, et voir le temps écoulé en gros.
- Montrer l'agenda du foyer, incluant les événements importés des agendas personnels.
- **Fonctionner sans réseau** et se réconcilier ensuite.
- **Fonctionner sans serveur du tout**, et en adopter un plus tard.
- **Détecter ses propres mises à jour** et les télécharger, puisqu'elle est d'abord
  distribuée hors magasin ([`docs/updates.md`](docs/updates.md)).

## Ce que l'app ne fait pas

- Pas de compte chez un tiers : on se connecte à **son** serveur, dont on saisit
  l'URL.
- Pas de télémétrie, pas d'analytics, aucun traqueur.
- Pas de publicité, pas d'achat intégré, pas de version « pro ».
- Pas de bouton pour exclure quelqu'un d'un foyer : cela n'existe pas dans le
  modèle. Chacun peut supprimer son propre compte, c'est tout.

## Stack technique prévue

| Couche | Choix |
|---|---|
| Langage | Kotlin |
| Interface | Jetpack Compose + Material 3 |
| Architecture | MVVM, une couche *repository* |
| Base locale | Room (SQLite) |
| Réseau | Ktor client + kotlinx.serialization *(à venir)* |
| Synchronisation | WorkManager *(à venir)* |
| Injection | Un conteneur assemblé à la main |
| Préférences | DataStore |
| Push | FCM, et UnifiedPush pour la distribution hors Play Store |
| Minimum | Android 8.0 (API 26) |
| Récurrence | Moteur Kotlin dans le module `core`, testé sans Android |

Les raisons de ces choix sont dans
[`docs/app-architecture.md`](docs/app-architecture.md).

## Construire

```bash
./gradlew :core:test          # les règles du foyer, sans SDK Android
./gradlew :app:assembleDebug  # l'APK, avec le SDK Android
```

Le module `core` se compile et se teste **partout** : c'est pour cela qu'il est
séparé. L'APK, lui, demande le SDK Android ; là où il n'est pas installé,
l'intégration continue s'en charge et publie l'APK en artefact de build.

## Documentation

| Document | Contenu |
|---|---|
| [`docs/app-architecture.md`](docs/app-architecture.md) | Couches, hors-ligne d'abord, choix techniques |
| [`docs/screens.md`](docs/screens.md) | Chaque écran, son rôle, ses interactions |
| [`docs/standalone.md`](docs/standalone.md) | Usage sans serveur, et adoption d'un serveur |
| [`docs/updates.md`](docs/updates.md) | Détection et téléchargement des mises à jour |
| [API du serveur](https://github.com/overline090/MyHousehold/blob/main/docs/api.md) | Contrat REST |
| [Protocole de synchro](https://github.com/overline090/MyHousehold/blob/main/docs/sync.md) | Delta, file de mutations, conflits |

## Distribution prévue

1. **APK signé** attaché aux *releases* GitHub — disponible dès la première version
   utilisable.
2. **F-Droid** — la cible : pas de service Google requis, construction
   reproductible depuis les sources.
3. **Play Store** — seulement si quelqu'un veut porter le compte développeur et ses
   obligations. Ce n'est pas une priorité.

La variante F-Droid devra se passer de FCM, d'où UnifiedPush prévu dès la conception
et non ajouté après coup.

## Contribuer

Voir [CONTRIBUTING.md](CONTRIBUTING.md). Les contributions les plus utiles
aujourd'hui : relire les écrans décrits dans `docs/screens.md` et dire ce qui
manquerait pour votre propre foyer.

## Licence

[PolyForm Noncommercial License 1.0.0](LICENSE) — usage, modification et partage
libres pour tout usage **non commercial**. Toute exploitation lucrative est interdite.

Required Notice: Copyright 2026 overline090 (https://github.com/overline090/MyHousehold-android)
