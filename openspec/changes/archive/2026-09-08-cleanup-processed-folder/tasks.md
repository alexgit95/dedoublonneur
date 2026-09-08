## 1. Cleanup backend safety

- [x] 1.1 Add a cleanup result DTO containing completion state, deleted paths, failed paths, and error reasons.
- [x] 1.2 Add repository queries/services to load the event, all `DONE` jobs, all historical output paths, processing results, photo assets, and thumbnail job ids.
- [x] 1.3 Implement secure target validation under configured source/output roots, rejecting roots, traversal, collisions, symlink escapes where detectable, and active workflows.
- [x] 1.4 Implement best-effort recursive deletion of the source directory, all historical output directories, and all associated thumbnail directories; continue after individual errors.
- [x] 1.5 Delete database history only after all physical/cache targets succeed; retain history after partial cleanup for retry.
- [x] 1.6 Add `DELETE /api/events/{folderName}/cleanup` with OpenAPI documentation and simple client-compatible confirmation semantics.

## 2. Processed-folder listing and UI

- [x] 2.1 Enrich processed-folder listing with latest saved bytes and cleanup availability.
- [x] 2.2 Add a `Nettoyer` button only on processed-folder tiles, with a simple confirmation dialog.
- [x] 2.3 Display saved bytes as `Espace gagne lors du dernier export` and render partial-cleanup errors with failed paths.
- [x] 2.4 Refresh/remove the folder tile after complete cleanup while keeping it available for retry after partial cleanup.
- [x] 2.5 Add responsive styling for cleanup controls, saved-space text, and partial-error feedback.

## 3. Tests and documentation

- [x] 3.1 Add tests for successful full cleanup of source, all outputs, thumbnails, and database history.
- [x] 3.2 Add tests for path traversal/root rejection, active-workflow rejection, and partial failures that retain history.
- [x] 3.3 Add tests verifying latest-export saved bytes and cleanup button/listing behavior.
- [x] 3.4 Run Node 22 checks and the full Maven suite with the required Maven binary.
- [x] 3.5 Update `README.md` and `CHANGELOG.md` with the irreversible cleanup behavior and partial-cleanup semantics.
