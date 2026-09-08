## Context

The application uses a vanilla HTML/CSS/JavaScript review shell with shared photo cards in `shared.js`. The Flou and Doublons tabs both render lazy-loaded thumbnail cards, and the current stylesheet uses a light teal/warm palette. A photo card only exposes a 320px thumbnail URL, so enlarging that element would not provide a useful inspection view.

The requested interaction is a temporary inspection gesture: press and hold a photo, inspect its existing thumbnail fullscreen, and close the preview as soon as the pointer or touch is released. The existing checkbox remains the control for keep/delete decisions and must not be swallowed by the preview gesture.

## Goals / Non-Goals

**Goals:**
- Establish a coherent black/deep-charcoal and neon-red Tron/Ares-inspired visual system across the workflow shell and review surfaces.
- Add a responsive fullscreen thumbnail preview shared by Flou and Doublons cards.
- Support mouse, touch, and pen input through Pointer Events, with release/cancel cleanup and keyboard escape support.
- Keep the UI framework-free, accessible, and usable on mobile and desktop.
- Preserve existing review selection, pagination, lazy loading, and API contracts without adding a new image endpoint.

**Non-Goals:**
- Rebuilding the application as a SPA or adding a CSS/JS framework.
- Editing, cropping, annotating, zooming, or rotating photos in the preview.
- Changing duplicate detection, blur scoring, deletion behavior, or workflow state transitions.
- Opening the preview from the checkbox or its label.

## Decisions

- **Use a shared overlay component in `shared.js`** rather than duplicating logic in `blur-review.js` and `duplicate-review.js`. Both tabs already use `createPhotoCard`, so the gesture and lifecycle can be kept consistent.
- **Use Pointer Events with a short hold threshold (approximately 450 ms)**. `pointerdown` starts the timer; once the threshold is reached, the overlay opens. `pointerup`, `pointercancel`, `pointerleave`, and `lostpointercapture` close it. Pointer capture is used while the gesture is active so release outside the card still closes the preview.
- **Ignore interactive descendants**: a press originating from the checkbox or its label does not start a preview. This preserves the existing toggle behavior and avoids a fullscreen overlay opening while the user changes deletion state.
- **Reuse the existing thumbnail URL for the preview**. The overlay sets its image source to `photo.thumbnailUrl`, avoiding a second network request and keeping the interaction responsive on mobile and constrained networks. The overlay makes the thumbnail large visually, but does not claim to provide original-resolution inspection.
- **Use a single overlay host in each page shell** with a backdrop, image, close button, and `aria-modal="true"`. The overlay is closed on release, explicit close, `Escape`, and overlay backdrop click. The page restores its previous focus when the overlay closes.
- **Use CSS variables and restrained circuit-line decoration**: near-black surfaces, off-white text, neon red as the action/focus color, and a small secondary cyan signal color only where needed for legibility. Lines are implemented with borders/pseudo-elements and subtle repeating patterns, not large decorative blobs.
- **Respect reduced motion** with a `prefers-reduced-motion` override. Focus rings use a visible neon outline with sufficient contrast, and card dimensions remain stable while hover/hold states animate.

## Risks / Trade-offs

- [Risk] Enlarging a thumbnail can look soft or pixelated → Mitigation: accept the intentional resolution trade-off for fluidity and network savings; retain the original-resolution image for the existing export workflow rather than loading it during review.
- [Risk] Touch scrolling can be mistaken for a hold → Mitigation: cancel the hold when movement exceeds a small tolerance before the threshold; do not call `preventDefault` until the preview is actually opened.
- [Risk] Holding a card can conflict with native browser image behavior → Mitigation: disable image dragging during the gesture and use Pointer Events consistently; keep normal click/tap behavior unchanged.
- [Risk] A cached thumbnail can be unavailable or stale → Mitigation: reuse the existing thumbnail error handling and close safely without blocking the review list.
- [Risk] A neon theme can reduce readability or become visually noisy → Mitigation: keep body text off-white, use red primarily for actions/focus/state, preserve spacing, and test desktop/mobile contrast and overflow.

## Migration Plan

No database migration or backend endpoint is required. Deploy the static frontend together with the CSS/JavaScript changes. Existing photo cards continue to use the thumbnail URL during preview. Rollback is a static asset rollback; existing photo and analysis data remain compatible.

## Open Questions

(none)
