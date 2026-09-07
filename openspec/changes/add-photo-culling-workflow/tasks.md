## 1. Project bootstrap

- [x] 1.1 Initialize Maven project (`pom.xml`, `mvnw`/`mvnw.cmd`, `.mvn/`) with Spring Boot 4.x parent, Java 25 target, matching the existing `Dockerfile` expectations (`target/dedoublonneur-*.jar`, `src/main/resources/static`)
- [x] 1.2 Add dependencies: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `springdoc-openapi-starter-webmvc-ui`, SQLite JDBC driver + Hibernate dialect, PostgreSQL JDBC driver, TwelveMonkeys ImageIO (JPEG plugin), metadata-extractor (drewnoakes)
- [x] 1.3 Create `application.yml` base config plus `application-local.yml` (SQLite, NAS bind mount path) and `application-docker.yml` (PostgreSQL) profiles
- [x] 1.4 Configure a single-thread `ThreadPoolTaskExecutor` bean (core=1, max=1, reject-on-full policy) for the analysis/processing background job
- [x] 1.5 Configure JVM/GC options appropriate for Raspberry Pi 4 constraints (documented in README, e.g. `-XX:+UseSerialGC` guidance for the `docker` profile entrypoint)

## 2. Data model

- [x] 2.1 Create `@Entity` classes: `Event`, `AnalysisJob`, `PhotoAsset`, `ProcessingResult`, using portable types/generation strategy (SQLite + PostgreSQL compatible, `GenerationType.SEQUENCE`/`AUTO`)
- [x] 2.2 Create Spring Data JPA repositories for each entity, including paginated finder methods for `PhotoAsset` (by job + blurred flag)
- [x] 2.3 Verify `ddl-auto` schema generation works identically against SQLite (local) and PostgreSQL (docker/test) profiles
- [x] 2.4 Update `docs/database.puml` PlantUML diagram to reflect the new entities and relationships

## 3. Event folder & workflow lock (event-folder-workflow)

