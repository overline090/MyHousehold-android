# Architecture de l'application

## Où en est le code

| Partie | État |
|---|---|
| `core` — domaine en Kotlin pur : sortes de tâches, priorités, cinq formes de répétition, état d'une tâche de fond, tour de rôle | ✅ écrit et testé |
| `app` — base locale Room, accueil, liste des tâches, formulaire | ✅ écrit |
| Agenda, foyer, réglages, profil | · à écrire |
| Connexion à un serveur, synchronisation, mises à jour hors magasin | · à écrire |

Le SDK Android ne s'installe pas partout : c'est l'intégration continue
(`.github/workflows/android.yml`) qui construit l'APK et fait tourner les tests. Le
module `core`, lui, se compile et se teste sans Android — c'est pour cela qu'il
existe.

## Deux modules

```
core/   Kotlin pur. Les règles du foyer, sans Android ni réseau.
app/    L'application : écrans Compose, base locale, client du serveur.
```

La séparation n'est pas un goût d'architecte : les règles du foyer sont **les mêmes**
des deux côtés du réseau. Le tour de rôle, par exemple, se déduit du rang d'une
occurrence dans sa série, jamais d'un curseur — l'app désigne donc la même personne
que le serveur sans avoir à le lui demander.

## Hors-ligne d'abord

C'est la décision qui structure tout le reste.

> **L'interface lit toujours la base locale. Elle n'attend jamais le réseau.**

Un foyer utilise cette app dans une cuisine, un garage, un sous-sol, en vacances. Une
app qui affiche un *spinner* pour cocher « poubelles sorties » ne sera pas utilisée.
Room n'est donc pas un cache : c'est **la** source de vérité de l'interface, et le
réseau la met à jour en arrière-plan.

```
┌──────────────────────────────────────────────────────┐
│  UI — Jetpack Compose                                │
│  observe un StateFlow, ne connaît ni Room ni Retrofit│
└───────────────────────┬──────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────┐
│  ViewModel                                           │
│  état d'écran, actions utilisateur                   │
└───────────────────────┬──────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────┐
│  Repository                                          │
│  lit Room · écrit Room + empile une mutation         │
└──────────┬───────────────────────────┬───────────────┘
           │                           │
┌──────────▼──────────┐    ┌───────────▼───────────────┐
│  Room               │    │  File de mutations        │
│  (source de vérité) │    │  (table locale persistée) │
└─────────────────────┘    └───────────┬───────────────┘
                                       │
                           ┌───────────▼───────────────┐
                           │  WorkManager → API REST   │
                           └───────────────────────────┘
```

## Écriture optimiste

Cocher une tâche déclenche, dans l'ordre, **une seule transaction locale** :

1. l'occurrence passe à `DONE` dans Room → l'UI se met à jour instantanément ;
2. une mutation `COMPLETE_OCCURRENCE` est ajoutée à la file.

Les deux dans la même transaction : sinon un *crash* entre les deux laisse une tâche
cochée localement que le serveur ne verra jamais.

Puis WorkManager envoie la file. Trois issues possibles :

| Résultat serveur | Effet dans l'app |
|---|---|
| `APPLIED` / `DUPLICATE` | La mutation sort de la file. Rien à l'écran. |
| `REJECTED` | L'état serveur remplace l'état local, et un bandeau explique (« déjà fait par Marie »). |
| Réseau indisponible | La mutation reste en file, nouvelle tentative avec *backoff*. |

Un `DUPLICATE` est un **succès** : il signifie que la mutation était déjà passée. Le
traiter comme une erreur ferait apparaître des alertes à chaque reprise de réseau.

## Indicateur de synchronisation

Un seul indicateur global, discret, dans la barre du haut :

| État | Affichage |
|---|---|
| Synchronisé | Rien. L'absence de message est le message. |
| N mutations en attente | Petite icône + compteur |
| Échec persistant (> 1 h) | Bandeau avec un bouton « Réessayer » |
| Hors-ligne | Icône barrée, **sans** blocage de l'interface |

Pas d'indicateur par élément : signaler chaque ligne non synchronisée transforme
l'écran en sapin de Noël et inquiète sans raison.

## Un serveur, un foyer

Il n'y a pas de sélecteur de foyer, et les requêtes locales n'en portent pas
l'identifiant : une machine sert un seul foyer, donc un téléphone connecté à un
serveur n'en voit qu'un. Tant qu'aucun serveur n'est lié, le foyer est **local** :
la base du téléphone est le foyer.

