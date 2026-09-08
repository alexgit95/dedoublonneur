## Why

The export screen currently shows only a generic "Traitement en cours" message while the asynchronous copy runs, so users cannot estimate remaining time or know how many files will be retained before committing to the export. The application already has all required photo decisions and file metadata to calculate a no-side-effect estimate before copying.

## What Changes

- Add a pre-export calculation action that reports kept photos, deleted photos, copied videos, source size, estimated kept size, and estimated bytes saved without creating an output folder or copying files.
- Define estimated savings as the size of photos marked for deletion; videos remain unconditionally copied and therefore do not contribute to savings.
- Add a processing-progress endpoint with processed item count, total item count, percentage, and bytes copied.
- Track export progress as the asynchronous job walks analyzed photos and source videos.
- Replace the indeterminate export feedback with a determinate progress bar and processed/total label.
- Keep the final recap separate from the pre-export estimate and do not modify source files during either operation.

## Capabilities

### New Capabilities
- `processing-preview`: calculate the export impact before starting any copy.
- `processing-progress`: expose and display determinate progress for an asynchronous export.

### Modified Capabilities
- `folder-processing`: add preflight estimation and progress reporting while preserving raw metadata copy and video inclusion rules.
- `responsive-ui`: provide responsive preview metrics and progress controls without overflow on desktop/mobile.

## Impact

- `FolderProcessingService` and new processing DTOs/controllers for preview and progress.
- In-memory or persisted per-job progress state for the active asynchronous export; no database schema change is expected unless restart persistence is required.
- `folder-processing.js`, `index.html`, and `traiter.html` for the preview action, metrics, progress bar, and final recap.
- Unit/integration tests for no-side-effect preview, correct savings, progress updates, and final completion.
- README and CHANGELOG documentation.
