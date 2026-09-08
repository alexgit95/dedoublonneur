## Why

The application can currently leave an active workflow locked on one event folder when analysis is interrupted or becomes stuck. The existing cancel path only changes the job status and the current UI reset is local to the browser, so analyzed `PhotoAsset` rows, thumbnails, and the asynchronous runner can survive the apparent reset and prevent a clean return to folder selection.

## What Changes

- Add a visible `Réinitialiser` action while the workflow is in analysis or manual review; do not expose it during folder selection, export processing, or the completed recap.
- Require confirmation before resetting an active workflow.
- Stop an analysis runner cooperatively before cleaning its data, so it cannot save new analysis results or transition the cancelled job back to review.
- Remove all analyzed photo records for the cancelled job, purge its thumbnail directory, and evict its duplicate-clustering cache.
- Preserve the cancelled `AnalysisJob` as historical data with status `CANCELLED`; never modify or delete the source event folder or its files.
- Return the server and UI to `IDLE`, clear the active job context and polling state, and reload the event-folder selection.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `event-folder-workflow`: reset becomes a complete, confirmed workflow operation available during analysis and review, releasing the global lock and returning to folder selection.
- `photo-analysis`: cancelling an in-progress analysis must stop further processing and remove its persisted photo analysis and temporary thumbnails without touching source photos.

## Impact

- `WorkflowStateService` and `WorkflowController`: reset orchestration and API behavior.
- `PhotoAnalysisRunner`: cooperative cancellation and runner lifecycle coordination.
- `PhotoAssetRepository` and `AnalysisJobRepository`: cleanup queries and job state handling.
- `ThumbnailService` and `DuplicateClusterService`: reset-time cleanup.
- `index.html` and `workflow.js`: confirmation, reset control, polling/state reset, and return to folder selection.
- Unit/integration tests for cancellation races, data cleanup, API behavior, and UI state.
- No source-folder deletion and no database schema change are expected.
