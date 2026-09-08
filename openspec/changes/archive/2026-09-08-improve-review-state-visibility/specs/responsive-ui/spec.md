## ADDED Requirements

### Requirement: Responsive state emphasis
The system SHALL keep kept/deletion card treatments and processed-folder badges readable and non-overlapping across desktop and mobile review and folder-selection layouts.

#### Scenario: Review cards fit on mobile
- **WHEN** the user opens a review grid on a narrow viewport
- **THEN** the kept and deletion visual treatments do not obscure the thumbnail, checkbox, or state label

#### Scenario: Processed badge fits on mobile
- **WHEN** a processed folder is displayed on a narrow viewport
- **THEN** its `Deja exporte` indicator wraps or reflows without overlapping the folder name or selection control
