## Context

Greenfield project (only `doc/INIT.MD` and a `Dockerfile` exist today). The Dockerfile already commits the project to:
- Spring Boot backend (Java 25), packaged as a single jar.
- A **vanilla HTML/JS/CSS frontend** served from `src/main/resources/static` — no separate SPA build stage.
- Deployment target: Raspberry Pi 4 (4 GB RAM shared with OS/containers), via Docker Compose orchestrated by Arcane.
- `local` Spring profile → SQLite; `docker`/prod profile → PostgreSQL. No migration tool (Liquibase/Flyway); schema is generated via Hibernate `ddl-auto`, so entities must stay portable across both dialects.

The functional need (see `doc/INIT.MD`) and the exploration that preceded this proposal fixed the following decisions:
- Only JPEG photos are in scope (no HEIC/RAW).
- Duplicate grouping must support **both** exact copies and near-duplicate bursts, via perceptual hashing (pHash) with a **user-adjustable similarity threshold (0-100%)**.
- The "best" photo in a duplicate group is chosen purely by sharpness score; ties broken by filename/date order.
- The similarity threshold must be adjustable **after** analysis completes, with instant re-clustering — this means duplicate groups are never persisted, only the raw pHash per photo.
- Analysis must be resumable after a container restart, at batch granularity (~20 photos), but resuming requires a manual user action (no auto-resume on boot).
- Only one workflow (analysis → review → processing) may be active across the whole app at any time.
- No authentication; NAS mount path is a fixed Docker bind mount, not user-configurable.
- Thumbnails are generated during analysis, cached on ephemeral local container disk, and purged once a folder is processed.

## Goals / Non-Goals

**Goals:**
- Deliver a working end-to-end workflow: select event folder → background analysis (blur + pHash) → review blurry/duplicate tabs → process into an output folder with recap.
- Keep the implementation lightweight enough to run comfortably within the Pi 4's shared 4 GB RAM (pagination, streaming file copy, no heavy CV/ML dependencies).
- Deliver a UI that feels modern and fluid (smooth transitions, instant-feeling interactions, no jarring full-page reloads) while remaining framework-free and asset-light so it stays fast to serve and render on the Pi and on modest client devices.
- Guarantee 100% preservation of EXIF/GPS/date metadata on every photo that is copied to the output folder.
- Keep entities/DDL portable between SQLite (local) and PostgreSQL (prod).

**Non-Goals:**
- No HEIC/RAW support in this iteration.
- No authentication/authorization.
- No auto-resume of interrupted jobs on container startup.
- No persistence of duplicate groupings (only raw pHash values are persisted; grouping is always computed on demand).
- No multi-user concurrency handling beyond the single global workflow lock (no per-user sessions).

## Decisions

### 1. Single global workflow lock via a DB-backed state machine + single-thread executor
A `WorkflowState` (or a singleton-row `AnalysisJob` table) tracks the current phase: `IDLE → ANALYZING → READY_FOR_REVIEW → PROCESSING → DONE`. The background analysis and processing tasks run through Spring's `@Async` with a `ThreadPoolTaskExecutor` configured with **core=1, max=1, unbounded queue rejected (reject new submissions while busy)**. This makes the thread pool itself act as a natural single-job enforcer, while the DB row makes the lock state visible/queryable and durable across restarts.
- *Alternative considered*: Quartz scheduler — rejected as overkill for a single always-serial job with no cron/multi-job needs.

### 2. Perceptual hashing: hand-rolled DCT-based pHash, no native/JNI dependency
Implemented in pure Java (downscale to a small fixed grid via `TwelveMonkeys ImageIO`, grayscale, DCT or average-hash, encode as a `long`/bit string). Avoids pulling OpenCV or other native ARM-sensitive dependencies onto the Pi.
- Similarity is computed via Hamming distance between two hashes, then converted to a **0-100% similarity** for the UI (`similarity% = 100 * (1 - hammingDistance / hashBitLength)`).
- *Alternative considered*: an existing Java image-hashing library — rejected in favor of a small in-house implementation to keep full control over performance/footprint on ARM and avoid unmaintained dependencies.

