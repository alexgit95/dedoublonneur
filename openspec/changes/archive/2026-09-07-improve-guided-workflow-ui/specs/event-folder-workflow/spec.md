## MODIFIED Requirements

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

### Requirement: Workflow state machine
The system SHALL track and expose the current workflow status as one of: `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, `DONE`, and the user interface SHALL map each status to the corresponding workflow step.

#### Scenario: Status transitions through a normal workflow
- **WHEN** analysis starts, completes, the user triggers processing, and processing completes
- **THEN** the workflow status transitions in order `IDLE` → `ANALYZING` → `READY_FOR_REVIEW` → `PROCESSING` → `DONE`, while the interface advances through folder, analysis, review, and export steps

#### Scenario: User returns during an active status
- **WHEN** the user opens the root landing page with status `ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`
- **THEN** the system opens or offers a resume action for the matching workflow step using the active job context

#### Scenario: User returns after completion
- **WHEN** the user opens the root landing page with status `DONE`
- **THEN** the system displays the export recap and offers an action that returns the workflow to folder selection for a new event
