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