### 3. Duplicate groups are computed on demand, never persisted
Only `PhotoAsset.pHash` (and `blurScore`) are persisted per photo. A `GET /api/events/{id}/duplicates?threshold=NN` endpoint performs clustering (union-find over pairwise Hamming distances within the threshold) at request time over the event's photos, returning groups with a `keep`/`delete` default flag per photo (sharpest = keep). This trades a bit of CPU per request for full flexibility on threshold changes without re-analysis.
- Given the 500-2000 photo/event volume, an O(n²) pairwise comparison per request is acceptable (≤ ~2M comparisons of cheap integer XOR+popcount) but should be bounded/paginated in the response.

### 4. Blur score: variance of Laplacian on a downscaled grayscale image
Cheap CPU-only algorithm: decode via `TwelveMonkeys ImageIO`, downscale to a small fixed max-dimension, convert to grayscale, convolve with a Laplacian kernel, take the variance of the result. Lower variance = blurrier. No ML/deep-learning dependency.

### 5. Checkpointing at batch granularity (~20 photos)
The `AnalysisJob` snapshots the file list at start (ignoring files added later) and persists a `lastProcessedIndex` (or count) after every batch of ~20 photos, alongside each `PhotoAsset`'s computed scores. On manual resume, the job skips already-persisted `PhotoAsset` rows and continues from the next unprocessed file in the snapshot.

### 6. Metadata preservation via raw byte copy
`folder-processing` copies kept photos and all videos using `java.nio.file.Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)` — no image is ever decoded-and-re-saved during processing. EXIF/GPS is embedded in the original bytes and is preserved automatically. `metadata-extractor` is used **only for reading** EXIF (e.g., to display capture date in the UI), never for writing.

### 7. Thumbnails: generated during analysis, ephemeral local disk cache
Stored under a local temp/cache directory inside the container (e.g., `${java.io.tmpdir}/thumbnails/{eventId}/{photoId}.jpg`), not on the NAS. Served via a REST endpoint that reads from this cache (falls back to regenerating on cache miss, e.g., after a container restart). Deleted when the folder-processing step completes.

### 8. Frontend performance: modern feel without framework overhead
The UI stays vanilla HTML/CSS/JS (per the existing `Dockerfile`/static-resources constraint) but achieves a modern, fluid feel through cheap techniques rather than a JS framework:
- **Lazy-loaded thumbnails** (`loading="lazy"` + paginated/infinite-scroll fetch) so review tabs never render/download hundreds of images at once.
- **Debounced similarity-threshold slider**: recompute requests to `/duplicates?threshold=NN` are debounced (e.g., ~300ms after the user stops dragging) instead of firing on every slider tick, avoiding request storms and redundant clustering work on the server.
- **CSS-only transitions/animations** (opacity/transform, GPU-composited) for tab switches, progress bars, and checkbox state changes — no animation libraries.
- **No client-side routing/SPA framework**: a handful of static pages with `fetch`-based partial updates (swap a results panel, not full navigation) keep the JS bundle tiny and parse/execute cost low on low-power client hardware.
- **Server-side pagination** (already required for RAM reasons) doubles as the mechanism keeping the DOM small, which is what actually keeps scrolling/interaction fluid in the browser.
- *Alternative considered*: a modern JS framework (React/Vue) with a build step — rejected, since it would change the existing Dockerfile's single-stage static-resources packaging and add build tooling/runtime weight not justified by this app's UI complexity.

