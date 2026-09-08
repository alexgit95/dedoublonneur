# responsive-ui Specification

## Purpose
TBD - created by archiving change add-photo-culling-workflow. Update Purpose after archive.
## Requirements
### Requirement: Lazy-loaded, paginated thumbnail grids
The system SHALL load photo thumbnails lazily and paginate review-tab results so that the browser never renders or downloads the full set of photos for an event at once.

#### Scenario: User scrolls through a large blur/duplicate result set
- **WHEN** the user opens a review tab for an event with hundreds of photos
- **THEN** only the thumbnails for the currently visible page are downloaded and rendered, with additional pages loaded as the user navigates or scrolls

### Requirement: Debounced similarity threshold interaction
The system SHALL debounce similarity-threshold change requests so that dragging the threshold control does not trigger a server recomputation for every intermediate value.

#### Scenario: User drags the similarity threshold slider
- **WHEN** the user moves the similarity threshold control continuously
- **THEN** the system issues a single recomputation request shortly after the user stops moving the control, rather than one request per intermediate value

### Requirement: Framework-free, low-overhead frontend
The system SHALL implement the web UI using vanilla HTML/CSS/JavaScript, without a client-side SPA framework or build/bundler step, and SHALL use CSS-based transitions for visual feedback (tab switches, progress indication, state changes).

#### Scenario: A tab switch or state change occurs
- **WHEN** the user switches between review tabs or toggles a photo's keep/delete state
- **THEN** the visual transition is rendered via CSS animation/transition without a full page reload and without loading a JavaScript framework

### Requirement: Neon review visual system
The system SHALL provide a responsive black/deep-charcoal and neon-red visual theme inspired by Tron/Ares across the workflow shell, stepper, review tabs, photo cards, controls, progress states, and status messages. The theme SHALL preserve readable contrast and stable layouts on desktop and mobile.

#### Scenario: User opens the workflow on desktop
- **WHEN** the user opens the workflow at a desktop viewport
- **THEN** the shell and review surfaces use the neon visual system without horizontal overflow or overlapping controls

#### Scenario: User opens the workflow on mobile
- **WHEN** the user opens a review tab on a narrow touch viewport
- **THEN** the controls, photo cards, and tabs remain usable, fit within the viewport, and preserve visible focus/active states

#### Scenario: User prefers reduced motion
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** decorative and state transitions are reduced or disabled without removing functional feedback

### Requirement: Responsive state emphasis
The system SHALL keep kept/deletion card treatments and processed-folder badges readable and non-overlapping across desktop and mobile review and folder-selection layouts.

#### Scenario: Review cards fit on mobile
- **WHEN** the user opens a review grid on a narrow viewport
- **THEN** the kept and deletion visual treatments do not obscure the thumbnail, checkbox, or state label

#### Scenario: Processed badge fits on mobile
- **WHEN** a processed folder is displayed on a narrow viewport
- **THEN** its `Deja exporte` indicator wraps or reflows without overlapping the folder name or selection control

### Requirement: Responsive processing metrics and progress
The system SHALL display processing preview metrics, determinate progress, and processed/total labels without horizontal overflow or overlapping controls on desktop and mobile layouts.

#### Scenario: User views the estimate on mobile
- **WHEN** the user calculates the processing preview on a narrow viewport
- **THEN** the metrics wrap into readable rows without overlapping the form or buttons

#### Scenario: User views active progress on mobile
- **WHEN** processing is active on a narrow viewport
- **THEN** the progress bar and processed/total label remain visible and readable

### Requirement: Responsive cleanup feedback
The system SHALL keep the cleanup action, saved-space indicator, confirmation outcome, and partial-cleanup error message readable and non-overlapping across desktop and mobile folder-selection layouts.

#### Scenario: Processed tile on mobile
- **WHEN** a processed folder is displayed on a narrow viewport
- **THEN** the saved-space text and cleanup action wrap without overlapping the folder name or selection control

#### Scenario: Partial cleanup response
- **WHEN** cleanup completes with one or more errors
- **THEN** the UI displays a clear partial-cleanup message and failed-path details without hiding the retry action