## Statuts et droits

L'app **affiche** le statut d'un membre et les droits qu'il porte ; elle n'offre
aucun écran pour les modifier. Ce n'est pas une limite technique — le serveur ne sait
pas qui l'appelle — mais une décision de produit : un réglage qui engage la sécurité
du foyer se fait assis, sur l'interface web du serveur. L'app le dit, plutôt que de
présenter un bouton grisé.

## Le temps

Trois règles, qui évitent l'essentiel des bugs de ce type d'app :

1. **Toujours afficher dans le fuseau du foyer**, pas celui du téléphone. En voyage,
   « la tâche de lundi 19h » reste 19h à la maison.
2. **Se méfier de l'horloge du téléphone** pour calculer un écoulement. Quand un
   serveur est lié, il renvoie `elapsedSeconds` en plus de la date : l'app mesure sa
   dérive et l'applique. Sans serveur, elle se rabat sur l'horloge locale, et un
   écoulement négatif est ramené à zéro plutôt que d'afficher « il y a −3 jours ».
3. **Les jours purs restent des dates.** `scheduledFor` est un `LocalDate`, jamais un
   instant : le convertir décale d'un jour selon le fuseau.

## Notifications

Un push ne contient aucune donnée métier, seulement `{"type":"sync"}`. Il déclenche
un delta, et c'est le **contenu local** qui produit la notification affichée. Le
contenu du foyer ne transite donc pas par les serveurs de Google.

Deux variantes de build :

- `playstore` → FCM ;
- `fdroid` → UnifiedPush, et repli sur une interrogation périodique (15 min) si
  aucun distributeur UnifiedPush n'est installé.

L'interface de notification est abstraite derrière une seule interface, implémentée
deux fois : aucun `if (variant)` dans le code métier.

## Choix techniques et alternatives écartées

| Choix | Pourquoi | Écarté |
|---|---|---|
| **Compose** | Un état observable rendu automatiquement : exactement ce que demande une UI pilotée par une base locale qui change en arrière-plan. | XML + Fragments — synchronisation manuelle de l'état, beaucoup plus verbeux. |
| **Room** | SQL, migrations versionnées, `Flow` réactif. | SQLDelight (bon, mais Room est mieux documenté, donc plus accessible aux contributeurs) ; un simple cache mémoire (perd tout au redémarrage). |
| **WorkManager** | Survit à la fermeture de l'app et aux redémarrages, respecte les contraintes de batterie d'Android. | Un service au premier plan — consommation injustifiée et notification permanente. |
| **Ktor client** | Le même langage que `core`, et une API qui se lit sans annotation ni génération de code. | Retrofit — très bien, mais il tire une génération de code pour un contrat de quelques routes. |
| **Un conteneur de dépendances à la main** | À cette taille, une bibliothèque d'injection coûterait plus de configuration qu'elle n'en ferait gagner, et un contributeur voit en un fichier ce dont l'app est faite. | Hilt (génération de code, temps de compilation) ; Koin (erreurs au *runtime*). |
| **Un module `core` sans Android** | Les règles du foyer se testent partout, y compris là où le SDK Android n'est pas installé — et elles restent les mêmes que côté serveur. | Tout mettre dans `app` — les règles deviendraient intestables sans émulateur. |
| **API 26 minimum** | Couvre > 95 % des appareils actifs et donne `java.time` sans rétroportage. | API 21 — le rétroportage des dates pour une app entièrement temporelle n'en vaut pas le coût. |
| **Mutations typées** | Une file de mutations explicites (`COMPLETE_OCCURRENCE`, …) se relit et se rejoue. | Un journal de *diffs* génériques — impossible à déboguer quand un conflit survient. |

## Tests

| Niveau | Portée | Outil |
|---|---|---|
| Unitaire | ViewModels, résolution des conflits, dérive d'horloge | JUnit + Turbine |
| Base | Migrations Room, requêtes | Room testing + Robolectric |
| UI | Parcours « cocher une tâche hors-ligne puis synchroniser » | Compose UI test |

Le scénario à ne jamais casser, et donc à tester en premier : **mode avion → cocher
trois tâches → fermer l'app → la rouvrir → rétablir le réseau → tout doit remonter
exactement une fois.**