### 9. Data model (Hibernate, SQLite + PostgreSQL portable)
```
Event          (id, folderName, folderPath)
AnalysisJob    (id, event_id FK, status, snapshotSize, lastProcessedCount, similarityThresholdDefault, startedAt, finishedAt)
PhotoAsset     (id, job_id FK, relativePath, fileSize, blurScore, pHash, isBlurredFlag, analyzedAt)
ProcessingResult (id, job_id FK, keptCount, deletedCount, videoCount, spaceBeforeBytes, spaceAfterBytes, outputFolderPath, processedAt)
```
- `GenerationType.AUTO` is used for all entities (Hibernate's table-based hi/lo id generator, the only strategy found to produce correct, portable DDL on both SQLite and PostgreSQL for this Hibernate/dialect version combination). Integration tests must NOT wrap DB-writing test methods in `@Transactional` rollback: the hi/lo generator bumps its counter via its own nested transaction, which deadlocks against SQLite's single-writer lock if an outer test transaction is left open for the whole test method. Tests instead clean up explicitly (`@AfterEach` deleting rows, children before parents) between cases.
- Any service/component that loads an entity in one short transaction and later navigates one of its lazy (`FetchType.LAZY`) associations in a different call MUST instead use a dedicated repository `@Query` selecting the needed field directly (e.g. `select j.event.folderPath from AnalysisJob j where j.id = :jobId`), to avoid `LazyInitializationException` on detached entities.
- `pHash` stored as a `BIGINT`/`long` (or string of hex) — avoid Postgres-only types (no `jsonb`, no arrays).
- Only one `AnalysisJob` may be in a non-terminal status (`ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`) at a time — enforced at the service layer (check-then-create under the single-thread executor), not via a DB constraint, since SQLite/PostgreSQL portable partial-unique-index syntax differs.

### 10. Progress reporting: HTTP polling
`GET /api/jobs/{id}` returns `{status, analyzedCount, totalCount}`. Frontend polls every few seconds while `status == ANALYZING`. No WebSocket/SSE — keeps the vanilla JS frontend simple and avoids extra infra on a resource-constrained Pi.

### 11. Pagination
Review-tab endpoints (`/duplicates`, `/blurred`) are paginated (Spring `Pageable`) to avoid loading hundreds of `PhotoAsset` rows (with thumbnails metadata) into memory at once, per the project's RAM constraints.

## Risks / Trade-offs

- **[Risk]** On-demand O(n²) duplicate clustering could be slow for events near 2000 photos → **Mitigation**: cheap integer Hamming distance (XOR + popcount) keeps per-pair cost negligible; add a short-lived in-memory cache of the last computed clustering per (event, threshold) to avoid recomputation on repeated identical requests.
- **[Risk]** Hand-rolled pHash may be less accurate than a mature library → **Mitigation**: keep the hash algorithm isolated behind an interface so it can be swapped later without touching persisted-data format (store hash + algorithm version).
- **[Risk]** Ephemeral thumbnail cache means a container restart mid-review forces thumbnail regeneration → **Mitigation**: acceptable per explicit product decision; regeneration is cheap for JPEG-only downscaled thumbnails.
- **[Risk]** Manual-only resume could leave a stuck `ANALYZING` job if the user never returns → **Mitigation**: expose a "cancel/reset" action so the single global lock can be manually released.
- **[Risk]** Single-thread executor serializes all analysis but also blocks any other background work (e.g., thumbnail regeneration on demand) → **Mitigation**: keep thumbnail-on-demand regeneration on the request thread (synchronous, cheap), not routed through the same executor.
- **[Risk]** "Modern and fluid" UI expectations could tempt adding a JS framework/bundler that conflicts with the single-stage static-resources Dockerfile and adds client-side weight → **Mitigation**: enforce vanilla JS + CSS-only animations, lazy loading, and debouncing as explicit design decisions (see Decision 8); revisit only if measured performance proves insufficient.

## Migration Plan

Greenfield feature — no existing data or running system to migrate. Initial deployment simply ships the new jar with Hibernate `ddl-auto=update` (or `create` for first boot) creating the new tables. Rollback = redeploy previous image (no schema rollback needed since no prior schema exists).

Since this is also the project's first real deployment target, the change ships a `docker-compose.yml` example (app + PostgreSQL, NAS bind mount, named volume for DB data) and a step-by-step Arcane deployment guide in the README, so the first Raspberry Pi install is fully documented end-to-end (stack creation, environment variables, health checks, and how to redeploy after future pushes).

## Open Questions

- Exact maximum thumbnail dimension / JPEG quality for cache size vs. review-tab responsiveness — to refine during implementation/testing on real Pi hardware.
- Exact DCT hash size (e.g., 8x8 vs 16x16 grid) trade-off between accuracy and compute cost — to validate empirically against the 500-2000 photo volume target.
- Exact thumbnail size/quality and pagination page size that feel fluid on the client while keeping the Pi's CPU/network usage low — to tune during implementation with real hardware measurements.
