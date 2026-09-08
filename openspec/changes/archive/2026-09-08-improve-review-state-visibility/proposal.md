## Why

The review cards currently emphasize photos marked for deletion and visually de-emphasize photos that will be kept, making the safest user decision harder to spot at a glance. The folder selection screen also shows only directory names, so users cannot tell which folders have already completed an export and which are merely analyzed or still new.

## What Changes

- Reverse the review-card visual hierarchy: make kept photos prominent and readable, while dimming photos marked for deletion.
- Keep the checkbox state and deletion behavior unchanged; only the visual emphasis and accessible state labeling change.
- Enrich the event-folder listing response with whether an export has completed and, when available, its completion timestamp.
- Display a clear `Deja exporte` indicator on folders whose latest completed workflow has status `DONE`.
- Do not mark a folder as processed merely because analysis reached `READY_FOR_REVIEW`; only a completed export counts.
- Preserve the ability to select and analyze a folder again after it has been exported.

## Capabilities

### New Capabilities
- `processed-folder-indicator`: expose and display completed-export status for folders on the selection screen.

### Modified Capabilities
- `blur-review`: visually prioritize photos kept in the output and visually attenuate photos marked for deletion.
- `duplicate-review`: visually prioritize photos kept in the output and visually attenuate photos marked for deletion.
- `event-folder-workflow`: enrich folder listing with completed-export state based only on a `DONE` workflow.
- `responsive-ui`: keep the new card states and folder badges readable and usable across desktop and mobile layouts.

## Impact

- `PhotoAsset` shared card state rendering and CSS classes in the review UI.
- `EventController`, repositories, and response DTOs for enriched folder-list data.
- `workflow.js` and folder-selection markup for the processed badge.
- Unit/integration tests for card state rendering, event listing status, and `DONE` versus `READY_FOR_REVIEW` behavior.
- No database schema change is expected; existing `AnalysisJob` and `ProcessingResult` data already identify completed exports.
