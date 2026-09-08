## ADDED Requirements

### Requirement: Duplicate review photo preview
The system SHALL expose the shared press-and-hold fullscreen original-photo preview for every photo card in the Doublons review tab without changing group membership, threshold recomputation, or deletion controls.

#### Scenario: User previews a duplicate candidate
- **WHEN** the user presses and holds a photo card image in a duplicate group
- **THEN** the system opens that photo in the shared fullscreen preview and preserves the current group review state
