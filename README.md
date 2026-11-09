# DataShare Backend

Collection Swagger importable dans Postman :
[collection DataShare](postman/DataShare.postman_collection.json),
[environnement local](postman/DataShare.local.postman_environment.json)
et [instructions](postman/README.md).

API Java 21 / Spring Boot 3.4.2. Architecture : controleurs HTTP, services metier,
repositories Spring Data JPA et entites PostgreSQL. Les DTO et les routes du contrat
OpenAPI existant sont conserves. Les dates utilisent `java.time`.

## Demarrer sous PowerShell

Prerequis : JDK 21, Maven et Docker Desktop demarre en mode conteneurs Linux.
Depuis `datashare_backend` :

```powershell
mvn spring-boot:run
```

API : `http://localhost:8080/api`. Swagger :
`http://localhost:8080/api/swagger-ui/index.html`.
Les migrations Flyway creent les tables automatiquement ; Hibernate verifie le schema.
La migration V1 d'origine est conservee pour les bases existantes ; V2 renomme les
tables en `"user"` et `file`, et les colonnes de mots de passe en `password_user`
et `password_file`, sans supprimer de donnees. Le nom `user` est delimite par des
guillemets car il est reserve par PostgreSQL.
Spring Boot demarre automatiquement le service PostgreSQL 17 de `compose.yaml`
dans Docker Desktop et attend son etat sain avant de connecter Flyway et JPA.
Il n'y a plus de commande `docker compose up` a executer avant l'application.
PostgreSQL reste demarre a l'arret de l'application (`start-only`) et les donnees
sont conservees dans le volume `postgres_data`. Pour l'arreter : `docker compose stop`.
Docker Desktop et son moteur Linux doivent etre actifs ; Spring Boot gere le
conteneur PostgreSQL, pas le lancement de l'interface Docker Desktop.

Le meme comportement est disponible avec le JAR depuis `datashare_backend` :

```powershell
mvn package -DskipTests
java -jar target/datashare_backend-0.0.1-SNAPSHOT.jar
```

Depuis un autre repertoire, definir `DOCKER_COMPOSE_FILE` avec le chemin absolu
vers `compose.yaml`.

Le JWT reprend le principe du projet 2 Etudiant : JJWT, un service de generation et
verification, un filtre `JwtAuthenticationFilter` et un `CustomUserDetailService`
qui recharge le compte depuis PostgreSQL. Une cle locale stable propre a DataShare
est fournie dans `JwtService` pour le developpement : `JWT_SECRET` n'est donc plus
necessaire pour lancer l'application en local. Pour un deploiement, fournir sa
propre cle Base64 via `JWT_SECRET`. Une nouvelle cle invalide les anciens jetons.

