## ADDED Requirements

### Requirement: Side-effect-free processing preview
The system SHALL expose a processing preview for a job in `READY_FOR_REVIEW` that calculates kept-photo count, deleted-photo count, copied-video count, source bytes, kept bytes, and estimated saved bytes without creating an output folder, copying files, modifying source files, or changing workflow status.

#### Scenario: User calculates an export estimate
- **WHEN** the user requests a preview before starting processing
- **THEN** the system returns the current counts and byte estimates without starting the export

#### Scenario: Savings are calculated from deleted photos
- **WHEN** photos are marked for deletion and videos are present in the source folder
- **THEN** estimated saved bytes equal the total size of deleted photos, while all videos remain included in the copied-video count and do not contribute to savings

#### Scenario: Preview is requested for a non-review job
- **WHEN** a preview is requested while the job is not `READY_FOR_REVIEW`
- **THEN** the system rejects the request without changing the job or source folder
