# L'app sans serveur

L'application doit être pleinement utilisable **sans serveur** : on l'installe, on
s'en sert, et on n'installe un serveur que si on veut partager son foyer.

> ⚠️ **Spécification.** Rien n'est encore écrit.

## Pourquoi c'est structurant

Ce n'est pas un mode dégradé ajouté sur le côté : c'est ce qui décide de
l'architecture. Si l'app ne marche qu'avec un serveur, personne ne l'essaie — il faut
d'abord brancher un Raspberry Pi. Si elle marche seule, on l'essaie le soir même, et
le serveur devient une amélioration qu'on ajoute quand le foyer s'y met.

Cela rend aussi l'[étape 2 de l'assistant d'installation](https://github.com/overline090/MyHousehold/blob/main/docs/setup-wizard.md)
nécessaire : quand le serveur arrive, la configuration existe déjà dans le téléphone
et doit être reprise, pas ressaisie.

## Ce qui marche seul, ce qui demande un serveur

| Fonction | Sans serveur | Pourquoi |
|---|---|---|
| Tâches récurrentes, rotation, complétion | ✅ | Tout est calculable localement |
| Compteurs de temps | ✅ | Idem |
| Rendez-vous | ✅ | |
| Rappels | ✅ | Notifications locales, planifiées par l'app |
| Agenda du téléphone en lecture | ✅ | Via le fournisseur d'agenda Android, sans passer par un serveur |
| Historique et statistiques | ✅ | |
| **Plusieurs membres, rôles, invitations** | ❌ | Il faut un point de rendez-vous commun |
| **Synchronisation entre appareils** | ❌ | Idem |
| **Agendas externes (Google, CalDAV)** | ❌ | Les identifiants sont chiffrés et gardés côté serveur, et la synchro doit tourner quand le téléphone est éteint |
| **Sauvegarde ailleurs que sur le téléphone** | ❌ | Un export manuel reste possible |

Le mode sans serveur est **mono-utilisateur**. Il n'y a ni compte, ni mot de passe, ni
rôle : le foyer, c'est la personne qui tient le téléphone. Simuler plusieurs membres
en local serait un piège — leurs tâches ne les atteindraient jamais.

## Comment c'est possible sans travail en double

L'architecture hors-ligne d'abord le permet déjà :

- l'interface lit **Room**, jamais le réseau ;
- les identifiants sont des **UUID générés par le client** ;
- les mutations partent dans une **file locale**.

Sans serveur, la file n'a simplement pas de destination : elle s'accumule, ou se
vide à mesure (un réglage, pas une refonte). Le reste du code ne change pas. C'est le
bénéfice de ne pas avoir fait de Room un cache.

Un foyer local porte un `householdId` généré sur le téléphone. C'est lui qui part
inchangé vers le serveur au moment de l'adoption, pour que les données gardent leur
identité.

## Le coût réel : la logique de récurrence existe deux fois

C'est le point dur, et il faut le dire franchement. Le serveur calcule les
occurrences en TypeScript ; sans serveur, l'app doit les calculer en Kotlin. Deux
implémentations de la même chose : récurrence RRULE, fenêtre de matérialisation, tour
de rôle déterministe, échéances en heure locale avec les changements d'heure.

Deux implémentations qui divergent, c'est un foyer où une tâche apparaît le lundi sur
le téléphone et le mardi sur le serveur.

La parade retenue : **des vecteurs de test partagés**. Un fichier de cas en JSON
(`fixtures/recurrence.json`), versionné dans le dépôt du serveur, contenant des
entrées et les sorties attendues — y compris les cas de changement d'heure. Les deux
suites de tests le lisent, celle du serveur et celle de l'app. Une divergence devient
un test rouge, pas un bug découvert six mois plus tard par quelqu'un qui n'a pas sorti
les poubelles.

Les alternatives écartées : un module partagé en Kotlin Multiplatform (il faudrait
que le serveur soit en Kotlin), et demander au serveur de calculer pour l'app (ce
qui, par définition, ne marche pas sans serveur).

## Adopter un serveur plus tard

Depuis *Réglages → Connecter un serveur* :

1. On saisit l'adresse, ou on scanne le QR code de l'assistant d'installation.
2. L'app affiche **ce qui va être envoyé** et ce qui restera local. On ne pousse pas
   des années d'historique familial sans le dire.
3. Elle pousse sa configuration (`POST /setup/import` pendant l'installation, ou une
   route d'adoption sur un serveur déjà installé).
4. Elle crée le premier compte si le serveur est neuf — ce compte devient
   administrateur de l'instance.
5. Elle bascule en mode synchronisé : la file de mutations a désormais une
   destination.

## Rejoindre un foyer déjà installé, plus tard

L'adoption ci-dessus suppose un serveur neuf. L'autre cas arrivera forcément :
quelqu'un qui a utilisé l'app seul pendant des mois rejoint un foyer **déjà en
place** — il emménage, ou il s'y met enfin. Ses données doivent être reprises là
aussi.

C'est prévu, mais **bien plus tard** : le foyer existe déjà, avec ses tâches et ses
membres, et il faut donc décider quoi faire de deux tâches « poubelles » ou de deux
historiques parallèles. Les limitations seront posées à ce moment-là plutôt que
devinées maintenant. Piste probable : reprendre les tâches et compteurs qui
n'existent pas déjà dans le foyer, et laisser l'historique personnel sur le
téléphone.

Ce qui compte aujourd'hui, c'est que **rien n'empêche** cette reprise plus tard : les
identifiants sont générés par le client, les données locales ne sont jamais écrasées
par une connexion à un serveur, et le mode local reste une base complète plutôt qu'un
cache.

Et l'inverse — **repasser en local** après une adoption — doit exister : si le
serveur meurt, l'app garde tout en base locale, elle doit pouvoir continuer seule.
C'est aussi la meilleure garantie contre la perte de données du foyer.
