## Context

`FolderProcessingService` currently computes kept/deleted counts and byte totals only while copying files asynchronously. The UI polls workflow status but receives no processing counters, so it can only show an indeterminate animation. The reviewed `PhotoAsset` rows already contain file sizes and deletion decisions, while videos can be enumerated from the source folder without copying.

## Goals / Non-Goals

**Goals:**
- Calculate a no-side-effect export preview before the user starts processing.
- Report photo/video counts, source bytes, kept bytes, and estimated savings.
- Track asynchronous processing progress by items and bytes and expose it through polling.
- Render a determinate progress bar and processed/total label in the export panel.
- Keep the final persisted processing result authoritative after completion.

**Non-Goals:**
- Modifying source files during preview.
- Changing which photos are deleted or which videos are copied.
- Making progress durable across an application restart; an interrupted export remains governed by existing workflow recovery rules.
- Estimating time remaining from throughput; only count and percentage are required.

## Decisions

- **Add a preview endpoint** such as `GET /api/jobs/{id}/process-preview`. It reads `PhotoAsset` metadata and lists source videos, but never creates the output directory or copies data. The preview response includes kept/deleted photo counts, video count, source bytes, kept bytes, and estimated saved bytes.
- **Define savings as deleted-photo bytes**. Every source video is always copied, so video bytes do not contribute to estimated savings. This matches the user-visible deletion decision and avoids claiming savings that processing cannot realize.
- **Use an in-memory progress registry keyed by job id** for active processing. It is suitable for the single-process, single-worker executor and avoids a schema migration. The registry is initialized before async processing starts and updated after each photo/video item.
- **Add a processing-progress endpoint** such as `GET /api/jobs/{id}/processing-progress`, returning `status`, `totalItems`, `processedItems`, `progressPercent`, and `bytesCopied`. Missing progress for a non-processing job returns a stable zero/terminal representation rather than a 404 where practical.
- **Count items, not bytes, for the main percentage**. A huge video should not make the UI appear frozen while small photos process; bytes copied are displayed as a secondary metric.
- **Keep preview and final recap separate**. Preview is transient and computed from current decisions; final recap is persisted only after successful processing and may differ if files change or processing fails.
- **Do not start processing from preview**. The user must explicitly click the existing processing button after reviewing the estimate.

## Risks / Trade-offs

- [Risk] Source files can change between preview and processing → Mitigation: label preview values as estimates and retain final recap as authoritative.
- [Risk] In-memory progress is lost on restart → Mitigation: expose terminal/interrupted status from the existing workflow and avoid claiming completion; persistence is explicitly out of scope.
- [Risk] A copy failure leaves partial output and partial progress → Mitigation: preserve existing failure handling, mark the job cancelled, and do not create a successful `ProcessingResult`.
- [Risk] Listing a very large folder for preview is non-trivial → Mitigation: stream/list only direct regular video entries and use already-persisted photo metadata; do not load image bytes.

## Migration Plan

No database migration is required. Add the preview and progress endpoints and update the static export panel together. Existing processing behavior remains valid, and old jobs simply return no active progress until a new export starts.

## Open Questions

(none)
