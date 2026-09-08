## MODIFIED Requirements

### Requirement: On-demand duplicate clustering by similarity threshold
The system SHALL group JPEG and PNG photos of the analyzed event into duplicate and near-duplicate clusters computed on demand from their stored DCT-based perceptual hashes and a similarity threshold expressed as a percentage (0-100%), without persisting the resulting groups. The clustering SHALL be able to include visually close, non-identical photos from the same burst, and the final deletion choice SHALL remain manual for every photo.

#### Scenario: User opens the duplicate review tab
- **WHEN** the user opens the "Doublons" tab after analysis has completed
- **THEN** the system computes and displays duplicate and near-duplicate groups using the current similarity threshold

#### Scenario: User changes the similarity threshold
- **WHEN** the user adjusts the similarity threshold control
- **THEN** the system recomputes and re-displays the duplicate and near-duplicate groups from stored pHash values without re-analyzing any photo

#### Scenario: Burst photos are close but not identical
- **WHEN** analyzed photos from one burst have small differences in subject position, lighting, or compression
- **THEN** the system can place those photos in the same review group when their pHash Hamming distance is within the selected threshold

#### Scenario: User confirms deletion choices manually
- **WHEN** a near-duplicate group contains neighboring but intentionally different photos
- **THEN** the system presents each photo for an individual keep/delete decision and does not delete any photo before the user confirms processing