# Écrans

Ce document décrit ce que chaque écran doit permettre, pas son apparence exacte. Il
sert de référence commune avant de coder, et à vérifier qu'aucun besoin du projet
n'est sans interface.

## Principe directeur

> L'action la plus fréquente doit être la plus rapide.

L'action la plus fréquente dans un foyer, c'est **déclarer qu'une chose est faite**.
Elle doit être atteignable depuis l'écran d'accueil, sans ouvrir de détail, sans
confirmation.

---

## 1. Première ouverture — connexion au serveur

Il n'y a pas de serveur par défaut : l'utilisateur saisit l'URL de **son** instance.

- Champ URL, avec `https://` prérempli.
- Bouton « Vérifier » → appelle `GET /health` et affiche la version du serveur. On
  valide l'adresse avant de demander un mot de passe, sinon une faute de frappe
  ressemble à un mauvais mot de passe.
- Erreurs distinguées explicitement : *nom introuvable*, *certificat invalide*,
  *serveur injoignable*, *ce n'est pas un serveur MyHousehold*. Un message unique
  « échec de connexion » est inexploitable par l'utilisateur.
- Avertissement en cas de `http://` : les jetons circuleraient en clair.

Puis : connexion, ou création de compte si le serveur l'autorise, ou saisie d'un code
d'invitation.

## 2. Aujourd'hui (écran d'accueil)

L'écran par défaut, celui qu'on ouvre dix fois par jour.

| Section | Contenu | Visible si |
|---|---|---|
| En retard | Tâches `MISSED` et compteurs `OVERDUE` | il y en a |
| Mes tâches du jour | Occupations assignées à moi, échéance aujourd'hui | toujours |
| Rendez-vous du jour | Agenda fusionné, heure à heure | il y en a |
| À faire par les autres | Repliable, replié par défaut | il y en a |
| Compteurs bientôt dus | `DUE_SOON` | il y en a |

Interactions :

- **Tape sur la case** → coché. Immédiat, sans dialogue, avec un `Snackbar`
  « Annuler » pendant 5 secondes.
- **Tape sur la ligne** → détail.
- **Appui long** → réassigner, reporter, annoter.
- **Tirer pour rafraîchir** → force une synchro.

Les sections vides disparaissent. Un écran d'accueil qui affiche cinq titres vides
donne l'impression d'une app cassée.

Si tout est fait : un état vide explicite et chaleureux, pas une page blanche.

## 3. Tâches

Onglet « Toutes » (définitions actives) et onglet « Historique » (occurrences
passées, filtrables par membre et par période).

Chaque définition affiche son titre, sa règle en langage naturel (« chaque lundi à
19h » — jamais `FREQ=WEEKLY;BYDAY=MO`), le prochain assigné et la prochaine échéance.

### Créer ou modifier une tâche

Le formulaire difficile du projet. Il doit rester compréhensible sans connaître
iCalendar.

| Champ | Interface |
|---|---|
| Titre | Texte, obligatoire |
| Fréquence | Choix guidé : *tous les jours*, *jours choisis*, *toutes les N semaines*, *jour du mois*, *personnalisé* |
| Heure | Sélecteur, ou « toute la journée » |
| Qui | *Une personne*, *à tour de rôle* (liste ordonnée, réordonnable), *n'importe qui* |
| Délai de grâce | Curseur, 0 à 72 h, 24 par défaut |
| Points | Optionnel, replié par défaut |

Un **aperçu permanent** sous le formulaire : « Prochaines : lun. 27 oct. (Paul),
lun. 3 nov. (Marie), lun. 10 nov. (Paul) ». C'est lui qui rend la récurrence
compréhensible — personne ne vérifie une règle dans l'abstrait, tout le monde
vérifie trois dates.

« Personnalisé » expose la RRULE brute, pour les cas rares, et reste hors du chemin
principal.

## 4. Compteurs

Une grille de cartes, parce qu'un compteur se lit d'un coup d'œil.

Chaque carte :

