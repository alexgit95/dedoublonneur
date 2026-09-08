## 1. Review-card visual hierarchy

- [x] 1.1 Rename/add the shared card state class so `markedForDeletion` is the visually de-emphasized state and kept cards remain primary.
- [x] 1.2 Update neon CSS for kept/deletion cards with readable contrast, explicit state text/icon treatment, and no loss of thumbnail preview or checkbox usability.
- [x] 1.3 Add responsive checks/styles so card state cues remain clear on desktop and mobile.

## 2. Processed-folder backend contract

- [x] 2.1 Add a DTO for event-folder listing entries with `name`, `processed`, and nullable `processedAt` fields.
- [x] 2.2 Add a repository projection/query that returns completed export state from `DONE` jobs with persisted processing results, grouped by event and using the latest completion timestamp.
- [x] 2.3 Update `EventController`/`EventFolderService` to merge available filesystem folders with the completed-export projection in one backend flow, while preserving folder selection and reprocessing.
- [x] 2.4 Update OpenAPI documentation and tests for `DONE`, `READY_FOR_REVIEW`, `ANALYZING`, and `CANCELLED` folder histories.

## 3. Folder-selection UI

- [x] 3.1 Update `workflow.js` to consume enriched folder entries while continuing to send the folder name in the analysis URL.
- [x] 3.2 Add an accessible `Deja exporte` badge/label to processed folders without disabling their radio control.
- [x] 3.3 Add responsive styling and frontend tests for processed/unprocessed folder rendering.

## 4. Validation and documentation

- [x] 4.1 Run JavaScript checks with `C:\dev\node-v22.23.1-win-x64\node.exe` and the full Maven test suite with the required Maven binary.
- [x] 4.2 Update `README.md` and `CHANGELOG.md` with the kept-photo visual priority and completed-export folder indicator.
