# Plan de tests — DataShare

Ce document décrit la stratégie de test de DataShare, les commandes permettant de
rejouer les tests et les résultats obtenus sur l'environnement local.

## 1. Périmètre et critères d'acceptation

Les fonctionnalités critiques couvertes sont :

- création de compte, connexion, hachage BCrypt et authentification JWT ;
- contrôle d'accès aux ressources protégées ;
- téléversement multipart, validation des fichiers et des options d'expiration ;
- stockage physique, suppression et rollback en cas d'échec ;
- consultation, téléchargement et suppression des partages ;
- validation des erreurs HTTP, CORS et authentification Swagger ;
- parcours frontend d'upload, de connexion, de dashboard et de téléchargement.

Le seuil d'acceptation de la couverture est de **70 % minimum**. Le projet est
plus exigeant : la règle JaCoCo configurée dans `pom.xml` impose **80 % de
couverture des lignes** lors de `mvn verify`.

## 2. Environnement nécessaire

Versions vérifiées sur la machine de validation :

| Outil | Version |
| --- | --- |
| Java | 21.0.12 LTS |
| Maven | 3.9.16 |
| Spring Boot | 3.4.2 |
| PostgreSQL utilisé par les tests | 17 |
| Testcontainers | 1.21.4 |
| Node.js | 24.18.0 |
| npm | 11.17.0 |
| Angular | 21.2.x |
| Jest | 30.5.2 |
| Cypress | 16.2.1 |

Docker Desktop doit être démarré avec les conteneurs Linux avant les tests
d'intégration. Testcontainers démarre un PostgreSQL temporaire ; les tests
n'utilisent pas la base de développement et leur répertoire de stockage est
temporaire.

## 3. Organisation des suites

| Niveau | Localisation | Outils | Périmètre |
| --- | --- | --- | --- |
| Unitaire backend | `src/test/java/com/datashare` | JUnit 5, Mockito, Spring MockMvc | Services, contrôleurs, JWT, sécurité, modèles et stockage isolé |
| Intégration backend | `src/test/java/com/datashare/integration` | Spring Boot Test, MockMvc, TestRestTemplate, Testcontainers | Contexte Spring complet, migrations Flyway, JPA, PostgreSQL réel et HTTP multipart |
| Unitaire frontend | `../datashare/src/app/**/*.spec.ts` | Jest, `jest-preset-angular` | Services d'authentification/fichiers et composants principaux |
| End-to-end frontend | `../datashare/cypress/e2e` | Cypress | Parcours navigateur sur upload, login, register, dashboard, header et download |

Les tests d'intégration Spring désactivent Docker Compose avec le profil `test`
et utilisent PostgreSQL Testcontainers. Le fichier
`ApiIntegrationTest` couvre les appels avec MockMvc ;
`MultipartHttpIntegrationTest` vérifie en plus un vrai échange HTTP sur un port
aléatoire.

## 4. Plan de tests détaillé

| ID | Scénario critique | Type | Résultat attendu |
| --- | --- | --- | --- |
| AUTH-01 | Créer un utilisateur valide | Unitaire + intégration | Compte créé, mot de passe non stocké en clair |
| AUTH-02 | Se connecter avec des identifiants valides | Unitaire + intégration + E2E | JWT retourné et accès au dashboard autorisé |
| AUTH-03 | Refuser un compte ou un JWT invalide | Unitaire + intégration | Réponse d'erreur, aucune ressource protégée exposée |
| FILE-01 | Envoyer un fichier multipart valide | Unitaire + intégration HTTP + E2E | Fichier accepté et métadonnées retournées |
| FILE-02 | Refuser un fichier ou une option invalide | Unitaire + intégration | Erreur de validation exploitable, fichier absent du stockage |
| FILE-03 | Nettoyer le stockage après une erreur | Unitaire | Aucun fichier partiel restant après rollback |
| SHARE-01 | Consulter et télécharger un partage | Unitaire + intégration + E2E | Métadonnées visibles et téléchargement autorisé selon les règles |
| SHARE-02 | Supprimer un fichier appartenant à l'utilisateur | Unitaire + intégration + E2E | Fichier supprimé et absent de la liste |
| UI-01 | Afficher « Se connecter » ou « Mon espace » selon l'état de session | Jest + Cypress | Navigation vers `/login` ou `/dashboard` correcte |
| UI-02 | Utiliser le bouton « Ajouter des fichiers » | Jest + Cypress | Navigation vers `/upload` correcte |

