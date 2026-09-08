## 1. Processing preview backend

- [x] 1.1 Add preview DTOs and `GET /api/jobs/{id}/process-preview` with READY_FOR_REVIEW validation.
- [x] 1.2 Calculate kept/deleted photo counts, copied video count, source bytes, kept bytes, and estimated deleted-photo savings without creating output or modifying source files.
- [x] 1.3 Add service/controller tests for preview values, video inclusion, savings semantics, no side effects, and non-review rejection.

## 2. Processing progress backend

- [x] 2.1 Add an in-memory per-job progress registry with total items, processed items, percentage, bytes copied, and terminal status.
- [x] 2.2 Update asynchronous processing after each photo/video item and expose `GET /api/jobs/{id}/processing-progress` for polling.
- [x] 2.3 Ensure successful processing reaches 100 percent and failed processing exposes a cancelled/failed terminal state without a successful recap.
- [x] 2.4 Add tests for progress initialization, intermediate updates, completion, and failure.

## 3. Export UI

- [x] 3.1 Add a `Calculer le recapitulatif` control before export and render the no-side-effect preview metrics.
- [x] 3.2 Keep `Lancer le traitement` as a separate explicit action after preview and display the final recap only after successful completion.
- [x] 3.3 Replace the indeterminate export animation with a determinate progress bar and processed/total plus bytes-copied labels.
- [x] 3.4 Add responsive styling for preview metrics and progress on desktop/mobile and update the legacy processing page consistently.

## 4. Validation and documentation

- [x] 4.1 Add JavaScript tests and run Node 22 syntax/unit checks with `C:\dev\node-v22.23.1-win-x64\node.exe`.
- [x] 4.2 Run the full Maven test suite with the required Maven binary.
- [x] 4.3 Update OpenAPI descriptions, `README.md`, and `CHANGELOG.md` with preview, savings, and progress behavior.
