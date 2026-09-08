## ADDED Requirements

### Requirement: Responsive processing metrics and progress
The system SHALL display processing preview metrics, determinate progress, and processed/total labels without horizontal overflow or overlapping controls on desktop and mobile layouts.

#### Scenario: User views the estimate on mobile
- **WHEN** the user calculates the processing preview on a narrow viewport
- **THEN** the metrics wrap into readable rows without overlapping the form or buttons

#### Scenario: User views active progress on mobile
- **WHEN** processing is active on a narrow viewport
- **THEN** the progress bar and processed/total label remain visible and readable
