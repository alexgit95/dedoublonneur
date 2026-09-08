## 1. Neon visual theme

- [x] 1.1 Define the black/deep-charcoal, neon-red, off-white, and restrained secondary accent CSS variables and apply them to the workflow shell.
- [x] 1.2 Redesign stepper, tabs, buttons, fields, status messages, progress bars, cards, sliders, and focus states with Tron/Ares-inspired line details while preserving stable dimensions and readable contrast.
- [x] 1.3 Add responsive mobile rules and `prefers-reduced-motion` handling; verify no overflow or overlapping controls in the workflow and legacy review pages.

## 2. Shared fullscreen hold interaction

- [x] 2.1 Add a shared fullscreen overlay host and lifecycle in the review shell/legacy pages, including loading/error states, backdrop, close action, `Escape`, focus restoration, and body scroll handling.
- [x] 2.2 Implement Pointer Events long-press behavior in `shared.js` with a roughly 450 ms threshold, pointer capture, movement cancellation, release/cancel cleanup, and no activation from checkbox/label descendants.
- [x] 2.3 Reuse `photo.thumbnailUrl` as the overlay image source and ensure no original-image request is made.
- [x] 2.4 Add shared JavaScript tests for hold timing, movement cancellation, release cleanup, checkbox exclusion, thumbnail reuse, and overlay close behavior using `C:\dev\node-v22.23.1-win-x64\node.exe`.

## 3. Review integration and validation

- [x] 3.1 Integrate the shared preview into Flou photo cards without changing pagination or deletion toggles.
- [x] 3.2 Integrate the shared preview into Doublons photo cards without changing group rendering, threshold recomputation, or deletion toggles.
- [ ] 3.3 Run JavaScript syntax/unit tests at desktop and mobile viewport checks, then run the full Maven test suite with the required Maven binary.
- [x] 3.4 Update `README.md` and `CHANGELOG.md` with the neon review theme and press-and-hold thumbnail preview behavior.
