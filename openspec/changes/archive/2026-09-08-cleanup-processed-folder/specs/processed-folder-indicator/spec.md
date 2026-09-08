## MODIFIED Requirements

### Requirement: Completed-export state in folder listing
The system SHALL expose each available event folder with its name, a boolean `processed` state, an optional completion timestamp, and the latest export's saved bytes. `processed` SHALL be true only when at least one export for that folder completed successfully with workflow status `DONE` and a persisted processing result. The saved-space value SHALL remain visible until complete cleanup removes the history.

#### Scenario: Folder has completed an export
- **WHEN** an event folder has a completed processing result linked to a `DONE` analysis job
- **THEN** the folder listing marks it as `processed: true`, includes its latest completion timestamp, and includes the bytes saved during that export

#### Scenario: Folder is only analyzed or under review
- **WHEN** an event folder has a job in `ANALYZING` or `READY_FOR_REVIEW` without a completed export
- **THEN** the folder listing marks it as `processed: false` and exposes no saved-space value

#### Scenario: Folder analysis was cancelled
- **WHEN** an event folder has only cancelled jobs and no completed export
- **THEN** the folder listing marks it as `processed: false` and exposes no saved-space value

#### Scenario: Cleanup succeeds
- **WHEN** cleanup removes the event and all associated history
- **THEN** the folder, processed indicator, and saved-space value are absent from the listing
