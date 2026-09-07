# event-folder-workflow Specification

## Purpose
TBD - created by archiving change add-photo-culling-workflow. Update Purpose after archive.

## Requirements

### Requirement: Event folder listing and selection
The system SHALL list the sub-folders (events) available under the configured NAS source mount, and SHALL allow the user to select exactly one event folder to start an analysis.

#### Scenario: User lists available event folders
- **WHEN** the user opens the folder-selection page
- **THEN** the system displays the list of sub-folder names found directly under the NAS source mount

#### Scenario: User selects a folder to analyze
- **WHEN** the user selects an event folder and confirms
- **THEN** the system creates a new analysis workflow scoped to that folder and starts the background analysis

### Requirement: Single active workflow lock
The system SHALL allow at most one active workflow (analysis, review, or processing) at any time across the whole application. A workflow is considered active from the moment analysis starts until the folder has been fully processed (or the workflow is explicitly cancelled/reset).

#### Scenario: Attempt to start a second analysis while one is active
- **WHEN** a user requests to start analysis on a new event folder while another workflow is in status `ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`
- **THEN** the system rejects the request and indicates which folder is currently being worked on

#### Scenario: Starting a new analysis after the previous workflow is done
- **WHEN** the previous workflow has reached status `DONE` (fully processed) or has been cancelled/reset
- **THEN** the system allows a new event folder to be selected and analyzed

### Requirement: Workflow state machine
The system SHALL track and expose the current workflow status as one of: `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, `DONE`.

#### Scenario: Status transitions through a normal workflow
- **WHEN** analysis starts, completes, the user triggers processing, and processing completes
- **THEN** the workflow status transitions in order `IDLE` → `ANALYZING` → `READY_FOR_REVIEW` → `PROCESSING` → `DONE`

### Requirement: Manual cancel/reset of a stuck workflow
The system SHALL allow the user to manually cancel or reset the currently active workflow, releasing the single global lock.

#### Scenario: User cancels an active workflow
- **WHEN** the user triggers cancel/reset on the currently active workflow
- **THEN** the workflow status returns to `IDLE` and a new event folder can be selected
