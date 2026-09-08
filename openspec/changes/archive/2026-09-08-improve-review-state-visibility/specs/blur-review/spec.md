## ADDED Requirements

### Requirement: Visual priority for kept blurry photos
The system SHALL visually prioritize blurry photos that are kept and visually de-emphasize blurry photos marked for deletion, while preserving their existing checkbox state and review behavior.

#### Scenario: Blurry photo is kept
- **WHEN** a blurry photo is unchecked and marked to be kept
- **THEN** its card remains fully readable and receives the primary kept-photo visual treatment

#### Scenario: Blurry photo is marked for deletion
- **WHEN** a blurry photo is checked for deletion
- **THEN** its card is visibly secondary but remains inspectable and its deletion label remains available
