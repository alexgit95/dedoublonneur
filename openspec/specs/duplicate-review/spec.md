# duplicate-review Specification

## Purpose
TBD - created by archiving change add-photo-culling-workflow. Update Purpose after archive.

## Requirements

### Requirement: On-demand duplicate clustering by similarity threshold
The system SHALL group JPEG and PNG photos of the analyzed event into duplicate and near-duplicate clusters computed on demand from their stored DCT-based perceptual hashes and a similarity threshold expressed as a percentage (0-100%), without persisting the resulting groups. The clustering SHALL be able to include visually close, non-identical photos from the same burst, and the final deletion choice SHALL remain manual for every photo.

#### Scenario: User opens the duplicate review tab
- **WHEN** the user opens the "Doublons" tab after analysis has completed
- **THEN** the system computes and displays duplicate and near-duplicate groups using the current similarity threshold

#### Scenario: Burst photos are close but not identical
- **WHEN** analyzed photos from one burst have small differences in subject position, lighting, or compression
- **THEN** the system can place those photos in the same review group when their pHash Hamming distance is within the selected threshold

#### Scenario: User confirms deletion choices manually
- **WHEN** a near-duplicate group contains neighboring but intentionally different photos
- **THEN** the system presents each photo for an individual keep/delete decision and does not delete any photo before the user confirms processing

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

### Requirement: Visual priority for kept duplicate candidates
The system SHALL visually prioritize photos kept in duplicate groups and visually de-emphasize photos marked for deletion, while preserving grouping, checkbox state, and threshold behavior.

#### Scenario: Duplicate candidate is kept
- **WHEN** a photo in a duplicate group is unchecked and marked to be kept
- **THEN** its card receives the primary kept-photo visual treatment

#### Scenario: Duplicate candidate is marked for deletion
- **WHEN** a photo in a duplicate group is checked for deletion
- **THEN** its card is visibly secondary but remains inspectable and its deletion label remains available

### Requirement: Duplicate review photo preview
The system SHALL expose the shared press-and-hold fullscreen thumbnail preview for every photo card in the Doublons review tab without changing group membership, threshold recomputation, or deletion controls.

#### Scenario: User previews a duplicate candidate
- **WHEN** the user presses and holds a photo card image in a duplicate group
- **THEN** the system opens that thumbnail in the shared fullscreen preview and preserves the current group review state
