## Context

The similarity default is configured as a literal YAML value and the duplicate-review slider is hardcoded to the same number. `AnalysisJob` already persists a per-job default threshold, but the frontend does not receive it explicitly.

The workflow currently treats the latest `DONE` job as the current status context. This is useful for the recap but causes active-workflow semantics and last-result semantics to overlap. A new analysis can race with the final visibility/transition of the completed job and briefly receive a lock response.

## Goals / Non-Goals

**Goals:**
- Configure the global starting threshold through `APP_DEFAULT_SIMILARITY_THRESHOLD`, defaulting to 90 and rejecting or normalizing invalid values deterministically.
- Expose the resolved per-job threshold so review initializes its slider from stored job state while preserving user adjustment.
- Return `IDLE` with no active job/folder after successful export while exposing last-completed recap context separately.
- Make a `DONE` job never participate in active-workflow locking, including when the same folder is selected again.
- Transition asynchronous export failures to `CANCELLED` and prevent them from being treated as completed exports.
- Preserve the last completed export timestamp/result context until a new folder is selected.

**Non-Goals:**
- Changing the similarity algorithm, threshold distance mapping, or duplicate grouping behavior.
- Blocking reprocessing of folders that were already exported.
- Introducing a new database table or migration.
- Cancelling an export once it has started.

## Decisions

- **Environment property with fallback:** use `${APP_DEFAULT_SIMILARITY_THRESHOLD:90}`. Validate at application binding time that the value is between 0 and 100; an invalid configured value fails fast rather than silently running with an unexpected threshold.
- **Per-job threshold is authoritative:** `AnalysisJob.similarityThresholdDefault` stores the resolved value at job creation. Return it in the job/status response and initialize both review sliders from it. User changes remain request-level overrides and do not mutate the job default.
- **Separate active and completed status fields:** extend `WorkflowStatus` with nullable `lastCompletedJobId`, `lastCompletedFolderName`, and `lastCompletedAt` (or an equivalent result context). `status` becomes `IDLE` after successful processing, with active `jobId` and `eventFolderName` null. The frontend keeps the recap visible using the last-completed job id until a new folder is selected, then clears it.
- **Keep active lock query unchanged:** `ACTIVE_STATUSES` remains `ANALYZING`, `READY_FOR_REVIEW`, and `PROCESSING`; `DONE` and `CANCELLED` never block `startAnalysis`. Add concurrency tests for same-folder reprocessing, rapid duplicate starts, and two concurrent clients.
- **Mark async processing failures as `CANCELLED`:** catch processing exceptions, reload the job, set `CANCELLED`, set `finishedAt`, and save. Do not persist a successful `ProcessingResult` on failure. The existing output folder may require cleanup as a separate best-effort concern, but the job must not be reported as completed.
- **Latest completed export wins:** when deriving recap or processed-folder timestamps, use the latest `ProcessingResult.processedAt` among completed jobs for the event.

## Risks / Trade-offs

- [Risk] A configuration typo prevents startup → Mitigation: fail fast with a clear property validation error and document the accepted range.
- [Risk] Frontend and backend status contracts are deployed out of sync → Mitigation: add nullable fields without removing existing fields and deploy static/backend changes together.
- [Risk] A last-completed recap could be mistaken for an active workflow → Mitigation: use distinct fields and clear active context (`jobId`/folder null) when status is `IDLE`.
- [Risk] A failed export leaves a partial output directory → Mitigation: mark the job `CANCELLED`, log the error, and add best-effort cleanup or make the output path visible for operator recovery; never mark it `DONE`.

## Migration Plan

No database migration is required. Existing jobs retain their persisted threshold values. Existing `DONE` jobs become last-completed context while the active workflow reports `IDLE`. Set `APP_DEFAULT_SIMILARITY_THRESHOLD` only when a deployment wants a starting value other than 90. Rollback is application/static asset rollback; nullable status fields preserve compatibility.

## Open Questions

(none)
