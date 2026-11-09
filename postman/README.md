# Collection Postman DataShare

Source : `../../najwa-5db-GeneratedAPI_2026-10-13-1137-1.0.0-unresolved.yaml`.
Format : Postman Collection v2.1. La collection contient toutes les operations du
Swagger, groupees en **Auth**, **Files** et **Shares** : 8 operations, 9 requetes
avec les deux variantes d'upload, anonyme et authentifiee. Les exemples de reponses
du contrat sont conserves.

## Import et utilisation

1. Dans Postman, utiliser **Import** et choisir `DataShare.postman_collection.json`.
2. Facultatif : importer `DataShare.local.postman_environment.json`, puis selectionner
   l'environnement **DataShare - Local**. Il definit seulement `baseUrl` ; les variables
   de la collection suffisent aussi sans environnement.
3. Demarrer le backend sur `http://localhost:8080/api`.
4. Dans les variables de la collection, renseigner `email` et `userPassword`.
   Utiliser un email libre pour une inscription ; une nouvelle inscription avec
   le meme email renvoie `409`. Pour un compte existant, utiliser la connexion.
5. Envoyer l'inscription ou la connexion. Le script de reponse memorise automatiquement
   `jwt`. Les requetes protegees utilisent `Authorization: Bearer {{jwt}}`.
6. Dans un upload, ouvrir **Body > form-data**, puis choisir un fichier local dans
   le champ `file` de type **File**. Le chemin du fichier n'est pas partage dans l'export.
7. Apres un upload reussi, `fileId`, `shareToken` et `downloadLink` sont renseignes.
   `shareToken` est le jeton du lien de partage, distinct du JWT de connexion.

`expirationDays` vaut 7 par defaut, avec des valeurs de 1 a 7. Le champ multipart
`password` est desactive par defaut : le cocher pour proteger le fichier avec
`filePassword`. La variante authentifiee contient deux champs `tags` facultatifs.
La variante anonyme n'envoie ni JWT ni tags. Ne pas ajouter manuellement le header
`Content-Type` multipart : Postman calcule la boundary.

Les routes liste, modification des tags, suppression, consultation du partage et
telechargement figurent dans la collection mais renvoient actuellement `501` dans
le backend. Leurs descriptions le precisent. Elles ne comportent pas d'assertion
automatique de succes. Le telechargement utilise un corps `{}` par defaut ; pour
un fichier protege, fournir `{ "password": "{{filePassword}}" }`.

Les exemples de mots de passe fournis sont des donnees de demonstration. Aucun JWT
reel n'est inclus. Eviter de partager un nouvel export apres avoir renseigne des
identifiants ou obtenu un JWT reel sans vider ces variables.

## Regenerer

Depuis `datashare_backend`, avec Node.js et npm :

```powershell
npx --yes --package=openapi-to-postmanv2@6.4.2 openapi2postmanv2 -s ../najwa-5db-GeneratedAPI_2026-10-13-1137-1.0.0-unresolved.yaml -o postman/DataShare.postman_collection.json -p -O 'folderStrategy=Tags,parametersResolution=Example,includeAuthInfoInExample=false'
node postman/configure-collection.cjs
```

Relancer les deux commandes ensemble : la premiere recree la collection Swagger,
la seconde ajoute les variables, les scripts et la variante d'upload authentifiee.
Cette regeneration remplace les valeurs locales de la collection et de l'environnement.

Conversion realisee avec le [convertisseur officiel Postman](https://github.com/postmanlabs/openapi-to-postman/blob/develop/README.md).
