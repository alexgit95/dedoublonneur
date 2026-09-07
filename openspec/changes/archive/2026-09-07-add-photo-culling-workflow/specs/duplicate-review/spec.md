## ADDED Requirements

### Requirement: On-demand duplicate clustering by similarity threshold
The system SHALL group photos of the analyzed event into duplicate clusters computed on demand from their stored perceptual hashes and a similarity threshold expressed as a percentage (0-100%), without persisting the resulting groups.

#### Scenario: User opens the duplicate review tab
- **WHEN** the user opens the "Doublons" tab after analysis has completed
- **THEN** the system computes and displays duplicate groups using the current similarity threshold

### Requirement: Adjustable similarity threshold with instant recomputation
The system SHALL allow the user to change the similarity threshold, including after the initial analysis has completed, and SHALL recompute duplicate groups from stored pHash values without re-analyzing any photo.

#### Scenario: User changes the similarity threshold
- **WHEN** the user adjusts the similarity threshold control
- **THEN** the system recomputes and re-displays the duplicate groups using the new threshold, without triggering a new analysis job

### Requirement: Default best-photo selection within a group
The system SHALL, within each duplicate group, leave unchecked (kept) the photo with the highest blur score (sharpest) and pre-check every other photo in the group for deletion. When multiple photos in a group have an identical blur score, the system SHALL keep the photo that sorts first by filename/date order.

#### Scenario: Group with a clear sharpest photo
- **WHEN** a duplicate group is displayed
- **THEN** the sharpest photo in the group is unchecked by default and all other photos in the group are checked for deletion

#### Scenario: Group with a tie in blur score
- **WHEN** two or more photos in a duplicate group have an identical blur score
- **THEN** the photo that sorts first by filename/date order is unchecked by default

### Requirement: Per-photo toggle within a duplicate group
The system SHALL allow the user to check or uncheck any individual photo within a duplicate group, overriding the default selection.

#### Scenario: User overrides the default kept photo
- **WHEN** the user unchecks the default-kept photo and checks another photo in the same group
- **THEN** the system reflects the new selection for that group when the folder is later processed