## 5. Commandes exécutables

### 5.1 Backend — tests unitaires et rapport JaCoCo

Depuis `datashare_backend` :

```powershell
mvn clean test
```

La configuration Surefire exclut volontairement les classes suffixées
`IntegrationTest` et `HttpIntegrationTest` de cette commande rapide.

Pour exécuter la vérification Maven et le seuil JaCoCo :

```powershell
mvn verify
```

Le rapport HTML est généré dans :

```text
target/site/jacoco/index.html
```

### 5.2 Backend — tests d'intégration

Docker Desktop doit être démarré. Depuis `datashare_backend` :

```powershell
mvn test '-Dtest=ApiIntegrationTest,MultipartHttpIntegrationTest' '-Dsurefire.excludes='
```

La syntaxe avec guillemets est importante dans PowerShell, car la virgule de
`-Dtest` peut sinon être interprétée par le shell.

Pour exécuter toute la suite backend, y compris les intégrations, en une seule
commande :

```powershell
mvn clean test '-Dsurefire.excludes='
```

### 5.3 Frontend — Jest

Depuis `datashare` :

```powershell
npm ci
npm test
npm run test:coverage
```

Le rapport Jest est généré dans `coverage/`.

### 5.4 Frontend — Cypress

Dans un terminal, démarrer l'application :

```powershell
cd ..\datashare
npm start
```

Dans un second terminal :

```powershell
cd ..\datashare
npm run cypress:run
```

Les scénarios Cypress interceptent les appels API afin de tester le comportement
du navigateur de manière déterministe. Ils constituent donc des E2E frontend ;
les échanges backend réels sont validés par les tests Spring/Testcontainers.

## 6. Résultats obtenus

| Suite | Tests | Réussis | Échecs | Résultat |
| --- | ---: | ---: | ---: | --- |
| Backend unitaire — `mvn clean test` | 49 | 49 | 0 | PASS |
| Backend intégration — API + HTTP multipart | 18 | 18 | 0 | PASS |
| Backend total | 67 | 67 | 0 | PASS |
| Frontend Jest | 14 | 14 | 0 | PASS |
| Frontend Cypress | 15 | 15 | 0 | PASS |

Les 18 tests d'intégration se répartissent ainsi : 16 scénarios dans
`ApiIntegrationTest` et 2 scénarios HTTP réels dans
`MultipartHttpIntegrationTest`.

## 7. Couverture de code

Rapport JaCoCo généré par le dernier `mvn clean verify` (suite unitaire ; les
tests d'intégration restent exclus par la configuration Surefire par défaut) :

| Indicateur backend | Résultat | Seuil | Statut |
| --- | ---: | ---: | --- |
| Classes analysées | 43 | — | INFO |
| Couverture des lignes | **82,08 %** | 70 % (80 % Maven) | PASS |
| Couverture des branches | **70,12 %** | 70 % indicatif | PASS |

La commande `mvn clean verify` a également terminé avec `BUILD SUCCESS` et le
message `All coverage checks have been met.`. La suite unitaire couvre donc
**82,08 % des lignes** sans dépendre d'une base PostgreSQL externe.

La couverture des branches est suivie séparément : elle dépasse 70 % sur le
dernier rapport, mais elle n'est pas la règle bloquante actuellement configurée
dans le `pom.xml`.

## 8. Lecture des résultats et actions en cas d'échec

- Un échec de `mvn clean test` indique généralement une régression dans un
  service, un contrôleur, la sécurité ou la gestion du stockage.
- Un échec de la suite d'intégration doit être analysé avec Docker Desktop,
  l'accès à l'image `postgres:17`, les migrations Flyway et les logs Surefire.
- Un échec de `npm test` concerne le comportement isolé du frontend ; relancer
  avec `npm test -- --runInBand --verbose` pour obtenir le nom du test fautif.
- Un échec Cypress doit être reproduit avec `npm run cypress:open` et vérifié
  dans `cypress/screenshots` ou `cypress/videos` lorsque ces artefacts sont
  activés.
- Avant une livraison, la commande de référence est `mvn verify`, complétée par
  `npm test` et `npm run cypress:run`.

