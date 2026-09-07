## Why

Sorting photo events manually before archiving them (removing blurry shots and duplicate bursts while keeping the best photo of each group) is slow and error-prone. This change introduces a self-hosted workflow app that analyzes an event folder on a NAS, flags blurry photos and duplicate/near-duplicate groups, lets the user review and adjust the selection, then produces a clean output folder with metadata (EXIF, GPS, dates) fully preserved.

## What Changes

- New web UI (vanilla HTML/JS/CSS served from the Spring Boot jar) to pick an event sub-folder from a NAS bind mount and launch a background analysis. The UI SHALL feel modern and fluid (smooth transitions, responsive layout, no full-page reloads) while staying framework-free and lightweight so it renders comfortably on the Raspberry Pi 4's shared resources.
- New background analysis job that computes, per JPEG photo: a sharpness/blur score (variance of Laplacian) and a perceptual hash (pHash), checkpointing progress every ~20 photos so analysis can resume after a restart without recomputing already-analyzed photos.
- Only one analysis/processing workflow may be active at a time, across the whole app (single global lock held from analysis start until the folder is processed).
- New "Flou" review tab listing blurry photos, pre-checked for deletion, with the ability to uncheck any photo to keep it.
- New "Doublons" review tab that clusters photos by similarity **on demand** from stored pHash values, using a user-adjustable similarity threshold (0-100%) that can be changed even after analysis completes, instantly recomputing groups without re-analyzing images. Within each group, the sharpest photo is unchecked (kept) by default; ties broken by filename/date order.
- New "process folder" action: user supplies a new, non-existing output folder name; the app copies every kept photo (raw byte copy, metadata untouched) and every video file found in the source folder to the output folder, then shows a recap (kept/deleted counts, disk space before/after).
- Deleted/unchecked photos are simply never copied to the output folder; the source folder is never modified or deleted from.
- Thumbnails for review tabs are generated during analysis and cached on local (ephemeral) container disk, purged automatically once the folder is processed.
- No authentication (trusted LAN access only).

## Capabilities

### New Capabilities
- `event-folder-workflow`: Selecting an event folder from the NAS mount, enforcing the single-active-workflow lock, and the overall state machine (idle → analyzing → ready-for-review → processing → done).
- `photo-analysis`: Background job that scans a snapshotted list of JPEG files, computes blur score and pHash per photo, persists per-batch checkpoints for resumable (manual-restart) progress, and exposes pollable progress (count analyzed / total).
- `blur-review`: The "Flou" tab — listing blurry photos with default-checked deletion state and per-photo toggle.
- `duplicate-review`: The "Doublons" tab — on-demand similarity clustering from stored pHash values with an adjustable threshold, best-photo-kept default selection, and per-photo toggle.
- `folder-processing`: The "Traiter le dossier" action — output folder validation, raw-byte copy of kept photos and all videos preserving metadata, and the final kept/deleted/space recap.
- `responsive-ui`: Cross-cutting frontend performance and UX requirements (lazy loading, debounced interactions, lightweight assets, smooth feedback) applied across the folder selection, review tabs, and processing screens.

### Modified Capabilities
(none — greenfield project, no existing specs)

## Impact

- New Spring Boot backend module(s): REST controllers, JPA entities (`Event`, `AnalysisJob`, `PhotoAsset`), services for blur/pHash computation, clustering, and file copy.
- New Maven dependencies: TwelveMonkeys ImageIO (robust JPEG decoding), metadata-extractor (EXIF reading, read-only).
- New static frontend pages/scripts under `src/main/resources/static`, built with modern CSS (Grid/Flexbox, transitions) and vanilla JS only — no SPA framework/bundler, keeping payload size and client-side CPU usage minimal for low-power client devices and Pi-served assets.
- New database tables (Hibernate `ddl-auto`, SQLite locally / PostgreSQL in prod-compatible schema).
- Docker/compose: requires a NAS bind mount for source+output folders (path fixed via compose, not user-configurable in UI).
