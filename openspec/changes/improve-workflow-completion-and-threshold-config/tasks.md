## 1. Similarity threshold configuration

- [x] 1.1 Bind `APP_DEFAULT_SIMILARITY_THRESHOLD` with fallback `90` and validate the resolved value is an integer from 0 to 100.
- [x] 1.2 Expose the stored per-job default threshold in job/workflow responses and initialize the duplicate-review slider from it while preserving user overrides.
- [x] 1.3 Add tests for absent, valid, invalid, and boundary threshold configuration values.

## 2. Workflow completion state

- [x] 2.1 Extend the workflow status contract with separate active context and last-completed export context.
- [x] 2.2 Make successful export transition the active workflow to `IDLE` with null active job/folder while retaining the latest recap context.
- [x] 2.3 Clear the retained recap when the user selects a new folder and keep the same-folder reanalysis path free of false 409 responses.
- [x] 2.4 Add tests for `DONE` not blocking new analysis, same-folder reprocessing, recap clearing, and genuine concurrent-start rejection.

## 3. Export failure handling

- [x] 3.1 Mark asynchronous processing failures as `CANCELLED`, preserve the source folder, and avoid presenting a successful recap.
- [x] 3.2 Add tests for copy failures and ensure no failed job is treated as a completed export.
- [x] 3.3 Report the latest completed export timestamp when multiple successful exports exist.

## 4. Frontend and documentation

- [x] 4.1 Update `workflow.js`, `duplicate-review.js`, and markup to consume separate active/last-completed status and per-job threshold fields.
- [x] 4.2 Ensure the recap remains visible after export and disappears when a new folder is selected; preserve the user-visible 409 message for real active conflicts only.
- [x] 4.3 Update OpenAPI descriptions, `README.md`, and `CHANGELOG.md` with the environment variable, state separation, reprocessing, and cancellation behavior.
- [x] 4.4 Run Node 22 checks and the full Maven test suite with the required Maven binary.
