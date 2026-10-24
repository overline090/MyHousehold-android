# Mises à jour de l'application

L'app doit détecter les mises à jour et les télécharger, parce qu'elle sera d'abord
distribuée hors magasin : un APK attaché à une publication GitHub ne se met pas à
jour tout seul.

> ⚠️ **Spécification.** Rien n'est encore écrit.

## Trois canaux, trois comportements

| Variante | Détection | Téléchargement | Installation |
|---|---|---|---|
| `fdroid` | F-Droid | F-Droid | F-Droid |
| `playstore` | Play Store | Play Store | Play Store |
| `direct` (APK) | L'app elle-même | L'app elle-même | L'utilisateur confirme |

Seule la variante `direct` embarque le code de mise à jour. Dans les deux autres, le
magasin s'en charge et une app qui se mettrait à jour elle-même serait, au mieux,
refusée. Même séparation que pour les notifications : une interface, deux
implémentations, aucun `if (variante)` dans le code métier.

## Où l'app va chercher la version

**D'abord son serveur, ensuite GitHub.** Dans cet ordre, et c'est un choix important.

```
GET  https://mon-serveur/app-release      → version, notes, somme SHA-256, URL
GET  https://mon-serveur/app/latest.apk   → l'APK, servi par le serveur du foyer
```

Trois raisons de passer par le serveur du foyer plutôt que par GitHub :

1. **Compatibilité.** Le serveur sait quelle version d'app il attend. Un serveur en
   v0.4 ne doit pas pousser les téléphones vers une app v0.9 qui parlera une API
   qu'il ne connaît pas. C'est le serveur qui décide, pas le téléphone.
2. **Réseau local.** Cinq téléphones du même foyer téléchargent le même APK ; autant
   qu'il vienne du placard plutôt que d'Internet cinq fois. Et cela fonctionne même
   quand la connexion extérieure est coupée.
3. **Vie privée.** Les téléphones du foyer n'ont pas à interroger GitHub pour
   signaler leur existence.

Le serveur, lui, va chercher l'APK sur les publications GitHub et le garde en cache.
Sans serveur (app seule), l'app interroge directement l'API des publications GitHub.

## Vérifications avant d'installer

Une app qui télécharge et installe un APK est exactement ce qu'un attaquant
souhaiterait détourner. Donc, sans exception :

- **HTTPS obligatoire**, y compris vers son propre serveur. Pas de repli en clair.
- **Somme SHA-256** publiée avec la version et vérifiée après téléchargement. Un
  octet qui diffère, on jette.
- **Signature.** Android refuse de lui-même une mise à jour signée par une autre clé
  que celle installée : c'est la protection la plus solide, et elle est gratuite. Le
  projet doit donc garder **une seule clé de signature** pour toute la durée de vie
  du canal direct, et la sauvegarder sérieusement — la perdre casse la mise à jour de
  tous les APK déjà installés.
- **Rien d'automatique.** L'app télécharge, puis l'utilisateur confirme : installer
  une application est son geste, pas celui du logiciel. La permission
  `REQUEST_INSTALL_PACKAGES` est demandée au moment voulu, avec une explication.

## Comportement

Vérification une fois par jour, sur Wi-Fi par défaut, via WorkManager. Jamais au
premier plan : l'utilisateur ne doit pas attendre une requête réseau pour voir ses
tâches.

Quand une version est disponible : une ligne discrète dans les réglages, et une
notification **seulement** si la mise à jour est marquée nécessaire par le serveur
(incompatibilité d'API). Une notification par version serait du harcèlement.

L'écran de mise à jour montre la version installée, la version disponible, les notes
de version et la taille. Puis *Télécharger*, puis *Installer*.

## Quand l'app et le serveur ne s'entendent plus

`GET /version` et `GET /app-release` donnent ensemble de quoi détecter une
incompatibilité. Le serveur annonce la plage d'API qu'il parle ; l'app sait celle
qu'elle parle.

- **App plus ancienne que ce que le serveur exige** → bandeau bloquant avec un bouton
  de mise à jour. C'est le seul cas où l'app insiste.
- **App plus récente qu'un serveur ancien** → l'app continue avec les fonctions que
  le serveur connaît, et le dit une fois. Casser l'app parce que le serveur n'a pas
  été mis à jour serait absurde : c'est souvent la même personne qui doit faire les
  deux, et elle a besoin de l'app pour s'en rendre compte.

Le serveur s'engage à garder ses migrations rétrocompatibles d'une version majeure à
la précédente, précisément pour que les téléphones du foyer ne soient pas tous à
mettre à jour le même soir.
