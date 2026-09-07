## Context

The backend already models a serial workflow with the states `IDLE`, `ANALYZING`, `READY_FOR_REVIEW`, `PROCESSING`, and `DONE`, and exposes APIs for event folders, job progress, review data, processing, and global status. The frontend currently consists of independent static pages for blurry photos, duplicates, and processing. Those pages receive `jobId` through the query string and have no common navigation or root entry point.

The change must improve discoverability and recovery without changing the resource-constrained deployment model: Spring Boot serves static resources, the frontend remains vanilla HTML/CSS/JavaScript, and the application must continue to work on a Raspberry Pi and on mobile browsers. Existing review behaviors such as pagination, lazy thumbnails, deletion toggles, and the duplicate threshold control remain part of the review step.

## Goals / Non-Goals

**Goals:**

- Make `/` the clear entry point for selecting an event folder or resuming the active workflow.
- Present folder selection, analysis, review, and export as one understandable workflow screen.
- Add a persistent stepper/breadcrumb that reflects workflow state and the next available action.
- Present Flou and Doublons as review tabs without losing the existing review functionality.
- Keep the active `jobId` and event context in one shared frontend state instead of requiring manual URL construction.
- Support mobile and desktop layouts with the existing lightweight frontend stack.
- Preserve direct page URLs as compatibility fallbacks during the transition where practical.

**Non-Goals:**

- No replacement of the vanilla frontend with React, Vue, or another framework.
- No redesign of the analysis algorithms, duplicate grouping, blur detection, or processing semantics.
- No multi-user session model; the existing single global workflow lock remains authoritative.
- No new authentication or authorization layer.
- No change to the review rules for marking photos for deletion.

## Decisions

### 1. Use a root landing page plus a shared workflow shell

Add `index.html` as the user-facing entry point and make it load a shared workflow controller. The shell owns the event/job context, reads the global workflow status, chooses the current step, and switches the visible panel. The existing review and processing logic can be extracted into panel-oriented modules or adapted behind the shell.

This is preferred over keeping separate pages connected by links because it removes the need for users to understand `jobId` URLs and gives the breadcrumb one authoritative place to render. It also avoids introducing a full client-side router or framework.

### 2. Map persisted workflow states to four visible steps

Use this mapping in the frontend:

```text
IDLE                  -> folder
ANALYZING             -> analysis
READY_FOR_REVIEW      -> review
PROCESSING            -> export
DONE                  -> export (recap)
```

The stepper marks earlier steps complete only when the corresponding state has been reached. Tabs that require a later state remain disabled. The state returned by `/api/workflow/status` remains the source of truth; local tab selection only controls presentation within the currently accessible step.

### 3. Keep Flou and Doublons as nested review tabs

The review step will expose two tabs labelled `Flou` and `Doublons`. Their current data-fetching endpoints and interactions remain unchanged. The shared shell supplies the active job context to the review modules rather than relying on each document independently parsing `window.location.search`.

This preserves the existing server-side pagination and debounced duplicate threshold behavior while making the relationship between both reviews visible to the user.

### 4. Make resume context explicit

The shell will call the workflow status endpoint on load and poll it during analysis or processing. The status response must provide, directly or through an associated job-status request, enough information to identify the active job and event folder. If the current DTO does not expose that context, extend the response with a nullable active job identifier and folder name rather than attempting to infer them from browser history or DOM state.

The frontend may store the last selected tab in the URL hash or local browser state, but it must always revalidate the workflow status before restoring that tab. Persisted backend state takes precedence over stale client state.

### 5. Use progressive enhancement for existing direct URLs

Keep `/flou.html?jobId=...`, `/doublons.html?jobId=...`, and `/traiter.html?jobId=...` functional while the unified screen is introduced. Add clear links back to `/` where needed. This gives operators a fallback during deployment and avoids making the change depend on an atomic frontend/backend cutover.

### 6. Centralize shared presentation primitives

Extend `shared.js` or add a small adjacent module for stepper rendering, workflow context, status labels, and tab activation. Keep photo-card and pagination helpers reusable. Styles for the shell should use stable grid/flex dimensions and responsive breakpoints consistent with the existing static CSS rather than introducing a separate design system or dependency.

## Risks / Trade-offs

- **[Risk]** The current status response may not contain an active job identifier or folder name. → **Mitigation:** inspect the DTO and add the smallest nullable context fields or a dedicated status endpoint; never guess the job from unrelated database rows.
- **[Risk]** Moving independent pages into panels could duplicate or break their event listeners. → **Mitigation:** isolate each panel's initialization and make initialization idempotent when tabs are revisited.
- **[Risk]** A user may keep a stale tab open while another workflow state changes. → **Mitigation:** poll status during long-running states and disable actions that no longer match the server state.
- **[Risk]** A four-step layout can become cramped on mobile. → **Mitigation:** use a horizontally scrollable or compact stepper with the active step always visible, and verify at narrow viewport widths.
- **[Risk]** Preserving direct legacy URLs temporarily increases frontend paths. → **Mitigation:** keep the compatibility layer thin and route new navigation exclusively through `/`.
- **[Risk]** Users may interpret the review step as complete after viewing only one review tab. → **Mitigation:** keep both review tabs visible and make the export action explicit from the shared review step.

## Migration Plan

1. Add the new root page and shared workflow shell without removing the existing static pages.
2. Add or adapt the minimal API response fields required to resume the active workflow.
3. Move or wrap the existing review and processing behaviors into shell panels while retaining direct URL compatibility.
4. Add frontend tests for status-to-step mapping, tab accessibility, resume behavior, and mobile-safe rendering assumptions; add backend tests if the status contract changes.
5. Update the README with the new entry point and supported resume behavior.
6. Roll back by serving the existing static pages and API contract if the unified shell has a regression; no database migration is expected.

## Open Questions

- Whether the active workflow status DTO already contains the job identifier and folder name, or whether a small response-contract extension is required.
- Whether the analysis progress view should expose a manual resume/cancel action immediately, or only after the backend reports a recoverable interrupted job.
- Whether the last selected review sub-tab should be restored after a refresh, or always default to Flou for a predictable entry point.
