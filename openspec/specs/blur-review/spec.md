# blur-review Specification

## Purpose
TBD - created by archiving change add-photo-culling-workflow. Update Purpose after archive.

## Requirements

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

### Requirement: Visual priority for kept blurry photos
The system SHALL visually prioritize blurry photos that are kept and visually de-emphasize blurry photos marked for deletion, while preserving their existing checkbox state and review behavior.

#### Scenario: Blurry photo is kept
- **WHEN** a blurry photo is unchecked and marked to be kept
- **THEN** its card remains fully readable and receives the primary kept-photo visual treatment

#### Scenario: Blurry photo is marked for deletion
- **WHEN** a blurry photo is checked for deletion
- **THEN** its card is visibly secondary but remains inspectable and its deletion label remains available

### Requirement: Blur review photo preview
The system SHALL expose the shared press-and-hold fullscreen thumbnail preview for every photo card in the Flou review tab without changing its pagination or deletion controls.

#### Scenario: User previews a blurry photo
- **WHEN** the user presses and holds a blurry photo card image in the Flou tab
- **THEN** the system opens that thumbnail in the shared fullscreen preview and preserves its deletion state
