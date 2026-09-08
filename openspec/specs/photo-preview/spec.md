# photo-preview Specification

## Purpose
Temporary thumbnail preview for photo review interactions.

## Requirements

### Requirement: Press-and-hold fullscreen thumbnail preview
The system SHALL provide a temporary fullscreen preview using the existing thumbnail URL when a user presses and holds a photo card for approximately 450 milliseconds in a review tab. The preview SHALL close when the pointer or touch is released, cancelled, or the overlay is explicitly dismissed, and SHALL not request the original photo file.

#### Scenario: User holds a photo on desktop
- **WHEN** the user presses and holds a photo image with a mouse or pen for the hold threshold
- **THEN** the system opens the corresponding thumbnail in a fullscreen overlay

#### Scenario: User holds a photo on touch
- **WHEN** the user touches and holds a photo image for the hold threshold without scrolling away
- **THEN** the system opens the corresponding thumbnail in a fullscreen overlay

#### Scenario: User releases the photo
- **WHEN** the user releases the pointer or touch after the preview has opened
- **THEN** the system closes the fullscreen overlay and returns to the review list at the same scroll position

#### Scenario: User moves before the hold threshold
- **WHEN** the user moves far enough to indicate scrolling before the hold threshold is reached
- **THEN** the system cancels the pending preview and leaves the review list unchanged

### Requirement: Preview does not interfere with deletion controls
The system SHALL not start a fullscreen preview when the press originates from a photo deletion checkbox or its label, and SHALL preserve the existing keep/delete toggle behavior.

#### Scenario: User toggles a deletion checkbox
- **WHEN** the user presses or taps the checkbox or its label
- **THEN** the system changes the deletion state without opening the fullscreen preview

#### Scenario: Preview reuses the thumbnail
- **WHEN** the preview opens for a photo card with an existing thumbnail URL
- **THEN** the overlay uses that thumbnail URL directly and does not initiate a request for the original source file