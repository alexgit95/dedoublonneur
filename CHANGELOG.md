# Changelog

Toutes les modifications notables de ce projet sont documentees dans ce fichier.

Le format est base sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

## [Unreleased]

### Added
- Seuil de similarite configurable via `APP_DEFAULT_SIMILARITY_THRESHOLD` (fallback 90), conservation de la valeur par job, separation du contexte actif et du dernier recapitulatif apres export, retraitement fiable des jobs `DONE` et annulation des exports en erreur.
- Hierarchie visuelle de revue corrigee : les photos conservees sont mises en avant, les photos marquees pour suppression sont attenuees, et l'ecran de selection signale les dossiers deja exportes (`DONE`) sans les desactiver.
- Theme de revue noir et rouge neon inspire de Tron/Ares, avec apercu plein ecran temporaire des thumbnails par maintien du pointeur/touch dans les onglets Flou et Doublons, sans telechargement de l'original.
- Bouton de reinitialisation du workflow pendant l'analyse ou la revue : arret cooperatif du runner, suppression des analyses et vignettes du job, purge du cache de doublons et retour au choix du dossier ; l'export reste volontairement non interrompu.
- Support des photos PNG et detection des quasi-doublons de rafale : le snapshot accepte JPEG/PNG et l'analyse utilise desormais un pHash DCT 64 bits, avec un seuil initial de 90% (distance maximale de 6 bits) ajustable pendant la revue manuelle.
- **Interface de workflow guidee** (`improve-guided-workflow-ui`) : landing page a la racine, fil d'etapes persistant, ecran unique avec onglets Flou/Doublons, reprise automatique selon le statut du workflow et recapitulatif d'export integre.
- **Workflow complet de tri de photos** (`add-photo-culling-workflow`) : selection d'un dossier evenement, analyse en tache de fond (flou + doublons), revue manuelle (onglets Flou/Doublons), puis traitement (copie vers un dossier de sortie avec preservation des metadonnees et recapitulatif). Detail des sous-fonctionnalites ci-dessous.
- Initialisation du projet Maven/Spring Boot 4 (`pom.xml`, wrapper Maven `mvnw`/`mvnw.cmd`) avec les dependances necessaires au workflow de tri de photos : `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `springdoc-openapi-starter-webmvc-ui`, pilotes SQLite (profil `local`) et PostgreSQL (profil `docker`), TwelveMonkeys ImageIO (decodage JPEG) et metadata-extractor (lecture EXIF).
- Configuration Spring par profils : `application.yml` (base), `application-local.yml` (SQLite, sans dependance Docker), `application-docker.yml` (PostgreSQL).
- Executeur mono-thread (`AsyncConfig`) dedie au futur job d'analyse/traitement en tache de fond, garantissant qu'un seul workflow tourne a la fois.
- Modele de donnees JPA portable SQLite/PostgreSQL : entites `Event`, `AnalysisJob`, `PhotoAsset`, `ProcessingResult` et leurs repositories Spring Data paginees.
- Diagramme de modelisation `docs/database.puml` decrivant ce modele.
- Machine a etats du workflow (`WorkflowStateService`) et verrou "un seul dossier a la fois" : demarrage d'analyse (`POST /api/events/{folderName}/analysis`), consultation du statut (`GET /api/workflow/status`) et annulation/reset manuel (`POST /api/workflow/cancel`), avec rejet HTTP 409 si un autre workflow est deja actif.
- Listage des dossiers evenement disponibles sous le point de montage NAS source (`GET /api/events`).
- Documentation Swagger/OpenAPI (`@Tag`, `@Operation`, `@ApiResponse`) sur les nouveaux endpoints `events` et `workflow`.
- Job d'analyse en tache de fond (`photo-analysis`) : instantane fige des photos JPEG ou PNG d'un dossier au demarrage (`AnalysisOrchestrator`), calcul par photo du score de nettete (variance du Laplacien) et d'un pHash DCT 64 bits (`ImageAnalysisService`), traitement asynchrone avec checkpoint de progression tous les ~20 photos (`PhotoAnalysisRunner`), reprise manuelle sans recalcul des photos deja analysees (`POST /api/jobs/{id}/resume`), et suivi de progression par polling (`GET /api/jobs/{id}`).
- Onglet "Flou" (`blur-review`) : listing pagine des photos floues (`GET /api/jobs/{id}/blurred`), pre-cochees pour suppression par defaut, bascule keep/delete par photo (`PATCH /api/photos/{id}`), et page IHM vanilla JS/HTML/CSS avec vignettes en chargement paresseux (`static/flou.html`).
- Onglet "Doublons" (`duplicate-review`) : clustering par similarite de hash perceptuel calcule a la demande (union-find, `DuplicateClusterService`) avec cache memoire par (job, seuil), endpoint `GET /api/jobs/{id}/duplicates?threshold=NN`, selection par defaut de la photo la plus nette de chaque groupe (egalite departagee par nom de fichier), bascule manuelle toujours respectee meme apres changement de seuil (`PhotoAsset.deletionOverridden`), et page IHM avec curseur de seuil debouonce (~300ms, `static/doublons.html`).
- Traitement du dossier (`folder-processing`) : `POST /api/jobs/{id}/process` valide le nom de dossier de sortie (rejet si deja existant ou job non pret), copie brute (`Files.copy` + `COPY_ATTRIBUTES`) des photos conservees et de toutes les videos vers la sortie sans jamais modifier la source, calcule et persiste le recapitulatif (`ProcessingResult`, `GET /api/jobs/{id}/result`), purge le cache de vignettes ephemere et le cache de clustering de doublons du job traite, et page IHM "Traiter le dossier" avec recapitulatif final (`static/traiter.html`).
- Vignettes (`ThumbnailService`) : generation pendant l'analyse, cache disque ephemere `${java.io.tmpdir}/thumbnails/{jobId}/{photoId}.jpg`, endpoint `GET /api/photos/{id}/thumbnail` avec regeneration a la demande en cas d'absence du cache.
- Module JS partage `static/js/shared.js` (debounce, construction d'URL paginee, carte photo avec vignette en chargement paresseux) reutilise par les pages Flou/Doublons/Traitement, barre de progression animee en CSS pendant le traitement, et tests legers sans dependance (`src/test/js/shared.test.js`, executable via `node`).
- Documentation fonctionnelle du workflow dans `README.md`, `docker-compose.yml` pret a l'emploi (app + PostgreSQL, montage NAS, volume de donnees) et guide de deploiement pas-a-pas sur Arcane.

### Changed
- `WorkflowStateService` : le dernier workflow termine reste visible avec son contexte `DONE`, afin que l'interface puisse afficher le recapitulatif et proposer un nouveau dossier.
- **Verrou SQLite au premier demarrage** : serialisation du lancement d'analyse et attente de verrou de 30 secondes dans le profil local ; le mode WAL est evite car il provoquait `SQLITE_BUSY_SNAPSHOT` avec les transactions Hibernate de creation d'identifiants.
- `Dockerfile` : ajout d'options JVM sobres pour Raspberry Pi 4 (`-XX:+UseSerialGC`, `-Xmx384m`, `-XX:MaxMetaspaceSize=128m`).
