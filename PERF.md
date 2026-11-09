# Performance et observabilité — DataShare backend

## 1. Objectif

Ce document présente un test de performance reproductible sur l'endpoint critique
de téléchargement et décrit les logs structurés utilisés pour analyser les
métriques HTTP importantes.

Le benchmark a été exécuté sur une instance locale Spring Boot avec PostgreSQL
géré par Docker Desktop. Il s'agit d'une mesure de référence fonctionnelle, pas
d'un test de capacité de production ni d'un test de montée en charge distribué.

## 2. Endpoint testé

```text
POST /api/shares/{token}/download
```

Le scénario prépare automatiquement un utilisateur et un fichier de test :

1. inscription d'un utilisateur unique ;
2. connexion implicite via le JWT retourné par l'inscription ;
3. upload d'un fichier PDF de 1 856 octets ;
4. récupération du lien de partage ;
5. cinq téléchargements d'échauffement non comptabilisés ;
6. cent téléchargements mesurés séquentiellement avec un corps JSON `{}`.

Le téléchargement est public pour ce fichier sans mot de passe. Aucun token,
mot de passe ou contenu de fichier n'est écrit dans le rapport.

## 3. Protocole reproductible

Prérequis : Java 21, Maven, Docker Desktop démarré avec les conteneurs Linux et
le backend disponible sur `http://localhost:8080/api`.

L'application peut être lancée avec :

```powershell
mvn spring-boot:run
```

Le client de mesure utilisé lors de la validation est PowerShell/.NET
`HttpClient`. Le protocole peut être rejoué avec un outil de charge tel que
k6, JMeter ou `oha` ; ces outils ne sont pas requis par le projet et aucun
n'ajoute de dépendance à l'application.

Exemple de commande de contrôle avec curl pour un token déjà disponible :

```powershell
curl.exe -s -o NUL -w "status=%{http_code} time=%{time_total}s size=%{size_download}\n" `
  -X POST "http://localhost:8080/api/shares/<token>/download" `
  -H "Content-Type: application/json" `
  -d "{}"
```

## 4. Résultats mesurés

| Métrique | Résultat |
| --- | ---: |
| Endpoint | `POST /api/shares/{token}/download` |
| Taille du fichier | 1 856 octets |
| Requêtes mesurées | 100 |
| Concurrence | 1 client séquentiel |
| Réponses 200 | 100 |
| Erreurs | 0 |
| Durée totale | 2 804,87 ms |
| Débit | 35,65 requêtes/s |
| Latence moyenne | 27,47 ms |
| P50 | 22,09 ms |
| P95 | 46,45 ms |
| P99 | 100,84 ms |
| Minimum | 15,56 ms |
| Maximum | 134,92 ms |

### Analyse

- Le taux de succès est de 100 % sur les 100 requêtes mesurées.
- La moitié des téléchargements se termine en moins de 22,09 ms.
- 95 % des requêtes restent sous 46,45 ms, ce qui constitue une bonne base
  locale pour un petit fichier.
- Le P99 à 100,84 ms et le maximum à 134,92 ms montrent quelques écarts de
  latence. Ils doivent être surveillés sur une charge concurrente et avec des
  fichiers plus volumineux.
- Le débit de 35,65 requêtes/s ne doit pas être extrapolé directement à la
  production : le test est séquentiel et les performances dépendent de la base,
  du disque, du réseau, de la taille du fichier et du nombre de workers.

### Critères de lecture proposés

Pour une exécution locale de référence :

| Critère | Seuil indicatif | Résultat | Statut |
| --- | ---: | ---: | --- |
| Taux d'erreur | 0 % | 0 % | PASS |
| P95 téléchargement petit fichier | < 100 ms | 46,45 ms | PASS |
| P99 téléchargement petit fichier | < 250 ms | 100,84 ms | PASS |

Ces seuils sont des seuils de non-régression locale, à recalibrer sur
l'environnement de déploiement.

## 5. Logs structurés mis en place

Spring Boot est configuré pour produire des événements JSON au format Logstash :

```properties
logging.structured.format.console=logstash
logging.structured.ecs.service.name=datashare-backend
logging.structured.ecs.service.version=0.0.1-SNAPSHOT
```

Le filtre `RequestMetricsLoggingFilter` ajoute un événement par requête avec
les champs suivants :

| Champ | Signification |
| --- | --- |
| `event.name` | Nom stable de l'événement (`http_request`) |
| `request.id` | Identifiant de corrélation placé aussi dans `X-Request-Id` |
| `http.request.method` | Méthode HTTP |
| `url.path` | Route normalisée, sans query string ni token réel |
| `http.response.status_code` | Statut HTTP retourné |
| `http.response.body.bytes` | Taille de réponse connue par le serveur |
| `http.server.duration_ms` | Durée de traitement côté serveur |
| `outcome` | `success` pour 2xx/3xx, `failure` pour 4xx/5xx |

Les requêtes 4xx sont journalisées en niveau `WARN` et les 5xx en niveau
`ERROR`. Les autres sont journalisées en `INFO`. Les tokens des liens de partage,
les query strings, les mots de passe, les JWT et les fichiers ne sont pas
enregistrés.

Exemple de structure attendue, avec les valeurs variables abrégées :

```json
{
  "@timestamp": "...",
  "log.level": "INFO",
  "service.name": "datashare-backend",
  "event.name": "http_request",
  "request.id": "...",
  "http.request.method": "POST",
  "url.path": "/api/shares/{token}/download",
  "http.response.status_code": 200,
  "http.server.duration_ms": 22,
  "outcome": "success",
  "message": "HTTP request completed"
}
```

## 6. Métriques à suivre

Les métriques minimales à extraire des logs sont :

- taux de requêtes par route et par méthode ;
- taux de réponses 4xx et 5xx ;
- moyenne, P50, P95 et P99 de `http.server.duration_ms` ;
- volume de données retournées via `http.response.body.bytes` ;
- débit de téléchargement et nombre de téléchargements réussis ;
- corrélation d'une erreur avec `request.id` ;
- comparaison de la latence selon la taille du fichier et le nombre de clients.

Exemples d'analyses dans un agrégateur JSON :

```text
group by url.path, http.response.status_code
percentile(http.server.duration_ms, 95) by url.path
count(outcome = "failure") / count(*)
sum(http.response.body.bytes) by url.path
```

## 7. Limites et prochaines mesures

- Le benchmark réalisé est séquentiel ; il ne mesure pas la saturation sous
  concurrence.
- Il porte sur un petit fichier et ne caractérise pas les uploads ou les
  téléchargements proches de la limite configurée à 1 Go.
- La mesure inclut le traitement HTTP, la lecture PostgreSQL du partage et la
  lecture du disque local, mais pas un réseau réel ni un proxy inverse.
- Une campagne complémentaire doit exécuter 1, 5, 10, 25 et 50 clients
  concurrents, avec des fichiers de 1 KiB, 1 MiB et 100 MiB, puis comparer les
  P95/P99, le débit, les erreurs et l'utilisation CPU/mémoire/disque.
- Les logs JSON doivent être collectés par la plateforme de déploiement, avec
  une rotation et une rétention adaptées, avant toute mise en production.

