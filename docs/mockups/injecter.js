// Chaque maquette porte un marqueur {{NAV:n}} ; ce script y pose la barre
// de navigation partagée, avec la bonne destination active. Une barre
// recopiée dans neuf fichiers finit toujours par diverger.
const fs = require('node:fs');
const path = require('node:path');

const nav = fs.readFileSync(path.join(__dirname, '_nav.html'), 'utf8').trimEnd();

for (const fichier of fs.readdirSync(__dirname).filter((f) => f.endsWith('.html') && !f.startsWith('_'))) {
  const chemin = path.join(__dirname, fichier);
  const source = fs.readFileSync(chemin, 'utf8');
  const remplace = source.replace(/\{\{NAV:(\d)\}\}/g, (_, n) =>
    nav.replace(`class="N${n}"`, `class="N${n} actif"`),
  );
  if (remplace !== source) {
    fs.writeFileSync(chemin, remplace);
    console.log('barre posée :', fichier);
  }
}
