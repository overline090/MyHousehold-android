// Deux modules, et la séparation compte :
//
//   core — Kotlin pur, sans Android. Le domaine du foyer : les trois
//          sortes de tâches, les priorités, les cinq formes de
//          répétition, l'état d'une tâche de fond. Il se compile et se
//          teste sans le SDK Android, donc partout.
//   app  — l'application elle-même : écrans, base locale, réseau.
//
// Les dépôts Google ne sont interrogés que pour ce qui leur appartient :
// un environnement qui ne les atteint pas peut quand même construire et
// tester `core`.

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "MyHousehold"
include(":core")
include(":app")
