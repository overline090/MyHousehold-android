# Architecture de l'application

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

## Multi-foyer

Un utilisateur peut appartenir à plusieurs foyers (famille + colocation, garde
alternée). Le foyer actif est stocké dans DataStore et toutes les requêtes Room sont
filtrées dessus. Le sélecteur existe même avec un seul foyer — l'ajouter après coup
demanderait de reprendre toutes les requêtes.

## Le temps

Trois règles, qui évitent l'essentiel des bugs de ce type d'app :

1. **Toujours afficher dans le fuseau du foyer**, pas celui du téléphone. En voyage,
   « la tâche de lundi 19h » reste 19h à la maison.
2. **Ne jamais se fier à l'horloge du téléphone** pour calculer un écoulement. Le
   serveur renvoie `serverTime` et `elapsedSeconds` ; l'app mesure sa dérive et
   l'applique. Un téléphone mal réglé afficherait sinon « dans 2 jours » pour une
   tâche en retard.
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
| **Retrofit** | Standard, simple à relire pour un contributeur occasionnel. | Ktor client — cohérent avec un serveur Kotlin, mais le serveur est en TypeScript : l'argument tombe. |
| **Hilt** | Injection standard Android, peu de code répétitif. | Koin (erreurs au *runtime* plutôt qu'à la compilation) ; aucune injection (rend les tests pénibles). |
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
