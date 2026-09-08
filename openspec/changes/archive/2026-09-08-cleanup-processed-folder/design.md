## Context

Processed folders are currently identified from completed processing history and displayed as selectable event tiles. The source path is derived from the configured NAS source root, while historical output paths are stored in `ProcessingResult.outputFolderPath`. Thumbnails live in the temporary cache under each analysis job id.

This feature is intentionally destructive. The backend must be the authority for eligibility, path safety, deletion order, partial failures, and history removal; the browser confirmation is only a user-experience guard.

## Goals / Non-Goals

**Goals:**
- Offer cleanup only for a successfully processed event with retained history.
- Delete the source event directory, every historical output directory for that event, and all associated thumbnail caches.
- Continue independent deletion attempts after individual errors and report successes/failures.
- Delete database history only when all filesystem/cache cleanup targets succeed.
- Keep history after partial cleanup so the user can retry.
- Display last-export saved bytes until cleanup succeeds, and remove the tile after source deletion.

**Non-Goals:**
- Recovering deleted files or providing an undo/trash workflow.
- Cleaning folders that were never successfully exported.
- Deleting arbitrary paths supplied by the client.
- Treating a partial cleanup as a successful cleanup.

## Decisions

- **Endpoint by source folder name, not arbitrary paths:** expose a cleanup endpoint such as `DELETE /api/events/{folderName}`. The server resolves the event from persisted history and configured roots; the client never supplies filesystem paths.
- **Eligibility requires completed history:** cleanup is allowed only if the event has at least one `DONE` job with a processing result and no active workflow exists. This also ensures all historical output paths can be enumerated.
- **Validate every target before deletion:** normalize absolute paths, require source paths under `APP_NAS_SOURCE_PATH` and output paths under `APP_NAS_OUTPUT_PATH`, reject roots themselves, traversal, symlink escapes where detectable, and source/output collisions.
- **Best-effort deletion with structured result:** attempt source directory, each distinct historical output directory, and every job thumbnail directory independently. Collect deleted paths and errors; continue after each failure.
- **Delete history only after complete physical cleanup:** if all targets are absent or deleted successfully, delete `PhotoAsset`, `ProcessingResult`, `AnalysisJob`, and `Event` records. If any target fails, retain all history and return a partial-cleanup response so retry remains possible.
- **Delete directories themselves:** recursive deletion removes directory contents and the source/output directory roots, matching the irreversible cleanup decision. Thumbnail directories are also removed recursively.
- **Simple confirmation in UI:** the processed-folder tile shows a `Nettoyer` action and uses a browser confirmation dialog. The backend remains idempotent-safe and revalidates every condition.
- **Saved-space display:** enrich processed-folder listing with `savedBytes = max(0, spaceBeforeBytes - spaceAfterBytes)` from the latest completed result and label it as saved during the last export. The value disappears with the tile after complete cleanup.

## Risks / Trade-offs

- [Risk] A NAS permission error produces partial cleanup → Mitigation: continue independent targets, retain history, return failed paths/reasons, and allow retry.
- [Risk] A malicious or corrupt historical output path points outside the output root → Mitigation: reject it before any deletion and abort only that target while preserving history.
- [Risk] Source deletion succeeds but history deletion fails → Mitigation: report partial cleanup; history remains and subsequent cleanup treats already-absent paths as successful.
- [Risk] Multiple historical exports can share an output path → Mitigation: deduplicate normalized targets before deletion.
- [Risk] Symlinks could escape a configured root → Mitigation: never follow directory links during recursive deletion, and validate real paths where the platform supports it.

## Migration Plan

No schema migration is required. Existing completed processing results immediately provide the output paths and saved-space values. Deploy backend and frontend together. Rollback cannot restore deleted files; rollback only restores the cleanup UI/API code for folders not yet cleaned.

## Open Questions

(none)
