## MODIFIED Requirements

### Requirement: Processing recap
The system SHALL present a recap after processing completes, including the number of photos kept, the number of photos deleted, disk space used before processing, and disk space used after processing (output folder size). The system SHALL also provide a side-effect-free preview before processing and determinate progress while processing asynchronously.

#### Scenario: Processing completes successfully
- **WHEN** processing of the folder finishes successfully
- **THEN** the system displays the count of kept photos, the count of deleted photos, and the disk space before vs. after, after progress reaches 100 percent

#### Scenario: User previews before processing
- **WHEN** the user requests an estimate before launching processing
- **THEN** the system displays kept/deleted counts and estimated saved bytes without copying files or changing the source folder
