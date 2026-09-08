## Context

The current workflow exposes `POST /api/workflow/cancel`, but cancellation only changes the active job status. The asynchronous `PhotoAnalysisRunner` keeps running, `PhotoAsset` rows and thumbnail files remain, and the runner can later save results or transition the job to `READY_FOR_REVIEW`. The browser's current "Nouveau dossier" action only resets local state and does not perform server-side cleanup.

The reset crosses the workflow service, asynchronous analysis runner, JPA repositories, thumbnail/cache services, REST controller, and workflow shell UI. The source event folder is read-only from the application's perspective and must never be deleted or changed.

## Goals / Non-Goals

**Goals:**
- Offer a confirmed reset action only during `ANALYZING` and `READY_FOR_REVIEW`.
- Stop an in-progress analysis safely before deleting its persisted analysis data.
- Delete all `PhotoAsset` rows for the cancelled job, purge its thumbnail directory, and evict duplicate-clustering cache entries.
- Preserve the `AnalysisJob` as `CANCELLED` history while releasing the global workflow lock.
- Return the server and browser to folder selection with no stale polling or job URL.

**Non-Goals:**
- Cancelling or interrupting `PROCESSING` export; reset is unavailable during export.
- Deleting or modifying source event folders, photos, videos, or other NAS content.
- Deleting the `Event` or the cancelled `AnalysisJob` history.
- Adding a new persistence table or external cancellation dependency.

## Decisions

- **Use cooperative cancellation owned by `PhotoAnalysisRunner`**: the runner maintains a cancellation request for each running job, checks it before each snapshot item and before finalizing status, and exits without setting `READY_FOR_REVIEW` when cancellation is requested. A forceful thread interrupt is avoided because image decoding and JPA calls may not leave consistent state when interrupted.
- **Coordinate reset completion with runner termination**: `WorkflowStateService` requests cancellation and waits for the runner's completion signal before deleting `PhotoAsset` rows and purging temporary files. The wait must be bounded and return an error rather than deleting concurrently if the runner cannot stop safely.
- **Keep `AnalysisJob` and mark it `CANCELLED`**: this preserves operational history and avoids orphaned references while `PhotoAsset` rows are explicitly removed. The existing `CANCELLED` status remains outside the active-workflow query.
- **Centralize cleanup in a reset operation**: reset calls repository deletion by job id, `ThumbnailService.purge(jobId)`, and `DuplicateClusterService.evictJob(jobId)` in one service-level flow. This avoids relying on database cascade behavior or leaving in-memory cache entries behind.
- **Expose one server-side reset endpoint**: the UI calls `POST /api/workflow/cancel` (or its renamed reset equivalent) and only clears local state after a successful response. The endpoint remains idempotent-friendly at the UI boundary, while the service can retain the existing no-active-workflow error contract.
- **Place the action in active analysis/review panels only**: the control is rendered when status is `ANALYZING` or `READY_FOR_REVIEW`; it is hidden for `IDLE`, `PROCESSING`, and `DONE`. Confirmation text makes clear that partial analysis and generated thumbnails will be discarded.

## Risks / Trade-offs

- [Risk] A runner may be inside one image decode or database save when reset is requested → Mitigation: cooperative cancellation plus a completion signal; cleanup starts only after the runner leaves its critical operation.
- [Risk] A bounded wait could leave the workflow locked if the runner is genuinely stuck → Mitigation: return a clear failure response and retain the job active rather than deleting data concurrently; expose diagnostics in logs.
- [Risk] Resetting a completed review discards user keep/delete choices → Mitigation: require explicit browser confirmation describing that all analysis and review decisions for this job will be lost.
- [Risk] Purge failure can leave temporary thumbnails → Mitigation: make purge best-effort with warning logs, while database cleanup and workflow unlock remain deterministic; a later cache cleanup can remove leftovers.

## Migration Plan

No schema migration is required. Deploy the backend and UI together, then verify reset during an active analysis and after analysis reaches review. Existing `CANCELLED` jobs remain valid; their historical rows are not automatically altered. Rollback consists of reverting the application version; source folders are unaffected.

## Open Questions

(none)
