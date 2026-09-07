## ADDED Requirements

### Requirement: Root landing page
The system SHALL serve a landing page at `/` that is the single user entry point for the photo-culling workflow.

#### Scenario: No workflow is active
- **WHEN** the user opens `/` and the workflow status is `IDLE`
- **THEN** the page displays the available event folders and an action to select one and start analysis

#### Scenario: A workflow already exists
- **WHEN** the user opens `/` and the workflow status is `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, or `DONE`
- **THEN** the page displays the current event folder, current status, and an action to resume that workflow

### Requirement: Unified workflow screen
The system SHALL provide one workflow screen with tabs for folder selection, analysis, review, and export, and SHALL keep the active job context while switching tabs.

#### Scenario: User moves between workflow tabs
- **WHEN** the user selects an accessible workflow tab
- **THEN** the corresponding content is displayed without requiring the user to edit a URL or provide a `jobId` manually

#### Scenario: User selects a tab that is not yet accessible
- **WHEN** the user selects a workflow tab whose prerequisite state has not been reached
- **THEN** the system keeps the current tab active and indicates that the prerequisite step must be completed first

### Requirement: Persistent workflow breadcrumb
The system SHALL display a persistent breadcrumb or stepper identifying the current event folder, the active workflow step, completed steps, and the next available action.

#### Scenario: Workflow step changes
- **WHEN** the workflow transitions between `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, and `DONE`
- **THEN** the breadcrumb updates the active step and marks the preceding step as completed where applicable

#### Scenario: User views a review tab
- **WHEN** the user is reviewing blurry photos or duplicate groups
- **THEN** the breadcrumb identifies review as the active step and the interface identifies the selected review tab

### Requirement: Guided review tabs
The system SHALL present Flou and Doublons as tabs within the review step while preserving their existing review behavior.

#### Scenario: User opens the review step
- **WHEN** the workflow status is `READY_FOR_REVIEW`
- **THEN** the review step displays Flou and Doublons tabs and opens one of them as the active tab

#### Scenario: User switches review type
- **WHEN** the user switches between Flou and Doublons
- **THEN** the selected review content changes in place and the active job context remains unchanged

### Requirement: Workflow resume
The system SHALL restore the workflow screen to the tab and context appropriate for the current persisted workflow status when the user returns to the application.

#### Scenario: Resume analysis
- **WHEN** the persisted status is `ANALYZING`
- **THEN** the workflow screen opens on the analysis step and displays pollable progress for the active job

#### Scenario: Resume review
- **WHEN** the persisted status is `READY_FOR_REVIEW`
- **THEN** the workflow screen opens on the review step with access to both review tabs

#### Scenario: Resume completed processing
- **WHEN** the persisted status is `DONE`
- **THEN** the workflow screen opens on the export step and displays the processing recap with an action to start a new workflow

### Requirement: Lightweight guided frontend
The system SHALL implement the landing page, workflow shell, breadcrumb, and tabs with the existing framework-free HTML, CSS, and JavaScript approach.

#### Scenario: User loads the workflow interface
- **WHEN** the user opens the application on a supported desktop or mobile browser
- **THEN** the interface loads without a client-side framework or bundler and preserves responsive layout behavior
