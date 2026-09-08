## MODIFIED Requirements

### Requirement: Processing recap
The system SHALL present a recap after processing completes, including the number of photos kept, the number of photos deleted, disk space used before processing, and disk space used after processing (output folder size). Successful processing SHALL release the active workflow context; failed asynchronous processing SHALL mark the job `CANCELLED` and SHALL not present a successful recap.

#### Scenario: Processing completes successfully
- **WHEN** the folder processing finishes successfully
- **THEN** the system displays the count of kept photos, the count of deleted photos, and the disk space before vs. after, while the active workflow becomes `IDLE`

#### Scenario: Processing fails
- **WHEN** an error prevents the folder processing from completing
- **THEN** the job is marked `CANCELLED`, no successful recap is presented for that attempt, and the workflow does not remain falsely marked as `DONE`