| Variable | Valeur par defaut / usage |
| --- | --- |
| `JWT_SECRET` | Facultative en local ; surcharge de la cle Base64, 32 octets aleatoires minimum |
| `JWT_ISSUER` | `http://localhost:8080/api` |
| `POSTGRES_DB` | `datashare` |
| `POSTGRES_USER` | `datashare` |
| `POSTGRES_PASSWORD` | `datashare` pour le developpement local |
| `POSTGRES_PORT` | `5432` |
| `DOCKER_COMPOSE_ENABLED` | `true`, demarrage automatique de PostgreSQL |
| `DOCKER_COMPOSE_FILE` | `compose.yaml`, chemin relatif au repertoire de lancement |
| `DB_URL` | URL JDBC PostgreSQL, utilisee lorsque la gestion Compose est desactivee |
| `UPLOAD_DIRECTORY` | `./uploads`, fichiers conserves sous un UUID |
| `PUBLIC_API_URL` | `http://localhost:8080/api`, prefixe des liens de partage |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200`, plusieurs origines separees par des virgules |

Spring Boot lit les details du service Compose et configure automatiquement la
connexion PostgreSQL (port publie, base, utilisateur et mot de passe), y compris
les valeurs du fichier `.env` lu par Docker Compose. Ces details sont prioritaires
sur `spring.datasource.*` tant que la gestion Compose est active.
Pour utiliser un serveur PostgreSQL gere ailleurs, definir
`DOCKER_COMPOSE_ENABLED=false`, puis `DB_URL`, `POSTGRES_USER` et `POSTGRES_PASSWORD`.

## Routes implementees

| Methode | Route avec contexte | Comportement |
| --- | --- | --- |
| POST | `/api/auth/register` | Cree un compte, retourne `201` et `{ "token": "..." }` |
| POST | `/api/auth/login` | Verifie les identifiants, retourne `200` et `{ "token": "..." }` |
| POST | `/api/files` | Upload multipart anonyme ou authentifie, retourne `201` et `{ "metadata": { ... } }` |

Les emails sont normalises en minuscules et uniques en base, y compris lors
d'inscriptions concurrentes. Le mot de passe du compte doit contenir au moins
8 caracteres et au plus 72 octets UTF-8 (limite BCrypt). Il est hache avec BCrypt,
cout 12. Une connexion incorrecte renvoie la meme erreur pour un email inconnu
ou un mot de passe incorrect. Les reponses d'authentification respectent le contrat
existant et contiennent uniquement le JWT.

Les JWT sont signes en HS256 avec JJWT 0.12.6, valables 24 heures et verifient la
signature, l'emetteur, l'expiration et la presence du sujet. Le sujet est l'email
normalise du compte, comme le login dans le projet 2.
Pour envoyer le JWT : `Authorization: Bearer <token>`. Le filtre
`JwtAuthenticationFilter` valide le jeton, charge le compte courant et remplit le
contexte Spring Security. Un compte supprime ne peut plus utiliser son ancien JWT.
Aucun cookie de session n'est utilise.
Un JWT fourni mais invalide est refuse sur les routes protegees et sur l'upload.
L'inscription et la connexion ignorent un eventuel ancien Bearer : elles utilisent
les identifiants du corps JSON et retournent un nouveau JWT apres succes.

### Exemple inscription et connexion (PowerShell)

```powershell
$body = @{ email = 'alice@example.com'; password = 'StrongPass123' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType application/json -Body $body
$auth = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login -ContentType application/json -Body $body

# Depuis PowerShell, curl.exe evite l'alias Invoke-WebRequest.
curl.exe -X POST http://localhost:8080/api/files -H "Authorization: Bearer $($auth.token)" -F "file=@C:/chemin/rapport.pdf" -F "expirationDays=3" -F "password=secret123" -F "tags=projet" -F "tags=prive"
```

### Upload multipart

- `file` : obligatoire, non vide, taille maximale de **1 000 000 000 octets**.
- `expirationDays` : facultatif, entier de 1 a 7, defaut 7.
- `password` : facultatif, minimum 6 caracteres, maximum 72 octets UTF-8 ; hache en base.
- `tags` : facultatif, champs texte repetes, reserves aux comptes authentifies ;
  valeurs uniques apres suppression des espaces aux extremites, 30 caracteres maximum.

Les parametres facultatifs sont des champs texte multipart ordinaires. Il n'est
pas necessaire de creer des parties JSON pour `expirationDays` ou `tags`.
Omettre `password` pour ne pas proteger le fichier.

Les extensions bloquees sont configurables dans `app.storage.blocked-extensions` :
`exe,bat,cmd,com,msi,sh,ps1,jar`. Le nom client est valide mais ne sert jamais de chemin
de stockage. Les octets sont copies en flux et le fichier physique est nettoye si
l'ecriture ou la transaction SQL echoue. Les metadonnees, le proprietaire facultatif,
le hash du mot de passe et le jeton de partage aleatoire de 256 bits sont persistants.

Le lien retourne identifie le partage. Les routes de consultation/telechargement
`/shares/**`, la liste, la suppression et la modification des tags restent les stubs
`501` du code genere ; leur implementation ne fait pas partie de cette livraison.
L'expiration est enregistree en base ; aucun traitement de purge n'est encore execute.
Le frontend Angular utilise toujours ses services mock et n'est pas raccorde ici.

### Exemples curl (Bash)

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"StrongPass123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"StrongPass123"}'

curl -X POST http://localhost:8080/api/files -F 'file=@rapport.pdf'

curl -X POST http://localhost:8080/api/files \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@rapport.pdf' -F 'expirationDays=3' \
  -F 'password=secret123' -F 'tags=projet' -F 'tags=prive'
```

## Erreurs HTTP

Les erreurs utilisent `ApiError` : `code`, `message`, `timestamp` et eventuellement
`fieldErrors`. Statuts : `400` validation, `401` identifiants/JWT invalides,
`409` email deja utilise, `413` taille depassee, `415` extension interdite.
Les erreurs internes renvoient un message generique sans trace technique.

Si Docker ne se connecte pas au moteur, demarrer Docker Desktop. Si PostgreSQL refuse
la connexion, verifier `docker compose ps` et les variables `POSTGRES_*` du terminal.
Si le port 5432 est occupe, definir `$env:POSTGRES_PORT = '5433'` avant le demarrage.
Si l'application refuse la cle JWT, verifier son encodage
Base64 et sa taille. Les identifiants PostgreSQL d'un volume deja initialise doivent
correspondre a ceux utilises lors de sa creation.

## Verification

```powershell
mvn clean verify
```

Les tests utilisent exclusivement PostgreSQL 17 via Testcontainers, les migrations
Flyway et un repertoire temporaire. Docker Desktop doit etre demarre. Aucune cle JWT
personnelle n'est necessaire. Chaque contexte d'integration utilise un PostgreSQL
ephemere sur un port aleatoire, nettoye a la fin des tests ; la base de developpement
et son volume ne sont pas modifies. Compose est desactive uniquement dans le profil
de test pour laisser Testcontainers gerer ces conteneurs. Ils couvrent
les parcours d'authentification, les erreurs, les signatures et expirations JWT,
les uploads anonymes/authentifies, la validation et le nettoyage des fichiers.
Un test utilise le serveur HTTP embarque pour verifier le multipart et la limite
de taille imposee par le conteneur servlet.
Testcontainers 1.21.4 est utilise pour sa compatibilite avec les versions recentes
de Docker Engine.

Reference JWT : [JJWT, generation et verification des signatures](https://github.com/jwtk/jjwt).
Demarrage automatique : [Spring Boot et Docker Compose](https://docs.spring.io/spring-boot/3.4/how-to/docker-compose.html).
Compatibilite Docker : [Testcontainers 1.21.4](https://github.com/testcontainers/testcontainers-java/releases/tag/1.21.4).
