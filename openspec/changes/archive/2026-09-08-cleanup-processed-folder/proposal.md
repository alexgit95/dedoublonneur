## Why

Users need a deliberate way to remove an exported event completely after archiving it. The operation is irreversible: it must delete the source folder, every historical output folder linked to that event, temporary thumbnails, and database history, while still continuing independent deletions when one path fails and reporting a partial cleanup accurately.

## What Changes

- Add a `Nettoyer` action to processed-folder tiles, available only while the folder is marked as successfully exported.
- Require a simple confirmation before invoking the destructive cleanup operation.
- Validate the event history and all deletion targets server-side; reject active workflows and paths outside the configured source/output roots.
- Delete the source event folder itself, every historical output folder recorded for the event, and all thumbnails for its analysis jobs.
- Delete the event's database history (`PhotoAsset`, `ProcessingResult`, `AnalysisJob`, and `Event`) only when all filesystem and thumbnail deletions succeed.
- Use best-effort deletion: continue attempting independent targets after an error and return successful/deferred paths with error details.
- Preserve history after a partial cleanup so the user can retry; remove the event from the folder listing after a complete cleanup.
- Display the last export's saved space on the processed-folder tile until cleanup succeeds.

## Capabilities

### New Capabilities
- `processed-folder-cleanup`: safely and explicitly remove a processed event and all associated exports/history.

### Modified Capabilities
- `event-folder-workflow`: processed-folder tiles expose cleanup and last-export saved-space information.
- `processed-folder-indicator`: include saved bytes and cleanup availability while processed history exists.
- `responsive-ui`: keep the cleanup action, saved-space indicator, confirmation result, and partial-error message usable on desktop/mobile.

## Impact

- `EventFolderService`/new cleanup service and controller for path validation, best-effort deletion, and database cleanup.
- `AnalysisJobRepository`, processing-result queries, and thumbnail cleanup for all historical jobs/outputs.
- `EventFolderListing` API and `workflow.js`/CSS for saved-space and cleanup controls.
- Tests for path safety, full cleanup, partial cleanup, history retention, and confirmation/API behavior.
- No schema change is expected, but deletion ordering and transaction boundaries must prevent orphaned or falsely completed history.
