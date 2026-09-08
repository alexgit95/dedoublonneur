## MODIFIED Requirements

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
- **WHEN** the user confirms cancel/reset on a workflow in `ANALYZING` or `READY_FOR_REVIEW`
- **THEN** the workflow status returns to `IDLE`, the cancelled job remains as `CANCELLED` history, its `PhotoAsset` records are removed, its thumbnails and duplicate cache are purged, and a new event folder can be selected
