## 1. Workflow context and API contract

- [x] 1.1 Inspect the existing workflow status and job response DTOs and identify the minimal active job and event-folder context required by the landing page to resume a workflow.
- [x] 1.2 Extend the workflow status contract, only if necessary, with nullable active job identifier and event-folder name while preserving the existing status values and API compatibility.
- [x] 1.3 Add or update backend tests covering status context for `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, and `DONE`.

## 2. Unified frontend shell

- [x] 2.1 Add `src/main/resources/static/index.html` as the root landing page with folder selection, analysis, review, and export panels.
- [x] 2.2 Add the shared workflow controller that loads folder/status data, stores the active job context, maps backend statuses to visible steps, and polls long-running states.
- [x] 2.3 Add the persistent breadcrumb/stepper with active, completed, disabled, and next-action states for desktop and mobile layouts.
- [x] 2.4 Add guarded workflow tabs so users cannot open analysis, review, or export before the corresponding prerequisite state is reached.
- [x] 2.5 Add resume behavior that revalidates server status on load and opens the appropriate step for an active or completed workflow.

## 3. Review and processing integration

- [x] 3.1 Adapt the existing blur review module to initialize inside the unified review panel using the shared job context while retaining pagination and deletion toggles.
- [x] 3.2 Adapt the existing duplicate review module to initialize inside the unified review panel using the shared job context while retaining threshold debouncing and deletion toggles.
- [x] 3.3 Add Flou and Doublons tab controls and ensure switching tabs does not lose the active job or reload unrelated workflow state.
- [x] 3.4 Adapt the processing panel to use the shared job context, show processing progress, display the recap, and expose the new-workflow action after `DONE`.
- [x] 3.5 Keep direct legacy URLs functional and add navigation back to the root workflow screen.

## 4. Styling and responsive behavior

- [x] 4.1 Extend the shared stylesheet for the workflow shell, stepper, tabs, status messaging, disabled states, and mobile layouts without adding a frontend dependency.
- [x] 4.2 Verify stable dimensions, readable labels, keyboard focus states, and non-overlapping content at desktop and narrow mobile viewport widths.

## 5. Documentation and validation

- [x] 5.1 Add JavaScript coverage for the backend-status-to-step mapping used by the workflow shell.
- [x] 5.2 Update `README.md` to document `/` as the application entry point and describe the guided workflow and resume behavior.
- [x] 5.3 Update `CHANGELOG.md` with the new landing page, unified tabs, breadcrumb, and workflow resume behavior.
- [x] 5.4 Run the focused JavaScript tests and the required Maven test command, then verify the application starts locally and serves `/` successfully.
