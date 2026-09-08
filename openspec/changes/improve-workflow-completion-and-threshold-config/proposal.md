## Why

The default duplicate-similarity threshold is currently fixed in configuration and the review slider is hardcoded, so deployments cannot choose a sensible starting value without code/configuration edits. Separately, after an export the workflow continues exposing the completed folder as the current context, which creates a race when users immediately start another analysis and can produce a transient HTTP 409 even though the previous job is already complete.

## What Changes

- Make the default duplicate-similarity threshold configurable through `APP_DEFAULT_SIMILARITY_THRESHOLD`, with a fallback of `90` and validation for values from 0 to 100.
- Store the resolved default on each analysis job and initialize the review slider from that job value while still allowing the user to change it.
- Separate the active workflow context from the last completed export: after a successful export, the active status becomes `IDLE` with no current job/folder, while the last completed job context remains available for the recap.
- Hide/clear the recap when the user selects a new folder.
- Ensure a `DONE` job never blocks a new analysis, including when the same event folder is selected again after export.
- Preserve the 409 response for genuine concurrent active workflows (`ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`).
- Mark failed asynchronous exports as `CANCELLED` and never expose them as completed exports.
- When a folder has multiple completed exports, expose the latest completion timestamp.

## Capabilities

### New Capabilities
- `workflow-completion-state`: distinguish active workflow state from the last completed export and expose a clean IDLE transition.
- `similarity-threshold-configuration`: configure and expose the per-job default similarity threshold while retaining user override.

### Modified Capabilities
- `event-folder-workflow`: completed jobs no longer hold the active workflow context or block subsequent analysis; genuine active-workflow locking remains.
- `duplicate-review`: initialize the adjustable similarity slider from the job's stored default threshold.
- `folder-processing`: failed asynchronous processing transitions to `CANCELLED` and successful completion releases the active workflow context.

## Impact

- `AppProperties`/`application.yml`: environment-backed default threshold.
- `AnalysisJobResponse`, `WorkflowStatus`, and frontend workflow/duplicate-review scripts: expose and consume per-job threshold and last-completed recap context.
- `WorkflowStateService`: separate active and completed state and allow reliable reprocessing after `DONE`.
- `FolderProcessingService`: explicit failure state for asynchronous export errors.
- Repository queries/tests for latest completed export and active status behavior.
- README, CHANGELOG, OpenAPI descriptions, and integration tests.
- No database schema change is expected; existing job and processing-result fields are sufficient.
