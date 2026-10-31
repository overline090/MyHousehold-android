# Écrans

Ce document décrit ce que chaque écran doit permettre, pas son apparence exacte. Il
sert de référence commune avant de coder, et à vérifier qu'aucun besoin du projet
n'est sans interface.

Il suit le modèle décidé côté serveur
([`data-model.md`](https://github.com/overline090/MyHousehold/blob/main/docs/data-model.md)) :
un serveur sert **un seul foyer**, **tout est une tâche**, les droits viennent du
**statut** du compte, et il n'y a **pas de points**.

## Principes directeurs

> L'action la plus fréquente doit être la plus rapide.

L'action la plus fréquente dans un foyer, c'est **déclarer qu'une chose est faite**.
Elle doit être atteignable depuis l'écran d'accueil, sans ouvrir de détail, sans
confirmation.

> Pas de sous-titre.

Aucun écran ne porte de ligne d'explication sous son titre — pas de « 4 membres ·
Europe/Paris » sous « Maison des Dupont », pas de « Depuis combien de temps ? » sous
« Tâches de fond ». Un sous-titre répète ce que le contenu montre déjà, et occupe la
place de la première ligne utile. Ce qui compte vraiment se dit dans le contenu, à
l'endroit où il s'applique.

> La configuration se fait au navigateur.

Les **statuts et leurs droits**, comme la configuration générale du foyer, ne se
modifient que depuis l'interface web du serveur. L'app les **affiche** et n'offre
aucun écran pour les changer : pas de bouton grisé, pas d'option cachée, une phrase
qui dit où cela se fait.

---

## 1. Première ouverture

Trois chemins, et le premier doit être le plus court :

```
┌──────────────────────────────────────────┐
│  Commencer seul                          │  ← sans serveur, immédiat
│  Rejoindre un foyer (code d'invitation)  │
│  Connecter mon serveur                   │
└──────────────────────────────────────────┘
```

**Commencer seul** crée un foyer local et ouvre l'app : aucune adresse à saisir,
aucun compte à créer. C'est le chemin par défaut, parce qu'exiger un serveur avant de
pouvoir essayer l'app condamne l'app. Voir [`standalone.md`](standalone.md).

Un bandeau discret, non bloquant, rappelle ensuite que le foyer est local et qu'un
serveur permettra de le partager.

### Connexion à un serveur

Il n'y a pas de serveur par défaut : l'utilisateur saisit l'URL de **son** instance,
ou scanne le QR code affiché par l'assistant d'installation du serveur.

- Champ URL, avec `https://` prérempli.
- Bouton « Vérifier » → appelle `GET /health` et affiche la version du serveur. On
  valide l'adresse avant de demander un mot de passe, sinon une faute de frappe
  ressemble à un mauvais mot de passe.
- Erreurs distinguées explicitement : *nom introuvable*, *certificat invalide*,
  *serveur injoignable*, *ce n'est pas un serveur MyHousehold*. Un message unique
  « échec de connexion » est inexploitable par l'utilisateur.
- Avertissement en cas de `http://` : les jetons circuleraient en clair.

Puis : connexion, ou création de compte si le serveur l'autorise, ou saisie d'un code
d'invitation. Un serveur ne servant qu'un foyer, il n'y a rien à choisir après la
connexion.

## 2. Accueil

L'écran par défaut, celui qu'on ouvre dix fois par jour. Il répond à deux questions,
dans cet ordre : *qu'est-ce que je dois faire ?* puis *qu'est-ce qui arrive cette
semaine ?*

| Section | Contenu | Visible si |
|---|---|---|
| En retard | Tâches `MISSED`, tâches de fond `OVERDUE`, tâches ponctuelles dont la date limite est passée | il y en a |
| Mes tâches du jour | Occurrences assignées à moi, échéance aujourd'hui | toujours |
| Rendez-vous de la semaine | Les sept jours qui viennent, groupés par jour, aujourd'hui en premier | il y en a |
| À faire par les autres | Repliable, replié par défaut | il y en a |
| À surveiller | Tâches de fond `DUE_SOON` | il y en a |

Les **rendez-vous de la semaine** remplacent la liste du seul jour courant : un foyer
s'organise à la semaine, et un rendez-vous de mercredi découvert mercredi matin est
découvert trop tard. Chaque jour n'affiche son nom qu'une fois, et l'on va à l'agenda
complet par la dernière ligne de la section.

Interactions :

- **Tape sur la case** → coché. Immédiat, sans dialogue, avec un `Snackbar`
  « Annuler » pendant 5 secondes.
- **Tape sur la ligne** → détail.
- **Appui long** → réassigner, reporter, annoter (droit `MANAGE_TASKS`).
- **Tirer pour rafraîchir** → force une synchro.

Les sections vides disparaissent. Un écran d'accueil qui affiche cinq titres vides
donne l'impression d'une app cassée.

Si tout est fait : un état vide explicite et chaleureux, pas une page blanche.

## 3. Tâches

**Tout est une tâche**, mais pas toutes de la même manière. Un sélecteur en haut de
l'écran filtre la même liste :

| Filtre | Contenu |
|---|---|
| Tout | Les tâches actives, triées par priorité puis par échéance |
| À faire | Ce qui est dû aujourd'hui ou en retard, et les ponctuelles pas encore faites |
| Faites | Ce qui a été fait aujourd'hui (et les ponctuelles faites) |
| Récurrentes | `SCHEDULED` — « chaque lundi à 19 h » |
| De fond | `BACKGROUND` — voir la section suivante |
| Ponctuelles | `ONE_OFF` — une seule fois, avec ou sans date limite |
| Groupes | La liste pliée par groupe de tâches |

Chaque ligne affiche son titre, son échéance en langage naturel (« chaque lundi à
19 h » — jamais `FREQ=WEEKLY;BYDAY=MO`), son groupe s'il en a un, et son assigné.
Une **priorité différente de normale** est marquée ; une priorité normale ne porte
aucune étiquette, sinon tout est étiqueté et rien ne ressort.

Chaque ligne dit aussi **où en est la tâche** : faite (à quelle heure, par qui), à
faire aujourd'hui (et pour qui), en retard de N jours, prochaine fois, ou « commence
le… » pour une série qui démarre plus tard. Un **rond** la valide d'un appui ; un
second appui annule une validation faite par erreur.

### Une tâche ouverte

Toucher une tâche ouvre sa fiche : son état en grand avec **C'est fait**, **Passer
cette fois** (récurrentes) ou **Annuler : pas encore faite**, ses **détails**, **les
dernières fois** (faite, passée ou manquée — une échéance restée à faire quand la
suivante est arrivée —, quand, par qui, en retard ou non) et les prochaines fois.
« Modifier » ouvre le formulaire.

### Les détails

Du texte mis en forme : gras, italique, souligné, barré, couleur et surligneur de la
palette, listes à puces ou numérotées, cases à cocher (« ☐ » en tête d'un élément).
À la lecture, les **téléphones** (appeler, copier), les **adresses** (ouvrir dans la
carte, copier) et les **liens** se reconnaissent d'eux-mêmes ; les règles de copie
automatique définies sur le serveur s'y ajouteront avec la synchronisation. Les
détails s'échangent avec le serveur en HTML restreint, que le serveur nettoie.

### Priorités

Sept niveaux, centrés sur « normale ». Les niveaux **négatifs** ne sont pas des
tâches sans importance : ce sont des tâches qui cèdent le passage, et qui restent
dans la liste sans noyer ce qui compte aujourd'hui.

| Valeur | Libellé | À l'écran |
|---|---|---|
| +3 | Urgente | Étiquette pleine, rouge |
| +2 | Très haute | Étiquette, rouge clair |
| +1 | Haute | Étiquette, accent |
| 0 | Normale | Rien |
| −1 | Basse | Étiquette grise |
| −2 | Très basse | Étiquette grise |
| −3 | En dernier | Étiquette grise, titre en gris |

### Groupes de tâches

Un groupe rassemble des tâches qui vont ensemble — « Cuisine », « Voiture », « Avant
les vacances ». Une tâche appartient **à un groupe au plus** : les appartenances
multiples transforment une liste de corvées en système de classement, et personne ne
range ses corvées.

Le groupe porte une couleur, reprise en liste comme repère, et son écran montre ses
tâches avec le compte de ce qui reste à faire.

### Créer ou modifier une tâche

Le formulaire difficile du projet. Il doit rester compréhensible sans connaître
iCalendar. Le premier choix est **le type**, parce qu'il décide de la suite du
formulaire :

```
┌────────────┬────────────┬────────────┐
│ Récurrente │  De fond   │ Ponctuelle │
└────────────┴────────────┴────────────┘
```

| Champ | Interface | Type |
|---|---|---|
| Titre | Texte, obligatoire | tous |
| Groupe | Choix, facultatif | tous |
| Priorité | Sélecteur à sept crans, « Normale » au centre | tous |
| Qui | *Une personne*, *à tour de rôle* (liste ordonnée, réordonnable), *n'importe qui* | tous |
| Répétition | Cinq formes, ci-dessous | récurrente |
| Heure | Sélecteur, ou « toute la journée » | récurrente |
| Délai de grâce | Curseur, 0 à 72 h, 24 par défaut, replié | récurrente |
| À refaire dans | Durée : 3 j, 7 j, 14 j, 1 mois, 3 mois, ou libre | de fond |
| Date limite | Date, facultative — « sans date limite » est un choix normal | ponctuelle |

Les **cinq formes de répétition**, et pas davantage : ce sont celles qu'un foyer
formule spontanément.

| Forme | Saisie |
|---|---|
| Tous les jours | tous les *N* jours |
| Jours de la semaine | les sept jours à cocher |
| Dates du mois | les jours du mois à cocher, et « dernier jour » |
| Dates de l'année | une ou plusieurs dates |
| Toutes les N semaines / N mois | nombre + unité, à partir d'une date |

Un **aperçu permanent** sous le formulaire : « Prochaines : lun. 27 oct. (Paul),
lun. 3 nov. (Marie), lun. 10 nov. (Paul) ». C'est lui qui rend la récurrence
compréhensible — personne ne vérifie une règle dans l'abstrait, tout le monde
vérifie trois dates.

Le tour de rôle n'est pas proposé pour une tâche **de fond** : il suppose une suite
d'échéances, et une tâche de fond n'en a pas.

## 4. Tâches de fond

Ce sont des tâches dont l'**impact dure** et qu'il faut refaire dans un délai long
plutôt qu'à une date : changer les draps dans les 14 jours, le filtre à eau dans les
trois mois. C'est ce que la version précédente du projet appelait des « compteurs » ;
ce n'était pas une autre nature d'objet, et deux listes là où le foyer n'en vit qu'une
était une erreur.

Elles ont leur propre présentation, atteinte par le filtre « De fond » de l'écran
Tâches, parce que ce qu'on y lit n'est pas une date mais une durée.

Chaque carte :

- le titre ;
- **le temps écoulé en gros** : « 23 jours » ;
- la dernière fois : « par Marie, le 1er oct. », et le délai imparti ;
- une barre de progression vers l'échéance, colorée selon l'état
  (`OK` / `DUE_SOON` / `OVERDUE`), doublée d'un mot ;
- un bouton **« C'est fait »**, qui repart de zéro.

Le temps écoulé est l'information centrale : c'est exactement la question que
l'utilisateur se pose (« ça fait combien de temps ? »).

Après la complétion : animation vers zéro et `Snackbar` « Annuler ». L'annulation
supprime l'occurrence créée, elle n'en ajoute pas une seconde.

Détail : historique complet, chaque fois avec sa date et son auteur, chacune
corrigeable. On rattrape ainsi une saisie erronée.

**Déclarer une action passée** : appui long sur « C'est fait » → sélecteur de date.
Nécessaire ici, où l'on constate souvent après coup.

Une tâche de fond sans délai imparti n'affiche ni barre ni couleur : elle ne fait que
compter le temps écoulé.

## 5. Agenda

Vue mensuelle avec points de densité, plus la liste du jour sélectionné. Une vue
hebdomadaire est utile mais peut attendre — l'accueil en donne déjà la semaine.

Chaque entrée porte un code visuel distinguant son type : rendez-vous, tâche,
échéance d'une tâche de fond. Les événements importés d'un agenda personnel portent
la couleur de leur propriétaire et une icône d'origine.

Un événement importé n'est pas modifiable : le dire par un bandeau
(« Depuis l'agenda Google de Marie — lecture seule »), pas par un champ grisé sans
explication.

Création d'un rendez-vous (droit `MANAGE_APPOINTMENTS`) : titre, date/heure, lieu,
participants, visibilité (foyer / privé), rappel.

## 6. Mon agenda personnel

**Chaque compte lie son propre agenda externe** : il appartient à la personne, pas au
foyer, et elle peut le retirer à tout moment.

Liste de mes comptes connectés, avec pour chacun son état de synchronisation et sa
dernière erreur éventuelle.

Ajouter un compte propose les trois voies, dans cet ordre, du plus simple au plus
puissant :

1. **URL iCal** — « collez l'adresse privée fournie par votre agenda ». Avertir du
   délai de rafraîchissement du fournisseur (8 à 24 h) : sans cela, l'utilisateur
   conclura à un bug.
2. **CalDAV** — URL, identifiant, mot de passe. Pour Nextcloud et assimilés.
3. **Google** — ouvre le consentement OAuth dans le navigateur. Nécessite que
   l'hébergeur ait configuré ses identifiants : si ce n'est pas le cas, l'option est
   masquée avec un lien vers la documentation plutôt que présentée puis en échec.

Par compte : un interrupteur **« Masquer les titres »** (`busyOnly`), **activé par
défaut**, expliqué en une phrase : « Votre foyer verra que vous êtes occupé, pas
pourquoi. »

Supprimer un compte retire aussi ses événements importés, et le dit avant de le faire.

## 7. Foyer

Liste des membres : photo, prénom et nom, statut. Avec le droit `MANAGE_MEMBERS` :
inviter (génère un code partageable) et changer le statut d'un membre.

Le statut dit ce que la personne peut faire. **Les statuts eux-mêmes, et leurs
droits, se configurent depuis l'interface web du serveur** — l'écran le dit en une
ligne et donne l'adresse, plutôt que de proposer un écran qui n'existera pas.

**Pas de bouton « exclure ».** On ne peut pas sortir quelqu'un du foyer : l'écran ne
doit pas laisser croire le contraire, ni par un bouton grisé, ni par une option
cachée derrière un appui long. La seule sortie est la suppression de son propre
compte, dans les réglages.

Onglet « Contributions » : par membre, sur une période, le **nombre de tâches
faites**. Il n'y a pas de points, et **pas de classement, pas de podium, pas de
comparaison mise en avant**. Des faits, consultables, sans jugement — un outil de
foyer qui désigne un mauvais élève crée des conflits au lieu d'en résoudre.

## 8. Réglages

- **Mon profil** : photo, prénom, nom, date de naissance, langue. Le **statut** y est
  affiché, non modifiable : personne ne change son propre statut, et les statuts se
  configurent au navigateur.
- Notifications : canaux, heures de silence, rappel par défaut.
- Synchronisation : état, dernière synchro, nombre de mutations en attente,
  « Synchroniser maintenant », et un bouton de diagnostic qui copie l'état technique
  (sans jeton) pour un rapport de bug.
- Appareils connectés, révocables.
- Se déconnecter.
- **Mises à jour** (variante hors magasin) : version installée, version disponible,
  notes de version, taille, puis *Télécharger* et *Installer*. Jamais d'installation
  automatique : installer une application est le geste de l'utilisateur.
- **Connecter un serveur** : adresse ou QR code, puis un récapitulatif de ce qui
  sera envoyé et de ce qui restera local — on ne pousse pas des années d'historique
  familial sans le dire.
- **Supprimer mon compte.** Irréversible, donc : écran dédié et non une ligne de
  liste, mot de passe exigé, et une explication en clair de ce qui se passe —
  l'historique du foyer est conservé sous « Ancien membre », les agendas connectés
  et les appareils sont effacés, et si l'on est le dernier compte à administrer le
  foyer, l'administration passe à quelqu'un d'autre, nommé à l'écran. Nommer le
  successeur avant de confirmer évite la mauvaise surprise.
- À propos : version, licence, lien vers les dépôts.

Un compte portant `MANAGE_SERVER` voit en plus la liste des comptes du serveur, avec
la possibilité d'en supprimer un. C'est le seul moyen de retirer un compte dont le
titulaire n'est plus joignable. Le reste de l'administration est au navigateur.

## Accessibilité

- Toutes les cibles tactiles ≥ 48 dp.
- **Jamais la couleur seule** pour indiquer un état : l'état d'une tâche de fond
  porte aussi un mot, et une priorité porte son libellé.
- Description de contenu sur chaque icône d'action, et sur chaque photo de profil
  (« Photo de Marie Dupont »).
- Mise en page vérifiée jusqu'à 200 % de taille de police. Les cartes de tâches de
  fond sont le point de rupture probable.
- Thème sombre et thème clair, couleurs dynamiques Material You quand disponibles.

## États à ne pas oublier

Ils représentent la moitié du travail d'interface réel :

| État | Attendu |
|---|---|
| Chargement initial | Squelettes, pas un *spinner* plein écran |
| Aucune donnée | Message qui explique quoi faire ensuite |
| Hors-ligne | Bandeau discret, interface pleinement utilisable |
| Erreur de synchro | Message compréhensible + bouton « Réessayer » |
| Conflit | Explication de ce qui s'est passé, pas un code d'erreur |
| Session expirée | Reconnexion sans perdre les mutations en attente |
| Droit retiré en cours de route | Les actions concernées disparaissent, avec une phrase qui dit pourquoi — pas un `403` affiché tel quel |
| Compte supprimé par un administrateur | Message clair, retour à la première ouverture |
