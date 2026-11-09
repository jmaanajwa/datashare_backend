// Execute apres la conversion OpenAPI pour ajouter les variables et scripts locaux.
const fs = require('node:fs');
const path = require('node:path');

const collectionPath = path.join(__dirname, 'DataShare.postman_collection.json');
const collection = JSON.parse(fs.readFileSync(collectionPath, 'utf8'));
const bearer = { type: 'bearer', bearer: [{ key: 'token', value: '{{jwt}}', type: 'string' }] };
const noauth = { type: 'noauth' };
const vars = [
  ['baseUrl', 'http://localhost:8080/api', 'URL de l API, avec le contexte /api et sans slash final.'],
  ['email', 'postman@example.com', 'Email de test ; utiliser un email libre pour creer un compte.'],
  ['userPassword', 'StrongPass123', 'Mot de passe du compte de test.'],
  ['jwt', '', 'Renseigne automatiquement apres inscription ou connexion.'],
  ['fileId', '', 'UUID du dernier fichier depose.'],
  ['shareToken', '', 'Jeton de partage du dernier fichier depose, distinct du JWT.'],
  ['downloadLink', '', 'Lien retourne apres le dernier upload.'],
  ['filePassword', 'secret123', 'Mot de passe facultatif du fichier, distinct du mot de passe du compte.'],
  ['expirationDays', '7', 'Validite du fichier : de 1 a 7 jours.']
];
collection.variable = vars.map(([key, value, description]) => ({ key, value, description, type: 'string' }));
collection.auth = bearer;
collection.info.name = 'DataShare API - Swagger';
collection.info.description = 'Collection generee depuis le contrat OpenAPI 3.0.3 DataShare. '
  + 'Les 8 operations Swagger sont presentes, avec une variante supplementaire pour l upload authentifie. '
  + 'Base locale : http://localhost:8080/api. Executer une inscription ou une connexion avant les requetes protegees. '
  + 'Selectionner un fichier dans Body > form-data > file avant un upload. '
  + 'Le JWT, fileId et shareToken sont memorises dans les variables de collection. '
  + 'Les routes non implementees sont signalees dans leur description et renvoient actuellement 501.';

function event(listen, code) {
  return { listen, script: { type: 'text/javascript', exec: code.trim().split('\n') } };
}
function description(item, suffix) {
  const current = item.request.description;
  item.request.description = (typeof current === 'string' ? current : current?.content || '') + '\n\n' + suffix;
}
function body(item, data) {
  item.request.body = { mode: 'raw', raw: JSON.stringify(data, null, 2), options: { raw: { language: 'json' } } };
}