- [x] 3.1 Implement `WorkflowStateService` exposing current status (`IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, `DONE`) backed by the single active `AnalysisJob` row
- [x] 3.2 Implement `GET /api/events` listing sub-folders under the configured NAS source mount
- [x] 3.3 Implement `POST /api/events/{folderName}/analysis` to start analysis, rejecting the request (409) if another workflow is active
- [x] 3.4 Implement `POST /api/workflow/cancel` to manually reset a stuck workflow back to `IDLE`
- [x] 3.5 Add Swagger/OpenAPI annotations (`@Tag`, `@Operation`, `@ApiResponse`) to all workflow endpoints
- [x] 3.6 Unit tests: lock rejection when active, state transitions, cancel/reset behavior

## 4. Photo analysis job (photo-analysis)

- [x] 4.1 Implement file-snapshot logic: list JPEG files in the selected folder at job start, persist as the job's fixed scope
- [x] 4.2 Implement blur score computation (grayscale downscale + Laplacian variance) using TwelveMonkeys ImageIO, without rewriting source files
- [x] 4.3 Implement pHash computation (DCT/average-hash on downscaled grayscale image) and persist as a `PhotoAsset` field
- [x] 4.4 Implement the `@Async` analysis task iterating the snapshot, persisting a checkpoint (last processed count) every ~20 photos
- [x] 4.5 Implement manual resume: on-demand endpoint that continues an interrupted job from its last checkpoint without recomputing existing `PhotoAsset` rows
- [x] 4.6 Implement `GET /api/jobs/{id}` progress endpoint (status, analyzedCount, totalCount) for polling
- [x] 4.7 Add Swagger/OpenAPI annotations to analysis endpoints
- [x] 4.8 Unit tests: blur score computation, pHash computation, checkpoint persistence, resume-skips-existing-photos, ignores files added after snapshot

## 5. Blur review tab (blur-review)

- [ ] 5.1 Implement `GET /api/jobs/{id}/blurred` paginated endpoint returning blurry photos (below blur threshold) with default `toDelete=true`
- [ ] 5.2 Implement `PATCH` endpoint (or per-photo toggle endpoint) to flip a photo's keep/delete state
- [ ] 5.3 Add Swagger/OpenAPI annotations
- [ ] 5.4 Frontend: "Flou" tab UI (vanilla JS/HTML/CSS) with paginated, lazily-loaded (`loading="lazy"`) thumbnail grid and checkboxes pre-checked for deletion, using the shared UI utilities from section 9
- [ ] 5.5 Unit tests: default pre-checked state, pagination, toggle persistence

## 6. Duplicate review tab (duplicate-review)

- [ ] 6.1 Implement Hamming-distance-based union-find clustering service operating over persisted `PhotoAsset.pHash` values
- [ ] 6.2 Implement `GET /api/jobs/{id}/duplicates?threshold=NN` computing groups on demand (no persistence), with sharpest photo marked `keep` and others `toDelete=true` by default, ties broken by filename/date order
- [ ] 6.3 Implement per-photo toggle to override the default keep/delete selection within a group
- [ ] 6.4 Add a short-lived in-memory cache keyed by (jobId, threshold) to avoid recomputation on repeated identical requests
- [ ] 6.5 Add Swagger/OpenAPI annotations
- [ ] 6.6 Frontend: "Doublons" tab UI with adjustable similarity threshold control (0-100%) using debounced input (~300ms), lazily-loaded grouped thumbnail display, checkboxes
- [ ] 6.7 Unit tests: clustering correctness at various thresholds, default best-photo selection, tie-break rule, override toggle

## 7. Folder processing (folder-processing)

- [ ] 7.1 Implement output folder name validation (`POST /api/jobs/{id}/process` rejects if target folder already exists under the NAS output location)
- [ ] 7.2 Implement raw byte copy (`Files.copy` with `COPY_ATTRIBUTES`) of all kept photos (not marked for deletion in either review tab) to the output folder
- [ ] 7.3 Implement unconditional copy of all video files found in the source folder to the output folder, excluded from kept/deleted counts
- [ ] 7.4 Compute and persist `ProcessingResult` (kept/deleted counts, space before/after, output path); transition workflow to `DONE`
- [ ] 7.5 Purge the ephemeral thumbnail cache for the processed job
- [ ] 7.6 Add Swagger/OpenAPI annotations
- [ ] 7.7 Frontend: "Traiter le dossier" action with output folder name prompt and final recap display
- [ ] 7.8 Unit tests: rejects existing output folder, metadata preservation (EXIF/timestamps unchanged after copy), video pass-through, deleted photos absent from output and source untouched, recap accuracy

## 8. Thumbnails

- [ ] 8.1 Implement thumbnail generation during analysis, cached under local ephemeral disk (`${java.io.tmpdir}/thumbnails/{jobId}/{photoId}.jpg`)
- [ ] 8.2 Implement `GET /api/photos/{id}/thumbnail` serving from cache, regenerating on cache miss
- [ ] 8.3 Unit tests: cache miss regeneration, cache purge after processing

## 9. Frontend performance & UX polish (responsive-ui)

- [ ] 9.1 Implement a small shared vanilla-JS utility module: debounce helper, lazy-load/infinite-scroll pagination helper, `fetch`-based partial-panel swapping (no full page reloads)
- [ ] 9.2 Implement CSS-only transitions/animations (opacity/transform) for tab switching, progress bar updates, and checkbox toggle feedback
- [ ] 9.3 Verify no SPA framework, bundler, or heavy JS/CSS library is introduced; keep total static asset payload minimal
- [ ] 9.4 Manual/perf check on representative hardware (or throttled browser profile) that review tabs with 500-2000 photos stay responsive while scrolling and while dragging the similarity threshold
- [ ] 9.5 Unit/UI tests: debounce delays the recomputation call as expected, lazy-load only fetches visible-page thumbnails

## 10. Documentation & project hygiene

- [ ] 10.1 Update root `README.md` with the functional workflow description (event selection, analysis, review tabs, processing, recap)
- [ ] 10.2 Update root `CHANGELOG.md` under `[Added]` describing the new photo-culling workflow feature
- [ ] 10.3 Provide a ready-to-use `docker-compose.yml` example (repo root or `docs/`) with two services — the app (built from the existing `Dockerfile`, `docker`/prod Spring profile) and `postgres` — including the NAS bind mount (source + output folders) and a named volume for PostgreSQL data persistence, plus the required environment variables (DB host/credentials, active profile, NAS paths)
- [ ] 10.4 Write a step-by-step **"Déploiement sur Arcane"** guide in `README.md` (or a linked `docs/DEPLOYMENT.md`), written for a non-expert user, covering:
  - Prérequis : Arcane accessible sur le Raspberry Pi, Docker fonctionnel, partage NAS déjà monté/accessible depuis l'hôte
  - Où récupérer l'image Docker (built automatiquement par la GitHub Action à chaque push) et comment référencer le bon tag/`latest` dans le compose
  - Créer la stack dans Arcane : Stacks → Add stack → coller le contenu du `docker-compose.yml` fourni → renseigner les variables d'environnement (identifiants PostgreSQL, chemins NAS) via l'UI Arcane → Deploy
  - Vérifications au premier démarrage : statut "healthy" des conteneurs dans Arcane, logs de l'app confirmant la création du schéma Hibernate, accès à l'IHM via `http://<ip-raspberry>:8686`
  - Mettre à jour l'application après un nouveau commit/push : comment redéployer la stack dans Arcane pour tirer la nouvelle image (pull + redeploy du service app, sans toucher au service PostgreSQL/volume de données)
  - Dépannage courant : partage NAS non visible dans le conteneur, erreur de connexion PostgreSQL, conteneur en boucle de redémarrage — où regarder les logs dans Arcane
- [ ] 10.5 Verify all new REST endpoints run and are tested via Maven at `C:\USINE_LOGICIELLE\apache-maven\bin`
