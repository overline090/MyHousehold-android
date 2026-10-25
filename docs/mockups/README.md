# Maquettes de l'application

Les écrans décrits dans [`../screens.md`](../screens.md), dessinés en HTML pour
pouvoir être rendus, mesurés et corrigés — plutôt qu'en image, qu'on ne peut que
regarder.

```bash
node docs/mockups/rendre.js    # écrit les PNG dans docs/mockups/png/
```

Le script rend chaque écran à **390 × 844** (un téléphone courant) et signale tout
débordement : une maquette qui déborde ment sur ce qu'elle promet.

| Fichier | Écran |
|---|---|
| `1-aujourdhui.html` | L'écran d'accueil : ce qui compte aujourd'hui |
| `2-taches.html` | Les tâches récurrentes du foyer |
| `3-nouvelle-tache.html` | Le formulaire de récurrence, avec son aperçu des trois prochaines fois |
| `4-compteurs.html` | Les compteurs de temps |
| `5-agenda.html` | Le mois, et le jour choisi |
| `6-foyer.html` | Membres et contributions |
| `7-reglages.html` | Réglages, synchronisation, mises à jour |
| `8-supprimer-compte.html` | Supprimer son compte — la seule sortie d'un foyer |
| `9-connecter-serveur.html` | Adopter un serveur : ce qui part, ce qui reste |

## Ce que ces maquettes décident

- **Une barre de navigation à cinq destinations** : Aujourd'hui, Tâches, Compteurs,
  Agenda, Foyer. C'est une proposition, pas une exigence du projet.
- **Cocher se fait depuis la liste**, sans ouvrir de détail : c'est l'action la plus
  fréquente du foyer, elle doit être la plus rapide.
- **Le temps écoulé est l'information centrale d'un compteur**, en très gros — c'est
  exactement la question qu'on se pose.
- **L'aperçu des trois prochaines fois** sur le formulaire de récurrence : personne ne
  vérifie une règle dans l'abstrait, tout le monde vérifie trois dates.
- **Les contributions sans classement** : des faits, pas un palmarès.
- **Le successeur du foyer est nommé avant** de confirmer une suppression de compte.
- **Aucun bouton pour retirer quelqu'un** d'un foyer, nulle part.

Ces maquettes sont à valider avant d'écrire la première ligne d'interface.
