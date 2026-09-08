## 1. Cancellation and cleanup backend

- [x] 1.1 Add repository cleanup methods to delete all `PhotoAsset` rows belonging to a cancelled job without deleting the source `Event` or `AnalysisJob` history.
- [x] 1.2 Add cooperative cancellation state and completion coordination to `PhotoAnalysisRunner`; check cancellation before each photo and before finalizing `READY_FOR_REVIEW`.
- [x] 1.3 Extend `WorkflowStateService` reset orchestration to request runner cancellation, wait for safe termination with a bounded timeout, purge thumbnails, evict duplicate-clustering cache, delete photo analysis rows, mark the job `CANCELLED`, and return `IDLE`.
- [x] 1.4 Ensure reset cannot race with analysis persistence and that a cancelled runner cannot transition the job back to `READY_FOR_REVIEW`.
- [x] 1.5 Keep reset unavailable for `PROCESSING`, while preserving the existing single-active-workflow lock and `DONE` behavior.

## 2. API and user interface

- [x] 2.1 Update the workflow reset endpoint's OpenAPI documentation and response/error behavior for resets during `ANALYZING` and `READY_FOR_REVIEW`.
- [x] 2.2 Add a confirmed `Réinitialiser` control to the analysis and review panels only; do not render it for folder selection, export, or completed recap.
- [x] 2.3 Make the browser call the server reset endpoint before clearing local job state, then stop polling, clear the URL job id, reset review/processing controllers, and reload folder selection.
- [x] 2.4 Show a clear failure message when the server cannot stop and clean the active analysis safely.

## 3. Tests and documentation

- [x] 3.1 Add service tests covering reset from analysis and review, deletion of `PhotoAsset` rows, thumbnail purge, cache eviction, retained `CANCELLED` job, and return to `IDLE`.
- [x] 3.2 Add runner tests covering cancellation during a snapshot and the race that must not save results or mark the job ready after reset.
- [x] 3.3 Cover reset availability and `PROCESSING` rejection through the workflow service contract and JavaScript syntax validation (the project does not include Spring's MockMvc test-autoconfigure module).
- [x] 3.4 Run the full Maven test suite with the required Maven binary and the JavaScript tests.
- [x] 3.5 Update `README.md` and `CHANGELOG.md` with the reset behavior and its scope limitation to analysis/review.
