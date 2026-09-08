# event-folder-workflow Specification

## Purpose
TBD - created by archiving change add-photo-culling-workflow. Update Purpose after archive.
## Requirements
### Requirement: Event folder listing and selection
The system SHALL list the sub-folders (events) available under the configured NAS source mount, SHALL allow the user to select exactly one event folder to start an analysis, and SHALL expose this flow from the root landing page.

#### Scenario: User lists available event folders
- **WHEN** the user opens the root landing page while no workflow is active
- **THEN** the system displays the list of sub-folder names found directly under the NAS source mount

#### Scenario: User selects a folder to analyze
- **WHEN** the user selects an event folder and confirms from the root landing page
- **THEN** the system creates a new analysis workflow scoped to that folder, starts the background analysis, and opens the analysis step in the unified workflow screen

#### Scenario: User attempts to select another folder during an active workflow
- **WHEN** the user opens the root landing page while a workflow is active
- **THEN** the system displays the current workflow and directs the user to resume it instead of presenting a second selectable workflow

### Requirement: Single active workflow lock
The system SHALL allow at most one active workflow (analysis, review, or processing) at any time across the whole application. A workflow is considered active from the moment analysis starts until the folder has been fully processed (or the workflow is explicitly cancelled/reset). Jobs in `DONE` or `CANCELLED` SHALL never block a new analysis.

#### Scenario: Attempt to start a second analysis while one is active
- **WHEN** a user requests to start analysis on a new event folder while another workflow is in status `ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`
- **THEN** the system rejects the request and indicates which folder is currently being worked on

#### Scenario: Starting a new analysis after the previous workflow is done
- **WHEN** the previous workflow has reached status `DONE` (fully processed) or has been cancelled/reset
- **THEN** the system allows a new event folder to be selected and analyzed

#### Scenario: Two clients start concurrently after completion
- **WHEN** two clients request analysis after the previous job is `DONE`
- **THEN** exactly one new analysis starts and the other receives HTTP 409 because the new workflow is genuinely active

### Requirement: Workflow state machine
The system SHALL track and expose the current workflow status as one of: `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, `DONE`, and the user interface SHALL map each status to the corresponding workflow step. The system SHALL expose a confirmed reset action only while the status is `ANALYZING` or `READY_FOR_REVIEW`; the reset SHALL return the workflow to `IDLE` after server-side cleanup and SHALL not be available during `PROCESSING`.

#### Scenario: Status transitions through a normal workflow
- **WHEN** analysis starts, completes, the user triggers processing, and processing completes
- **THEN** the workflow status transitions in order `IDLE` → `ANALYZING` → `READY_FOR_REVIEW` → `PROCESSING` → `DONE`, while the interface advances through folder, analysis, review, and export steps

#### Scenario: User returns during an active status
- **WHEN** the user opens the root landing page with status `ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`
- **THEN** the system opens or offers a resume action for the matching workflow step using the active job context

#### Scenario: User returns after completion
- **WHEN** the user opens the root landing page with status `DONE`
- **THEN** the system displays the export recap and offers an action that returns the workflow to folder selection for a new event

#### Scenario: User resets during analysis
- **WHEN** the user confirms reset while the workflow status is `ANALYZING`
- **THEN** the system stops the active analysis safely, cleans up the job's analysis data and temporary thumbnails, changes the job status to `CANCELLED`, returns the workflow status to `IDLE`, and allows a new folder selection

#### Scenario: User resets during review
- **WHEN** the user confirms reset while the workflow status is `READY_FOR_REVIEW`
- **THEN** the system removes the job's analysis data and temporary thumbnails, changes the job status to `CANCELLED`, returns the workflow status to `IDLE`, and allows a new folder selection

#### Scenario: Reset is unavailable during export
- **WHEN** the workflow status is `PROCESSING`
- **THEN** the interface does not offer the reset action and the export continues under its existing processing rules

### Requirement: Manual cancel/reset of a stuck workflow
The system SHALL allow the user to manually reset the currently active workflow only from `ANALYZING` or `READY_FOR_REVIEW`, releasing the single global lock only after the analysis runner has stopped (when applicable) and the job's persisted analysis data and temporary caches have been cleaned.

#### Scenario: User cancels an active workflow
- **WHEN** the user triggers cancel/reset on the currently active workflow
- **THEN** the workflow status returns to `IDLE`, the cancelled job remains as `CANCELLED` history, its `PhotoAsset` records are removed, its thumbnails and duplicate cache are purged, and a new event folder can be selected

### Requirement: Informational processed-folder indicator
The system SHALL display an informational `Deja exporte` indicator for event folders whose export has completed with workflow status `DONE`, and SHALL not display that indicator for folders only analyzed, under review, or cancelled.

#### Scenario: User opens folder selection with exported folders
- **WHEN** the folder selection screen loads and an available folder has a completed export
- **THEN** that folder displays a clear `Deja exporte` indicator alongside its name

#### Scenario: User opens folder selection with an unexported folder
- **WHEN** an available folder has no completed export
- **THEN** the folder is displayed without the completed-export indicator

#### Scenario: User chooses an exported folder again
- **WHEN** the user selects an available folder marked `Deja exporte`
- **THEN** the selection remains valid and the user can start a new analysis

### Requirement: Cleanup and saved-space information on processed folder tiles
The system SHALL expose the latest successful export's saved bytes for a processed folder and SHALL expose cleanup availability while its processed history remains.

#### Scenario: Processed folder has a latest export
- **WHEN** an available event has a completed export
- **THEN** the folder listing includes the saved bytes from the latest export and indicates that cleanup is available

#### Scenario: Processed folder is completely cleaned
- **WHEN** cleanup deletes the source, all outputs, thumbnails, and database history successfully
- **THEN** the folder no longer appears in the available-folder listing

