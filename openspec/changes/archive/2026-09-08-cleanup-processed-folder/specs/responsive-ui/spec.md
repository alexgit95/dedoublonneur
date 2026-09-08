## ADDED Requirements

### Requirement: Responsive cleanup feedback
The system SHALL keep the cleanup action, saved-space indicator, confirmation outcome, and partial-cleanup error message readable and non-overlapping across desktop and mobile folder-selection layouts.

#### Scenario: Processed tile on mobile
- **WHEN** a processed folder is displayed on a narrow viewport
- **THEN** the saved-space text and cleanup action wrap without overlapping the folder name or selection control

#### Scenario: Partial cleanup response
- **WHEN** cleanup completes with one or more errors
- **THEN** the UI displays a clear partial-cleanup message and failed-path details without hiding the retry action
