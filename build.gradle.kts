// La racine ne déclare aucun greffon, et c'est voulu.
//
// Déclarer ici les greffons Kotlin ferait taire l'avertissement « chargé
// deux fois », mais exigerait aussi le greffon Android sur le chemin de
// classe de la racine — donc le dépôt de Google pour *configurer* le
// module `core`, qui n'en a pas besoin. Un avertissement vaut mieux
// qu'un module qu'on ne peut plus construire là où Google n'est pas
// joignable.
