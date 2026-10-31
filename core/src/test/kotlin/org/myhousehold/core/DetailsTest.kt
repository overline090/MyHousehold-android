package org.myhousehold.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DetailsTest {
    @Test
    fun `lit la mise en forme en blocs`() {
        val blocs = lireDetails(
            "<p><b>Appeler</b> <mark style=\"background-color: #fde68a\">vite</mark></p>" +
                "<ul><li>Fusible <span style=\"color: #b91c1c\">16 A</span></li><li data-case=\"oui\">Lampe</li></ul>",
        )
        assertEquals(3, blocs.size)
        val p = blocs[0] as Bloc.Paragraphe
        assertEquals("Appeler vite", p.texte)
        assertTrue(p.morceaux.first().style.gras)
        assertEquals("#fde68a", p.morceaux.last().style.surligneur)
        val e = blocs[1] as Bloc.Element
        assertEquals("#b91c1c", e.morceaux.last().style.couleur)
        assertEquals(null, e.cochee)
        val c = blocs[2] as Bloc.Element
        assertEquals(true, c.cochee)
        assertEquals(0, c.rangDeCase)
    }

    @Test
    fun `numérote les listes et lit les styles en ligne`() {
        val blocs = lireDetails("<ol><li>un</li><li><span style=\"font-weight: bold\">deux</span> &amp; trois</li></ol>")
        assertEquals(listOf(1, 2), blocs.map { (it as Bloc.Element).numero })
        assertTrue(blocs[1].morceaux.first().style.gras)
        assertEquals("deux & trois", blocs[1].texte)
    }

    @Test
    fun `cocher une case ne touche qu'elle`() {
        val html = "<ul><li data-case=\"non\">a</li><li data-case=\"non\">b</li></ul>"
        assertEquals("<ul><li data-case=\"non\">a</li><li data-case=\"oui\">b</li></ul>", basculerCase(html, 1, true))
    }

    @Test
    fun `reconnaît téléphones, adresses, liens et codes`() {
        val texte = "Appeler au 06 12 34 56 78. Compteur : 12 rue des Lilas, 69003 Lyon\nCode du portail : 4821B, contrat CT-204817, https://exemple.fr/a."
        val trouvees = detecter(
            texte,
            ReglagesDeDetection(
                regles = listOf(
                    RegleDeCopie("p", "Code du portail", "MOT_CLE", "Code du portail"),
                    RegleDeCopie("c", "Contrat", "FORMAT", "CT-######"),
                ),
            ),
        )
        assertEquals(
            listOf(
                SorteTrouvee.TELEPHONE to "0612345678",
                SorteTrouvee.ADRESSE to "12 rue des Lilas, 69003 Lyon",
                SorteTrouvee.COPIE to "4821B",
                SorteTrouvee.COPIE to "CT-204817",
                SorteTrouvee.LIEN to "https://exemple.fr/a",
            ),
            trouvees.map { it.sorte to it.valeur },
        )
    }

    @Test
    fun `les cases passent par l'éditeur de l'app sans se perdre`() {
        val html = "<ul><li data-case=\"oui\">a</li><li data-case=\"non\">b</li><li>c</li></ul>"
        val pourEditeur = casesVersSymboles(html)
        assertEquals("<ul><li>☑ a</li><li>☐ b</li><li>c</li></ul>", pourEditeur)
        assertEquals(html, symbolesVersCases(pourEditeur))
        // Ce que l'éditeur écrit, avec un paragraphe dans l'élément.
        assertEquals(
            "<ul><li data-case=\"non\"><p>lait</p></li></ul>",
            symbolesVersCases("<ul><li><p>☐ lait</p></li></ul>"),
        )
    }
}
