# Contribuer à l'application Android

Merci de l'intérêt porté au projet. Les contributions sont ouvertes à tout le monde :
code, interface, traductions, tests, retours d'usage, relecture de la conception.

## Avant tout : la licence

L'application est sous [PolyForm Noncommercial 1.0.0](LICENSE).

**Autorisé :** utiliser, modifier, redistribuer, pour soi, sa famille, une
colocation, une association, une école, une collectivité.

**Interdit :** toute exploitation commerciale — revente, hébergement payant,
intégration dans un produit payant, publication sur un *store* à titre onéreux.

Ce n'est volontairement pas une licence open source au sens de l'OSI, qui exige
d'autoriser l'usage commercial. La formulation juste est « libre d'usage et de
modification, non commercial ».

### En proposant une contribution

Vous acceptez qu'elle soit distribuée sous cette même licence (*inbound = outbound*)
et certifiez avoir le droit de la proposer. Signez vos commits avec `git commit -s`
([Developer Certificate of Origin](https://developercertificate.org/)) : c'est une
attestation, pas une cession — vous gardez le copyright sur votre contribution.

## Code de conduite

Voir [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Par où commencer

Le projet est en **phase de conception** : il n'y a pas encore de projet Gradle. Les
contributions les plus utiles ne sont donc pas du code :

1. **Relire [`docs/screens.md`](docs/screens.md)** — manque-t-il un écran ou une
   interaction pour votre propre foyer ?
2. **Relire [`docs/app-architecture.md`](docs/app-architecture.md)** — la stratégie
   hors-ligne tient-elle ? Les cas limites vous paraissent-ils couverts ?
3. **Décrire votre usage réel** — garde alternée, colocation tournante, horaires
   décalés. Ces cas limites valent plus que des avis généraux.
4. **Proposer des maquettes** — même au crayon. Une capture dans une issue vaut dix
   paragraphes.

Ouvrez une issue avant de commencer une fonctionnalité conséquente.

## Flux de travail

```bash
git clone https://github.com/overline090/MyHousehold-android.git
cd MyHousehold-android
git checkout -b feat/mon-ecran
git commit -s -m "feat(counters): affiche le temps écoulé sur la carte"
git push -u origin feat/mon-ecran
```

Branches : `feat/`, `fix/`, `docs/`, `refactor/`, `test/`, `chore/`.
Commits au format [Conventional Commits](https://www.conventionalcommits.org/).

## Attentes sur le code

Les règles de [`docs/app-architecture.md`](docs/app-architecture.md) s'appliqueront :

- **L'UI lit Room, jamais le réseau.** Un composable qui attend une réponse HTTP est
  un bug d'architecture, pas un détail.
- **Écriture locale et mise en file dans la même transaction.** Sinon un *crash*
  entre les deux perd la mutation.
- **Jamais `System.currentTimeMillis()` pour un écoulement.** On utilise l'horloge
  serveur corrigée de la dérive — voir la section « Le temps ».
- **Jamais la couleur seule** pour porter une information : icône et texte aussi.
- **Pas de chaîne en dur** dans un composable : tout passe par `strings.xml`, pour
  que la traduction reste possible.
- **Aucun `if (variant)` dans le code métier.** Les différences FCM / UnifiedPush
  sont derrière une interface.

Avant une pull request :

```bash
./gradlew ktlintCheck detekt test
```

## Pull requests

- Une PR = un sujet.
- Pour tout changement visible : **joignez une capture d'écran**, en thème clair et
  en thème sombre.
- Décrivez le *pourquoi* ; le diff dit déjà le *quoi*.
- Mettez la documentation à jour dans la même PR si le comportement change.

La revue porte sur le code, jamais sur la personne. Si une remarque vous paraît
injuste, dites-le.

## Signaler un bug

Modèle, version d'Android, version de l'app, version du serveur, étapes de
reproduction, comportement attendu et observé. Les logs aident, en retirant les
jetons.

Le réglage *Synchronisation → Diagnostic* copiera un état technique prêt à coller
(sans aucun jeton).

## Signaler une faille de sécurité

**N'ouvrez pas d'issue publique.** Onglet *Security* → *Report a vulnerability*, ou
contact privé du mainteneur. Les utilisateurs auto-hébergent : laissez-leur le temps
de mettre à jour.

## Questions

Issue avec le label `question`. La documentation est encore incomplète par
construction : il n'y a pas de question bête.
