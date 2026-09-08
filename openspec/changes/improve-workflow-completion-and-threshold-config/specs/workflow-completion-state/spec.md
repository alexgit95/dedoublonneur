## ADDED Requirements

### Requirement: Completed export releases active workflow context
The system SHALL report the workflow as `IDLE` with no active job or current folder after a successful export, while exposing the latest completed export context separately for recap display.

#### Scenario: Export completes successfully
- **WHEN** folder processing finishes successfully
- **THEN** workflow status is `IDLE`, active job/folder fields are null, and the latest completed job context remains available for the recap

#### Scenario: User selects a new folder
- **WHEN** the user selects a new folder after a completed export
- **THEN** the latest completed recap is cleared and the new analysis starts without an active-workflow conflict

### Requirement: Failed export is cancelled
The system SHALL mark an asynchronous export job as `CANCELLED` when processing fails and SHALL not expose it as a completed export.

#### Scenario: Export fails during asynchronous processing
- **WHEN** a file-copy or processing error prevents export completion
- **THEN** the job status becomes `CANCELLED`, no successful processing result is created for that attempt, and the workflow does not report `DONE`
