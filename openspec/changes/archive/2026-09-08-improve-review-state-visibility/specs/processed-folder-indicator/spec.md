## ADDED Requirements

### Requirement: Completed-export state in folder listing
The system SHALL expose each available event folder with its name, a boolean `processed` state, and an optional completion timestamp. `processed` SHALL be true only when at least one export for that folder completed successfully with workflow status `DONE` and a persisted processing result.

#### Scenario: Folder has completed an export
- **WHEN** an event folder has a completed processing result linked to a `DONE` analysis job
- **THEN** the folder listing marks it as `processed: true` and includes its latest completion timestamp

#### Scenario: Folder is only analyzed or under review
- **WHEN** an event folder has a job in `ANALYZING` or `READY_FOR_REVIEW` without a completed export
- **THEN** the folder listing marks it as `processed: false`

#### Scenario: Folder analysis was cancelled
- **WHEN** an event folder has only cancelled jobs and no completed export
- **THEN** the folder listing marks it as `processed: false`

### Requirement: Processed folder remains selectable
The system SHALL display the completed-export state as informational and SHALL continue allowing the user to select and analyze a processed folder.

#### Scenario: User selects an already exported folder
- **WHEN** the user selects a folder marked `processed: true`
- **THEN** the folder remains selectable and the user can start a new analysis workflow
