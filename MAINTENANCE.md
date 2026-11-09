# Procédures de maintenance — DataShare

Ce document décrit les opérations courantes de maintenance, de diagnostic, de
correction et de retour arrière du backend DataShare. Il complète le
[README backend](./README.md), [TESTING.md](./TESTING.md) et [PERF.md](./PERF.md).

## 1. Architecture à maintenir

Le backend est une application Java 21 / Spring Boot 3.4.2 composée de :

- contrôleurs HTTP sous `src/main/java/com/datashare/controller` ;
- services métier sous `src/main/java/com/datashare/service` ;
- persistance PostgreSQL avec Spring Data JPA ;
- migrations de schéma Flyway sous `src/main/resources/db/migration` ;
- stockage des octets dans le répertoire configuré par `UPLOAD_DIRECTORY` ;
- authentification JWT avec expiration de 24 heures ;
- logs JSON et métriques HTTP via
  [RequestMetricsLoggingFilter](./src/main/java/com/datashare/configuration/RequestMetricsLoggingFilter.java).

Le stockage est réparti entre PostgreSQL pour les métadonnées et le système de
fichiers pour les octets. Une sauvegarde exploitable doit donc couvrir les deux.

## 2. Pré-requis opérateur

Installer et vérifier :

| Élément | Version ou condition |
| --- | --- |
| Java | JDK 21 |
| Maven | 3.9.x recommandé |
| Docker Desktop | Démarré, moteur Linux actif |
| PostgreSQL | 17 via `compose.yaml`, ou serveur compatible PostgreSQL |
| Node.js/npm | Requis uniquement pour maintenir le frontend |

Docker Desktop doit être démarré avant le backend. Spring Boot démarre le service
PostgreSQL de `compose.yaml` automatiquement avec Docker Compose et conserve le
volume `postgres_data` à l'arrêt de l'application.

## 3. Démarrage, arrêt et vérification

### Démarrage local

Depuis `datashare_backend` :

```powershell
mvn spring-boot:run
```

L'application est disponible sur `http://localhost:8080/api` et Swagger sur
`http://localhost:8080/api/swagger-ui/index.html`.

Vérifications rapides :

```powershell
docker compose ps
Invoke-WebRequest -Uri http://localhost:8080/api/ -UseBasicParsing
```

Un statut HTTP 200 sur `/api/` confirme que l'application répond. Le projet ne
déclare pas actuellement d'endpoint Actuator `/health` ; Swagger ou un appel
fonctionnel d'authentification servent de smoke test HTTP.

### Démarrage avec le JAR

```powershell
mvn clean package -DskipTests
java -jar target/datashare_backend-0.0.1-SNAPSHOT.jar
```

Pour arrêter uniquement PostgreSQL :

```powershell
docker compose stop
```

Ne pas supprimer le volume `postgres_data` sans sauvegarde et validation
explicite : il contient la base de développement.

## 4. Variables de configuration

Les variables les plus importantes sont :

| Variable | Usage de maintenance |
| --- | --- |
| `JWT_SECRET` | Clé Base64 privée de production ; la changer invalide les JWT existants |
| `JWT_ISSUER` | Émetteur attendu par les JWT |
| `DB_URL` | URL JDBC si Docker Compose est désactivé |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Connexion PostgreSQL |
| `POSTGRES_PORT` | Port local publié par Docker |
| `DOCKER_COMPOSE_ENABLED` | `false` pour utiliser une base externe |
| `DOCKER_COMPOSE_FILE` | Chemin vers le fichier Compose si lancement hors dossier backend |
| `UPLOAD_DIRECTORY` | Répertoire des fichiers physiques |
| `PUBLIC_API_URL` | Préfixe des liens de partage générés |
| `CORS_ALLOWED_ORIGINS` | Origines frontend autorisées, séparées par des virgules |
| `LOG_FORMAT` | `logstash` par défaut ; `console` pour un affichage lisible local |
| `APP_VERSION` | Version ajoutée aux logs structurés |

En production, les secrets doivent être fournis par le gestionnaire de secrets
de l'environnement. Ne pas les committer dans `.env`, le code source ou les logs.

