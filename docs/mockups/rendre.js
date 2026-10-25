const fs = require('node:fs');
const path = require('node:path');
const { chromium } = require('playwright');

// Rend chaque maquette en PNG à la taille d'un téléphone, et signale
// tout débordement — une maquette qui déborde ment sur ce qu'elle
// promet.
const SRC = __dirname;
const OUT = process.env.SORTIE ?? path.join(__dirname, 'png');
fs.mkdirSync(OUT, { recursive: true });

(async () => {
  const navigateur = await chromium.launch();
  const fichiers = fs.readdirSync(SRC).filter((f) => f.endsWith('.html') && !f.startsWith('_'));

  for (const fichier of fichiers.sort()) {
    const page = await navigateur.newPage({
      viewport: { width: 390, height: 844 },
      deviceScaleFactor: 2,
    });
    await page.goto('file://' + path.join(SRC, fichier));
    await page.waitForLoadState('networkidle');
    try { await page.evaluate(() => document.fonts.ready); } catch {}

    const debordement = await page.evaluate(() => ({
      h: document.documentElement.scrollWidth > 390,
      v: document.body.scrollHeight > 844,
    }));

    const sortie = path.join(OUT, fichier.replace('.html', '.png'));
    await page.screenshot({ path: sortie });
    await page.close();
    console.log(
      fichier.replace('.html', '.png'),
      debordement.h ? '⚠ déborde en largeur' : '',
      debordement.v ? '⚠ contenu coupé en hauteur' : '',
    );
  }

  await navigateur.close();
})();
