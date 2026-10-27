# Maquettes de l'application

Les écrans décrits dans [`../screens.md`](../screens.md), dessinés en HTML pour
pouvoir être rendus, mesurés et corrigés — plutôt qu'en image, qu'on ne peut que
regarder.

```bash
node docs/mockups/injecter.js   # pose la barre de navigation partagée
node docs/mockups/rendre.js     # écrit les PNG dans docs/mockups/png/
```

Le script rend chaque écran à **390 × 844** (un téléphone courant) et signale tout
débordement : une maquette qui déborde ment sur ce qu'elle promet. Il mesure le
conteneur défilant, pas la page — `body` est en `overflow: hidden`, et une maquette
coupée passerait sinon pour correcte.

| Fichier | Écran |
|---|---|
| `1-accueil.html` | L'accueil : ce qui est en retard, mes tâches du jour, les rendez-vous de la semaine |
| `2-taches.html` | Toutes les tâches : récurrentes, de fond, ponctuelles, groupes |
| `3-nouvelle-tache.html` | Le formulaire : type, répétition, priorité, aperçu des trois prochaines fois |
| `4-taches-de-fond.html` | Le filtre « De fond » : ce qu'on y lit est une durée, pas une date |
| `5-agenda.html` | Le mois, et le jour choisi |
| `6-foyer.html` | Membres, statuts, contributions |
| `7-reglages.html` | Profil, synchronisation, mises à jour |
| `8-supprimer-compte.html` | Supprimer son compte — la seule sortie du foyer |
| `10-mon-profil.html` | Photo, prénom, nom, date de naissance, statut |
| `11-volet.html` | Le volet de navigation, ouvert par-dessus l'accueil |
| `12-depart.html` | Première ouverture : serveur trouvé sur le Wi-Fi, scan, adresse, ou sans serveur |
| `13-depart-hors-wifi.html` | La même, hors Wi-Fi : la détection le dit, les autres chemins restent |
| `14-scanner.html` | Scanner le QR code affiché par la page web du serveur |
| `15-adresse.html` | Saisir l'adresse du serveur à la main |
| `16-verifier-serveur.html` | Comparer l'empreinte du serveur — sauf QR code, qui la porte |
| `17-compte-choix.html` | Serveur vérifié : créer son compte ou se connecter |
| `18-creer-compte.html` | Créer son compte avec un code d'invitation |
| `19-premier-compte.html` | Le premier compte du serveur, administrateur (QR code de l'assistant) |
| `20-se-connecter.html` | Se connecter à un compte existant |
| `21-reprise.html` | Reprendre le foyer du téléphone sur un serveur neuf : ce qui sera envoyé |
| `22-reprise-envoyee.html` | Foyer envoyé : la suite se fait sur la page web, puis par le QR code suivant |
| `23-ajout-rapide.html` | Ajout rapide depuis l'accueil : tâche, rendez-vous ou tâche de fond, en deux champs |
| `24-nouveau-rdv.html` | Nouveau rendez-vous, depuis le bouton d'ajout de l'agenda |
| `25-photo.html` | Changer sa photo de profil : prendre, choisir, retirer |
| `26-commencer-seul.html` | Commencer sans serveur : photo et date de naissance facultatives |

## Ce que ces maquettes décident

- **Trois onglets** : Accueil, Tâches, Agenda — ce qu'on ouvre tous les jours. Foyer,
  Mon profil, Serveur, Réglages et À propos vivent dans un **volet de navigation**,
  ouvert par le bouton ☰ de **n'importe quel écran**. Les anciens « compteurs » n'ont
  pas de destination à eux : ce sont des tâches, atteintes par un filtre de l'écran
  Tâches.
- **L'accueil montre la semaine qui vient**, pas le seul jour courant : un rendez-vous
  de mercredi découvert mercredi matin est découvert trop tard.
- **Aucun sous-titre sous un titre d'écran.** Ni « 4 membres · Europe/Paris », ni
  « Depuis combien de temps ? » : un sous-titre répète ce que le contenu montre déjà.
- **Cocher se fait depuis la liste**, sans ouvrir de détail : c'est l'action la plus
  fréquente du foyer, elle doit être la plus rapide.
- **Le temps écoulé est l'information centrale d'une tâche de fond**, en très gros —
  c'est exactement la question qu'on se pose.
- **Une priorité « normale » ne porte aucune étiquette.** Si tout est étiqueté, plus
  rien ne ressort.
- **Le type de tâche est le premier champ du formulaire** : c'est lui qui décide de
  la suite.
- **L'aperçu des trois prochaines fois** sur le formulaire de récurrence : personne ne
  vérifie une règle dans l'abstrait, tout le monde vérifie trois dates.
- **Le statut est affiché, jamais modifiable depuis l'app** : les statuts et leurs
  droits se configurent sur l'interface web du serveur, et l'app le dit plutôt que de
  présenter un écran qui n'existera pas.
- **Les contributions comptent des tâches faites**, sans points et sans classement :
  des faits, pas un palmarès.
- **Le successeur à l'administration du foyer est nommé avant** de confirmer une
  suppression de compte.
- **Aucun bouton pour retirer quelqu'un** du foyer, nulle part.

Dans ces maquettes, un dégradé coloré tient la place d'une photo de profil : ce qui se
décide ici, c'est sa taille et sa forme, pas son contenu.

Ces maquettes sont à valider avant d'écrire la première ligne d'interface.
