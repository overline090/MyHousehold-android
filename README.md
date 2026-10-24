# MyHousehold — Application Android

Client Android de [MyHousehold](https://github.com/overline090/MyHousehold), un
assistant de gestion du quotidien pour un foyer : tâches récurrentes, compteurs de
temps, rendez-vous et synchronisation des agendas personnels.

> ⚠️ **Projet en phase de conception.** Aucun code n'est encore écrit. Ce dépôt
> contient la spécification de l'application : architecture, écrans, stratégie
> hors-ligne. Les contributions sur la conception sont aussi bienvenues que celles
> sur le code.

## Ce que l'app doit faire

- Afficher **ce qui compte aujourd'hui** : mes tâches, les compteurs en retard, les
  rendez-vous du jour.
- Cocher une tâche en deux tapes, depuis la liste, **sans ouvrir de détail**.
- Remettre un compteur à zéro en un geste.
- Montrer l'agenda du foyer, incluant les événements importés des agendas personnels.
- **Fonctionner sans réseau** et se réconcilier ensuite.

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
| Réseau | Retrofit + kotlinx.serialization |
| Synchronisation | WorkManager |
| Injection | Hilt |
| Préférences | DataStore |
| Push | FCM, et UnifiedPush pour la distribution hors Play Store |
| Minimum | Android 8.0 (API 26) |

Les raisons de ces choix sont dans
[`docs/app-architecture.md`](docs/app-architecture.md).

## Documentation

| Document | Contenu |
|---|---|
| [`docs/app-architecture.md`](docs/app-architecture.md) | Couches, hors-ligne d'abord, choix techniques |
| [`docs/screens.md`](docs/screens.md) | Chaque écran, son rôle, ses interactions |
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