- le titre ;
- **le temps écoulé en gros** : « 23 jours » ;
- la dernière personne : « par Marie, le 1er oct. » ;
- une barre de progression vers `targetInterval`, colorée selon l'état
  (`OK` / `DUE_SOON` / `OVERDUE`) ;
- un bouton **« C'est fait »** qui remet à zéro.

Le temps écoulé est l'information centrale : c'est exactement la question que
l'utilisateur se pose (« ça fait combien de temps ? »).

Après la remise à zéro : animation vers zéro et `Snackbar` « Annuler ». L'annulation
supprime l'événement créé, elle n'en ajoute pas un second.

Détail d'un compteur : historique complet des événements, chacun avec sa date et son
auteur, chacun supprimable. On corrige ainsi une saisie erronée.

**Déclarer une action passée** : appui long sur « C'est fait » → sélecteur de date.
Nécessaire pour les compteurs, où l'on constate souvent après coup.

Un compteur sans `targetInterval` n'affiche aucune barre ni couleur : il ne fait que
compter.

## 5. Agenda

Vue mensuelle avec points de densité, plus la liste du jour sélectionné. Une vue
hebdomadaire est utile mais peut attendre.

Chaque entrée porte un code visuel distinguant son type : rendez-vous, tâche,
échéance de compteur. Les événements importés d'un agenda personnel portent la
couleur de leur propriétaire et une icône d'origine.

Un événement importé n'est pas modifiable : le dire par un bandeau
(« Depuis l'agenda Google de Marie — lecture seule »), pas par un champ grisé sans
explication.

Création d'un rendez-vous : titre, date/heure, lieu, participants, visibilité
(foyer / privé), rappel.

## 6. Mon agenda personnel

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

Liste des membres avec leur rôle. Pour un `OWNER` : inviter (génère un code
partageable) et changer un rôle.

**Pas de bouton « exclure ».** On ne peut pas sortir quelqu'un d'un foyer : l'écran
ne doit pas laisser croire le contraire, ni par un bouton grisé, ni par une option
cachée derrière un appui long. La seule sortie est la suppression de son propre
compte, dans les réglages.

Onglet « Contributions » : par membre, sur une période, le nombre de tâches faites et
les points. **Pas de classement, pas de podium, pas de comparaison mise en avant.**
Des faits, consultables, sans jugement — un outil de foyer qui désigne un mauvais
élève crée des conflits au lieu d'en résoudre.

Réglages du foyer (`OWNER`) : nom, fuseau horaire, premier jour de la semaine.

## 8. Réglages

- Profil : nom affiché, couleur, langue.
- Notifications : canaux, heures de silence, rappel par défaut.
- Synchronisation : état, dernière synchro, nombre de mutations en attente,
  « Synchroniser maintenant », et un bouton de diagnostic qui copie l'état technique
  (sans jeton) pour un rapport de bug.
- Appareils connectés, révocables.
- Changer de foyer, se déconnecter.
- **Supprimer mon compte.** Irréversible, donc : écran dédié et non une ligne de
  liste, mot de passe exigé, et une explication en clair de ce qui se passe —
  l'historique du foyer est conservé sous « Ancien membre », les agendas connectés
  et les appareils sont effacés, et si l'on est le dernier propriétaire d'un foyer,
  la propriété passe à quelqu'un d'autre (nommé à l'écran) ou le foyer est supprimé.
  Nommer le successeur avant de confirmer évite la mauvaise surprise.
- À propos : version, licence, lien vers les dépôts.

Un compte administrateur de l'instance voit en plus la liste des comptes du serveur,
avec la possibilité d'en supprimer un. C'est le seul moyen de retirer un compte dont
le titulaire n'est plus joignable.

## Accessibilité

- Toutes les cibles tactiles ≥ 48 dp.
- **Jamais la couleur seule** pour indiquer un état : les statuts des compteurs
  portent aussi une icône et un texte.
- Description de contenu sur chaque icône d'action.
- Mise en page vérifiée jusqu'à 200 % de taille de police. Les cartes de compteurs
  sont le point de rupture probable.
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
| Membre exclu du foyer | Message clair, retour au sélecteur de foyer |
