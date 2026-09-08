# processed-folder-cleanup Specification

## Purpose
TBD - created by archiving change cleanup-processed-folder. Update Purpose after archive.
## Requirements
### Requirement: Confirmed cleanup of a processed event
The system SHALL expose a cleanup action for an event with at least one successfully completed export, and SHALL require a user confirmation before the client invokes it.

#### Scenario: User confirms cleanup
- **WHEN** the user confirms cleanup for a processed event
- **THEN** the system attempts to delete the source folder, every historical output folder, and all associated thumbnail caches

#### Scenario: User cancels cleanup
- **WHEN** the user dismisses the confirmation
- **THEN** no filesystem, cache, or database data is changed

### Requirement: Safe cleanup target validation
The system SHALL resolve cleanup targets from persisted event/job history and configured NAS roots, SHALL reject active workflows, SHALL reject paths outside the configured roots or equal to a configured root, and SHALL not accept arbitrary client-supplied filesystem paths.

#### Scenario: Historical output path is unsafe
- **WHEN** a stored output path is outside the configured output root or resolves through traversal
- **THEN** the system refuses that target without deleting it and retains the cleanup history

### Requirement: Complete cleanup removes event and history
The system SHALL delete the source event directory itself, all historical output directories, all associated thumbnail directories, and the event's `PhotoAsset`, `ProcessingResult`, `AnalysisJob`, and `Event` history when every cleanup target is absent or deleted successfully.

#### Scenario: All cleanup targets are accessible
- **WHEN** every source, output, and thumbnail target can be deleted
- **THEN** the system removes the filesystem targets and database history, and the event no longer appears in folder listing

### Requirement: Best-effort partial cleanup
The system SHALL continue attempting independent cleanup targets after an error and SHALL return deleted targets and error details. When any target fails, the system SHALL retain database history so cleanup can be retried.

#### Scenario: One output cannot be deleted
- **WHEN** one historical output directory cannot be deleted but other targets can
- **THEN** the system deletes the accessible targets, reports the failed path and reason, retains history, and does not report cleanup as complete

