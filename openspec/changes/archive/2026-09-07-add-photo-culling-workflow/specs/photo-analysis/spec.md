## ADDED Requirements

### Requirement: Snapshot of files at analysis start
The system SHALL snapshot the list of JPEG photo files present in the selected event folder at the moment analysis starts, and SHALL only analyze files present in that snapshot, ignoring any files added to the source folder afterwards.

#### Scenario: Photos added after analysis has started
- **WHEN** new photo files are added to the source folder while an analysis is in progress
- **THEN** the system does not include those new files in the current analysis job

### Requirement: Per-photo blur score computation
The system SHALL compute a sharpness/blur score for each JPEG photo in the snapshot, using a variance-of-Laplacian style algorithm on a downscaled grayscale version of the image, without modifying the original file.

#### Scenario: Blur score computed for a photo
- **WHEN** the analysis job processes a photo
- **THEN** the system persists a numeric blur score for that photo, and the original file on the source folder remains byte-for-byte unchanged

### Requirement: Per-photo perceptual hash computation
The system SHALL compute a perceptual hash (pHash) for each JPEG photo in the snapshot and persist it alongside the photo record, without persisting any duplicate grouping.

#### Scenario: pHash computed for a photo
- **WHEN** the analysis job processes a photo
- **THEN** the system persists the photo's perceptual hash value for later on-demand similarity clustering

### Requirement: Resumable analysis with batch checkpointing
The system SHALL persist analysis progress at a granularity of approximately every 20 processed photos, so that analysis can resume from the last completed batch after an application restart, without recomputing already-persisted photos. Resuming SHALL require an explicit user action; the system SHALL NOT automatically resume an interrupted analysis on startup.

#### Scenario: Application restarts mid-analysis
- **WHEN** the application restarts while an analysis job has processed some but not all photos of the snapshot
- **THEN** the job status reflects that it is paused/interrupted and does not automatically continue

#### Scenario: User manually resumes an interrupted analysis
- **WHEN** the user triggers resume on an interrupted analysis job
- **THEN** the system continues processing from the next photo after the last checkpointed batch, without recomputing already-analyzed photos

### Requirement: Analysis progress reporting
The system SHALL expose the current analysis progress as the count of photos analyzed versus the total snapshot size, retrievable via polling.

#### Scenario: Client polls analysis progress
- **WHEN** a client requests the status of an in-progress analysis job
- **THEN** the system returns the current status and the analyzed/total photo counts
