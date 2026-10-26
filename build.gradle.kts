// La racine n'applique rien : elle déclare seulement les greffons
// Kotlin une bonne fois, pour qu'ils ne soient pas chargés deux fois —
// une fois par sous-projet — ce que Gradle signale à juste titre.
//
// Le greffon Android, lui, reste déclaré dans `app` : il vient du dépôt
// de Google, et le module `core` doit pouvoir se construire sans.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
}