## 5. Maintenance de la base et des migrations

### Ajouter une évolution de schéma

1. Créer une nouvelle migration versionnée, par exemple
   `V3__add_column_description.sql`.
2. Écrire une migration idempotente autant que possible et compatible avec les
   données existantes.
3. Ne jamais modifier une migration déjà appliquée (`V1`, `V2`, etc.) sur un
   environnement partagé.
4. Tester la migration sur une base vide et sur une copie représentative d'une
   base existante.
5. Exécuter `mvn clean verify` avec Docker Desktop actif.
6. Déployer l'application et vérifier les logs Flyway ainsi qu'un smoke test
   fonctionnel.

Flyway est exécuté au démarrage et Hibernate est configuré en validation du
schéma (`ddl-auto=validate`). Une divergence de schéma doit être corrigée par
une migration Flyway, pas par une modification manuelle des tables.

### Vérifier l'état de la base

```powershell
docker compose exec postgres pg_isready -U datashare -d datashare
docker compose exec postgres psql -U datashare -d datashare -c '\dt'
```

En cas de base externe, exécuter les commandes équivalentes avec `psql` en
respectant les paramètres de `DB_URL`.

## 6. Sauvegarde et restauration

### Sauvegarde PostgreSQL

Effectuer une sauvegarde avant une migration ou une opération corrective :

```powershell
New-Item -ItemType Directory -Force .\backups | Out-Null
docker compose exec -T postgres pg_dump -U datashare -d datashare --format=custom > .\backups\datashare-$(Get-Date -Format yyyyMMdd-HHmmss).dump
```

### Sauvegarde des fichiers

Sauvegarder le répertoire configuré par `UPLOAD_DIRECTORY` avec le même point de
cohérence que la base. En local, il s'agit par défaut de `.\uploads` :

```powershell
Copy-Item -Recurse -Force .\uploads .\backups\uploads-$(Get-Date -Format yyyyMMdd-HHmmss)
```

En production, utiliser un outil de sauvegarde avec versioning, checksum et
rétention. Ne pas considérer le dump PostgreSQL comme une sauvegarde suffisante :
les lignes de métadonnées ne contiennent pas les octets des fichiers.

### Restauration

La restauration est une opération exceptionnelle à valider avant exécution.
Arrêter l'application pour éviter les écritures concurrentes, restaurer d'abord
la base puis le répertoire de fichiers correspondant au même instant :

```powershell
docker compose stop
docker compose start postgres
Get-Content .\backups\datashare-YYYYMMDD-HHMMSS.dump -AsByteStream |
  docker compose exec -T postgres pg_restore -U datashare -d datashare --clean --if-exists
```

`pg_restore` attend un dump au format custom. Vérifier ensuite les migrations,
le nombre de fichiers et un téléchargement de test avant de relancer le service.

## 7. Tests avant livraison

Depuis `datashare_backend` :

```powershell
mvn clean verify
```

Cette commande doit terminer par `BUILD SUCCESS`, exécuter les tests unitaires et
respecter le seuil JaCoCo configuré à 80 % de couverture des lignes.

Pour les tests d'intégration PostgreSQL/Testcontainers :

```powershell
mvn test '-Dtest=ApiIntegrationTest,MultipartHttpIntegrationTest' '-Dsurefire.excludes='
```

Les tests ne doivent jamais être lancés contre la base de développement : le
profil `test` utilise un PostgreSQL temporaire et un répertoire de stockage
temporaire.

Pour maintenir le frontend associé :

```powershell
cd ..\datashare
npm ci
npm test
npm run cypress:run
npm audit --audit-level=high
```

Les détails du plan et des résultats sont centralisés dans
[TESTING.md](./TESTING.md) et [SECURITY.md](../datashare/SECURITY.md).

## 8. Logs et diagnostic

Les logs applicatifs sont structurés au format Logstash par défaut. Le filtre
[RequestMetricsLoggingFilter](./src/main/java/com/datashare/configuration/RequestMetricsLoggingFilter.java)
émet un événement `http_request` avec :

