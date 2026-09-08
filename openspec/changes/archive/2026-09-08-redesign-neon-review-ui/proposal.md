## Why

The current review interface is functional but visually neutral, and inspecting a photo requires navigating away from the review grid or relying on a small thumbnail. A focused visual language and a fast press-and-hold preview will make the Flou and Doublons reviews feel more deliberate while keeping the repeated keep/delete workflow efficient on desktop and touch devices.

## What Changes

- Redesign the workflow and review UI with a black, deep-charcoal, and neon-red visual system inspired by Tron/Ares: luminous red accents, restrained circuit-line motifs, high-contrast controls, and responsive behavior.
- Apply the visual treatment consistently to the workflow shell, stepper, review tabs, photo cards, progress states, sliders, buttons, and status messages without changing workflow behavior or API contracts.
- Add a press-and-hold photo preview in both the Flou and Doublons review tabs.
- Open the selected thumbnail in a fullscreen/lightbox overlay while the pointer or touch hold is active, and close it on release, cancellation, or overlay dismissal.
- Reuse the existing thumbnail URL in the fullscreen overlay so the interaction stays fluid and does not download the original photo over the network.
- Preserve ordinary tap/click behavior for photo selection controls and keyboard accessibility; the preview must not interfere with checkbox toggles or scrolling.

## Capabilities

### New Capabilities
- `photo-preview`: temporary fullscreen preview of a reviewed photo activated by a sustained pointer/touch press.

### Modified Capabilities
- `responsive-ui`: add the neon visual theme and ensure the review shell and preview work across desktop and mobile viewports.
- `blur-review`: expose the press-and-hold preview on blurred-photo cards.
- `duplicate-review`: expose the press-and-hold preview on duplicate-group photo cards.

## Impact

- `src/main/resources/static/css/style.css`: color tokens, circuit-line visual motifs, contrast, focus states, responsive layout, and fullscreen overlay styling.
- `src/main/resources/static/js/shared.js`: shared long-press/pointer preview behavior and overlay lifecycle.
- `src/main/resources/static/js/blur-review.js` and `duplicate-review.js`: attach preview behavior without breaking card checkbox actions.
- `src/main/resources/static/index.html` and legacy review pages: shared preview overlay host and theme hooks if required.
- Reuse the existing thumbnail API for the preview; no new endpoint, database schema change, or external dependency is expected.
- Add focused JavaScript tests for hold timing, release/cancel behavior, overlay cleanup, and non-interference with selection controls.
