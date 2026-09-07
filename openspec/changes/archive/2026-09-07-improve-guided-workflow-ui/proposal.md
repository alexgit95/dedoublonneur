## Why

L'application expose aujourd'hui des pages de revue isolées, accessibles avec un `jobId` dans l'URL, sans page d'accueil ni indication de l'étape courante. Un utilisateur ne sait donc pas où commencer, comment passer de l'analyse à la revue, ni comment reprendre un workflow déjà lancé. Cette amélioration rend le workflow photo découvrable et guidé depuis la racine de l'application.

## What Changes

- Ajouter une landing page à la racine `/` permettant de lister les dossiers événement disponibles et d'en lancer l'analyse.
- Remplacer le parcours composé de pages isolées par un écran de workflow unique avec des onglets pour le dossier, l'analyse, la revue et l'export.
- Regrouper les écrans Flou et Doublons dans deux onglets d'une même étape de revue.
- Ajouter un fil d'Ariane persistant indiquant le dossier courant, l'étape active, les étapes terminées et l'action suivante.
- Afficher automatiquement l'état du workflow existant et permettre sa reprise selon les statuts `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING` et `DONE`.
- Fournir des transitions claires entre les étapes sans demander à l'utilisateur de manipuler les URLs ou le `jobId`.
- Conserver l'implémentation frontend légère en HTML, CSS et JavaScript vanilla, sans framework ni étape de bundling.

## Capabilities

### New Capabilities

- `guided-workflow-ui`: Landing page, écran de workflow unifié, onglets de revue, fil d'Ariane et reprise guidée du workflow.

### Modified Capabilities

- `event-folder-workflow`: Le système doit exposer la sélection du dossier et la reprise du workflow via l'interface racine, avec une navigation utilisateur correspondant aux états du workflow.

## Impact

- Nouvelles ressources statiques sous `src/main/resources/static`, dont la page racine, les scripts de navigation et les styles partagés.
- Adaptation des pages Flou, Doublons et Traitement pour être intégrées dans l'écran de workflow et recevoir leur contexte depuis la navigation commune.
- Utilisation des API existantes de dossiers, jobs et statut global ; une évolution mineure pourra être nécessaire pour retourner les informations de contexte nécessaires à la reprise.
- Mise à jour de la documentation fonctionnelle et ajout de tests JavaScript et/ou d'intégration couvrant les états et transitions visibles.
