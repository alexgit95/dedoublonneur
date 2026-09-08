## ADDED Requirements

### Requirement: Cleanup and saved-space information on processed folder tiles
The system SHALL expose the latest successful export's saved bytes for a processed folder and SHALL expose cleanup availability while its processed history remains.

#### Scenario: Processed folder has a latest export
- **WHEN** an available event has a completed export
- **THEN** the folder listing includes the saved bytes from the latest export and indicates that cleanup is available

#### Scenario: Processed folder is completely cleaned
- **WHEN** cleanup deletes the source, all outputs, thumbnails, and database history successfully
- **THEN** the folder no longer appears in the available-folder listing