for (const folder of collection.item) {
  for (const item of folder.item) {
    const request = item.request;
    const route = request.url.path.join('/');
    request.auth = folder.name === 'Auth' || folder.name === 'Shares' ? noauth : bearer;
    for (const variable of request.url.variable || []) {
      variable.value = variable.key === 'fileId' ? '{{fileId}}' : '{{shareToken}}';
    }
    // Postman calcule lui-meme la boundary multipart ; ne pas fixer Content-Type.
    if (request.body?.mode === 'formdata') {
      request.header = request.header.filter(header => header.key.toLowerCase() !== 'content-type');
    }
    if (folder.name === 'Auth') {
      body(item, { email: '{{email}}', password: '{{userPassword}}' });
      const expected = route.endsWith('register') ? 201 : 200;
      item.event = [
        event('prerequest', "pm.collectionVariables.set('jwt', '');"),
        event('test', `
pm.test('Statut HTTP ${expected}', function () { pm.response.to.have.status(${expected}); });
if (pm.response.code === ${expected}) {
  const data = pm.response.json();
  pm.test('JWT recu', function () { pm.expect(data.token).to.be.a('string').and.not.empty; });
  if (typeof data.token === 'string' && data.token) { pm.collectionVariables.set('jwt', data.token); }
}`)
      ];
    } else if (request.method === 'POST' && route === 'files') {
      item.name = 'Deposer un fichier - anonyme';
      request.auth = noauth;
      request.body.formdata = [
        { key: 'file', type: 'file', src: [], description: 'Choisir un fichier local, maximum 1 000 000 000 octets.' },
        { key: 'expirationDays', type: 'text', value: '{{expirationDays}}' },
        { key: 'password', type: 'text', value: '{{filePassword}}', disabled: true, description: 'Activer pour proteger le fichier.' }
      ];
      description(item, 'Implemente. Seul file est obligatoire. Le mot de passe est desactive par defaut. Aucun JWT ni tag envoye.');
      item.event = [event('test', `
pm.test('Statut HTTP 201', function () { pm.response.to.have.status(201); });
if (pm.response.code === 201) {
  const metadata = pm.response.json().metadata;
  pm.test('Metadonnees recues', function () {
    pm.expect(metadata).to.have.property('id');
    pm.expect(metadata).to.have.property('downloadLink');
  });
  if (metadata && metadata.id) { pm.collectionVariables.set('fileId', metadata.id); }
  if (metadata && metadata.downloadLink) {
    pm.collectionVariables.set('downloadLink', metadata.downloadLink);
    const sharedPath = metadata.downloadLink.split('/shares/')[1];
    pm.collectionVariables.set('shareToken', sharedPath ? decodeURIComponent(sharedPath.split(/[?#]/)[0]) : '');
  }
}`)];
    } else {
      description(item, 'NON IMPLEMENTE dans le backend actuel : reponse HTTP 501 apres authentification si necessaire. '
        + 'Requete conservee pour couvrir le contrat Swagger.');
      if (route === 'shares/:token/download') {
        body(item, {});
        description(item, 'Pour un fichier protege, remplacer le corps par { "password": "{{filePassword}}" }. '
          + 'Utiliser la commande de Postman permettant d enregistrer la reponse pour conserver le fichier binaire.');
      }
    }
  }
}

const files = collection.item.find(folder => folder.name === 'Files');
const anonymous = files.item.find(item => item.request.method === 'POST');
const authenticated = structuredClone(anonymous);
authenticated.id = 'e1984773-309b-4daa-b599-2744c455f91e';
authenticated.name = 'Deposer un fichier - authentifie';
authenticated.request.auth = bearer;
authenticated.request.description = 'Implemente. Envoie Authorization: Bearer {{jwt}}. '
  + 'Le fichier est associe au compte connecte ; tags facultatifs sous forme de champs texte repetes. '
  + 'Choisir un fichier dans Body > form-data > file. Activer password pour proteger le fichier. '
  + 'Memorise fileId, shareToken et downloadLink apres un upload reussi.';
authenticated.request.body.formdata.push(
  { key: 'tags', type: 'text', value: 'projet' },
  { key: 'tags', type: 'text', value: 'confidentiel' }
);
files.item.splice(files.item.indexOf(anonymous) + 1, 0, authenticated);

// Les exemples gardent leurs statuts et corps Swagger, avec la requete locale correspondante.
for (const folder of collection.item) {
  for (const item of folder.item) {
    for (const response of item.response || []) { response.originalRequest = structuredClone(item.request); }
  }
}

fs.writeFileSync(collectionPath, JSON.stringify(collection, null, 2) + '\n');
fs.writeFileSync(path.join(__dirname, 'DataShare.local.postman_environment.json'), JSON.stringify({
  id: '02633383-869d-415d-8ffb-e979231f4c6a',
  name: 'DataShare - Local',
  values: [{ key: 'baseUrl', value: 'http://localhost:8080/api', enabled: true, type: 'default' }],
  _postman_variable_scope: 'environment'
}, null, 2) + '\n');
console.log('Collection configuree : 8 operations Swagger, 9 requetes Postman.');
