# Changelog

Toutes les modifications notables de ce projet sont documentees dans ce fichier.

Le format est base sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

## [Unreleased]

### Added
- Initialisation du projet Maven/Spring Boot 4 (`pom.xml`, wrapper Maven `mvnw`/`mvnw.cmd`) avec les dependances necessaires au workflow de tri de photos : `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `springdoc-openapi-starter-webmvc-ui`, pilotes SQLite (profil `local`) et PostgreSQL (profil `docker`), TwelveMonkeys ImageIO (decodage JPEG) et metadata-extractor (lecture EXIF).
- Configuration Spring par profils : `application.yml` (base), `application-local.yml` (SQLite, sans dependance Docker), `application-docker.yml` (PostgreSQL).
- Executeur mono-thread (`AsyncConfig`) dedie au futur job d'analyse/traitement en tache de fond, garantissant qu'un seul workflow tourne a la fois.
- Modele de donnees JPA portable SQLite/PostgreSQL : entites `Event`, `AnalysisJob`, `PhotoAsset`, `ProcessingResult` et leurs repositories Spring Data paginees.
- Diagramme de modelisation `docs/database.puml` decrivant ce modele.
- Machine a etats du workflow (`WorkflowStateService`) et verrou "un seul dossier a la fois" : demarrage d'analyse (`POST /api/events/{folderName}/analysis`), consultation du statut (`GET /api/workflow/status`) et annulation/reset manuel (`POST /api/workflow/cancel`), avec rejet HTTP 409 si un autre workflow est deja actif.
- Listage des dossiers evenement disponibles sous le point de montage NAS source (`GET /api/events`).
- Documentation Swagger/OpenAPI (`@Tag`, `@Operation`, `@ApiResponse`) sur les nouveaux endpoints `events` et `workflow`.
- Job d'analyse en tache de fond (`photo-analysis`) : instantane fige des photos JPEG d'un dossier au demarrage (`AnalysisOrchestrator`), calcul par photo du score de nettete (variance du Laplacien) et d'un hash perceptuel average-hash 64 bits (`ImageAnalysisService`), traitement asynchrone avec checkpoint de progression tous les ~20 photos (`PhotoAnalysisRunner`), reprise manuelle sans recalcul des photos deja analysees (`POST /api/jobs/{id}/resume`), et suivi de progression par polling (`GET /api/jobs/{id}`).
- Onglet "Flou" (`blur-review`) : listing pagine des photos floues (`GET /api/jobs/{id}/blurred`), pre-cochees pour suppression par defaut, bascule keep/delete par photo (`PATCH /api/photos/{id}`), et page IHM vanilla JS/HTML/CSS avec vignettes en chargement paresseux (`static/flou.html`).

### Changed
- `Dockerfile` : ajout d'options JVM sobres pour Raspberry Pi 4 (`-XX:+UseSerialGC`, `-Xmx384m`, `-XX:MaxMetaspaceSize=128m`).
