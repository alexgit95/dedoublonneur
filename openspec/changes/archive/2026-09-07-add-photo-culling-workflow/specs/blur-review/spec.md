## ADDED Requirements

### Requirement: Blurred photos listing
The system SHALL provide a "Flou" review tab listing every photo from the completed analysis whose blur score falls below the blur threshold, paginated to avoid loading the full result set at once.

#### Scenario: User opens the blur review tab
- **WHEN** the user opens the "Flou" tab after analysis has completed
- **THEN** the system displays a paginated list of blurry photos with their thumbnails

### Requirement: Default deletion state for blurry photos
The system SHALL pre-check every blurry photo for deletion by default.

#### Scenario: Blurry photo shown with default state
- **WHEN** a blurry photo is displayed in the "Flou" tab
- **THEN** its deletion checkbox is checked by default

### Requirement: Per-photo toggle to keep a blurry photo
The system SHALL allow the user to uncheck any blurry photo to keep it instead of deleting it.

#### Scenario: User unchecks a blurry photo
- **WHEN** the user unchecks the deletion checkbox for a blurry photo
- **THEN** that photo is marked to be kept and included in the final output when the folder is processed
