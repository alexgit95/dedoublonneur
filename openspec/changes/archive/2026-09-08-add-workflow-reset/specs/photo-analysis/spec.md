## MODIFIED Requirements

### Requirement: Resumable analysis with batch checkpointing
The system SHALL persist analysis progress at a granularity of approximately every 20 processed photos, so that analysis can resume from the last completed batch after an application restart, without recomputing already-persisted photos. Resuming SHALL require an explicit user action; the system SHALL NOT automatically resume an interrupted analysis on startup. When a user resets an analysis, the runner SHALL stop cooperatively before processing another snapshot item or finalizing the job, and the system SHALL remove all persisted photo analysis records for that job before returning to `IDLE`.

#### Scenario: Application restarts mid-analysis
- **WHEN** the application restarts while an analysis job has processed some but not all photos of the snapshot
- **THEN** the job status reflects that it is paused/interrupted and does not automatically continue

#### Scenario: User manually resumes an interrupted analysis
- **WHEN** the user triggers resume on an interrupted analysis job
- **THEN** the system continues processing from the next photo after the last checkpointed batch, without recomputing already-analyzed photos

#### Scenario: User resets an analysis in progress
- **WHEN** the user confirms reset while the analysis runner is processing a job
- **THEN** the runner stops before processing another photo or marking the job ready for review, and all `PhotoAsset` records for the cancelled job are removed

### Requirement: Analysis progress reporting
The system SHALL expose the current analysis progress as the count of photos analyzed versus the total snapshot size, retrievable via polling.

#### Scenario: Client polls analysis progress
- **WHEN** a client requests the status of an in-progress analysis job
- **THEN** the system returns the current status and the analyzed/total photo counts