- `request.id` et l'en-tête de corrélation `X-Request-Id` ;
- `http.request.method` ;
- `url.path` normalisée : les tokens de partage et les identifiants de fichiers
  sont remplacés par `{token}` et `{fileId}` ;
- `http.response.status_code` ;
- `http.server.duration_ms` ;
- `http.response.body.bytes` lorsqu'un `Content-Length` est disponible ;
- `outcome` à `success` ou `failure`.

Exemple de lecture locale :

```powershell
$env:LOG_FORMAT = 'console'
mvn spring-boot:run
```

Pour l'exploitation, conserver `LOG_FORMAT=logstash` et agréger les événements
JSON. Surveiller en priorité :

| Signal | Interprétation ou action |
| --- | --- |
| Hausse des 5xx | Vérifier exception applicative, base, disque et dépendances externes |
| Hausse des 401/403 | Vérifier JWT, `JWT_SECRET`, expiration et configuration CORS |
| P95/P99 en hausse | Comparer taille de fichier, charge, disque et requêtes PostgreSQL |
| `http.response.body.bytes=-1` | Le serveur n'a pas fourni de `Content-Length`, pas nécessairement une erreur |
| Erreur de stockage | Vérifier espace disque, droits de `UPLOAD_DIRECTORY` et cohérence DB/fichiers |
| Erreur Flyway au démarrage | Ne pas contourner la migration ; sauvegarder et corriger le schéma |

Les tokens, mots de passe, JWT et contenus de fichiers ne doivent pas apparaître
dans les logs. Lors d'un incident, transmettre le `request.id`, l'horodatage, la
route normalisée et le statut, jamais le header `Authorization`.

## 9. Procédure de correction d'un incident

1. **Qualifier** : noter l'environnement, l'heure, la version déployée, le
   `request.id`, le statut HTTP et l'impact utilisateur.
2. **Reproduire** : utiliser Swagger/Postman ou un test automatisé avec des
   données non sensibles. Vérifier d'abord les logs JSON et la santé PostgreSQL.
3. **Isoler** : déterminer si la cause concerne le code, la migration, JWT,
   CORS, le stockage, le réseau ou une dépendance.
4. **Corriger** : faire une modification minimale, sans modifier directement une
   migration Flyway déjà appliquée ni supprimer manuellement des fichiers.
5. **Tester** : ajouter ou mettre à jour un test de non-régression, puis exécuter
   `mvn clean verify`, les intégrations concernées et le benchmark si le chemin
   upload/download est touché.
6. **Vérifier la sécurité** : exécuter `npm audit` pour le frontend et contrôler
   qu'aucun secret ou token n'est introduit dans les logs ou le diff.
7. **Déployer progressivement** : sauvegarder la base et les fichiers, déployer,
   exécuter les smoke tests et surveiller les métriques P95/P99 et les 5xx.
8. **Documenter** : noter la cause racine, le correctif, les tests, le risque
   résiduel et l'action préventive.

## 10. Retour arrière

### Correction applicative

- conserver l'artefact de la version précédente ;
- arrêter l'instance actuelle ;
- redémarrer le JAR précédent avec les mêmes variables de configuration ;
- vérifier `/api/`, l'authentification et un téléchargement de test ;
- surveiller les logs et conserver l'incident pour analyse.

### Migration de base

Ne pas annuler manuellement une migration en production. Préférer une migration
corrective forward-only. Une restauration complète nécessite l'approbation de
l'opérateur responsable, une sauvegarde vérifiée et la restauration coordonnée de
PostgreSQL et de `UPLOAD_DIRECTORY`.

## 11. Fréquence recommandée

| Fréquence | Opération |
| --- | --- |
| À chaque changement | Tests ciblés, `mvn clean verify`, revue du diff et des logs |
| À chaque release | Sauvegarde, migrations testées, audit sécurité, smoke test et benchmark si nécessaire |
| Quotidienne | Vérifier erreurs 5xx, latences P95/P99, espace disque et disponibilité PostgreSQL |
| Hebdomadaire | Vérifier les sauvegardes par une restauration de test et l'absence de fichiers orphelins |
| Mensuelle | Mettre à jour les dépendances, revoir les secrets, la rétention des logs et les seuils de performance |

