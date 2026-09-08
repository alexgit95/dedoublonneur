## ADDED Requirements

### Requirement: Visual priority for kept duplicate candidates
The system SHALL visually prioritize photos kept in duplicate groups and visually de-emphasize photos marked for deletion, while preserving grouping, checkbox state, and threshold behavior.

#### Scenario: Duplicate candidate is kept
- **WHEN** a photo in a duplicate group is unchecked and marked to be kept
- **THEN** its card receives the primary kept-photo visual treatment

#### Scenario: Duplicate candidate is marked for deletion
- **WHEN** a photo in a duplicate group is checked for deletion
- **THEN** its card is visibly secondary but remains inspectable and its deletion label remains available
