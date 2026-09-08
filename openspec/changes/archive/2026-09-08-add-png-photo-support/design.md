## Context

Analysis snapshotting only recognizes `.jpg`/`.jpeg` files (case-insensitive) via a hardcoded extension list in `AnalysisOrchestrator`. Manual testing with a folder containing only `.png` duplicates showed the snapshot resolves to an empty list, so the whole analysis/duplicate pipeline silently processes nothing. `ImageAnalysisService` and `ThumbnailService` already use `ImageIO.read`/`ImageIO.write`, which natively supports PNG, so no change is needed there.

## Goals / Non-Goals

**Goals:**
- Recognize `.png` (case-insensitive) as a valid photo file alongside `.jpg`/`.jpeg` when building the analysis snapshot.
- Ensure blur score, pHash, thumbnail generation, and metadata-preserving copy behave the same for PNG as for JPEG.
- Detect visually close, non-identical photos from the same burst, not only byte-identical copies.
- Preserve manual review as the final authority before any photo is deleted.

**Non-Goals:**
- Adding support for other formats (HEIC, TIFF, WebP, etc.) — out of scope for this change.
- Detecting arbitrary image similarity across different scenes or large crops.

## Decisions

- **Extend `JPEG_EXTENSIONS` list to include `.png`** in `AnalysisOrchestrator` rather than introducing a new abstraction (e.g., pluggable format registry). The list is a single small constant used in one place; a broader abstraction isn't justified for one added extension.
- **Rename the constant/wording from "JPEG" to "photo"** where it refers to the snapshot filter, since it's no longer JPEG-only (comments, README). Domain class/field names (`PhotoAsset`, DB columns) are left as-is — renaming them is a larger, unrelated change.
- **Use a DCT-based pHash rather than the current average-hash**. Average-hash is adequate for exact copies but is too sensitive to luminance and local changes for burst photos. A DCT pHash compares low-frequency visual structure and is better suited to recompression, lighting variation, and small movements while retaining a compact 64-bit Hamming-distance representation.
- **Keep the existing Hamming-distance clustering and transitive grouping**. A burst can evolve gradually, so grouping A-B and B-C into one review group is preferable to missing the sequence because A and C differ more directly. The UI remains the safeguard against unwanted deletion.
- **Calibrate the default threshold with burst fixtures instead of assuming the current 90% mapping remains correct**. The similarity control remains adjustable; exact threshold values are an implementation/test decision after measuring distances from positive burst pairs and negative pairs.
- **No format-specific branching in `ImageAnalysisService`/`ThumbnailService`**: `ImageIO` already decodes PNG transparently, and thumbnails are always re-encoded as `.jpg` regardless of source format (existing behavior), so PNG source photos produce JPEG thumbnails like today.
- **Metadata-preserving copy (`folder-processing`) is unaffected**: it operates on whatever the snapshot contains via raw byte copy, so no spec change needed there.

## Risks / Trade-offs

- [Risk] PNG files can carry an alpha channel; `toGrayscale` draws into a `TYPE_BYTE_GRAY` image without explicitly flattening transparency onto a background, which could produce a black background for transparent areas → skewed blur/hash for photos with transparency. Mitigation: real photos exported from cameras/phones are practically always opaque; this is an accepted, documented limitation rather than a fix in this change.
- [Risk] Broadening the snapshot filter could pick up unrelated PNG assets (e.g., screenshots) placed in an event folder. Mitigation: unchanged from today's JPEG behavior — the app already trusts whatever files are physically present in the selected event folder.
- [Risk] A permissive pHash threshold can group neighboring but intentionally different photos. Mitigation: keep deletion decisions manual, add positive burst and negative near-scene fixtures, and make the threshold adjustable in the review UI.
- [Risk] DCT pHash distance distributions differ from average-hash, so the current 90% threshold may be too strict or too broad. Mitigation: calibrate the default against representative burst pairs before finalizing the implementation.

## Open Questions

(none)
