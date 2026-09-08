## Why

Analysis only recognizes `.jpg`/`.jpeg` files when building the snapshot of an event folder. Event folders that contain PNG photos (confirmed during manual testing with a folder of `.png` duplicates) are snapshotted as empty, so analysis silently processes zero photos and no duplicate group is ever detected — even though the files are genuine near-identical duplicates.

## What Changes

- Extend the snapshot file filter to also recognize `.png` (case-insensitive), alongside the existing `.jpg`/`.jpeg`.
- Analyze PNG photos exactly like JPEG ones: blur score, perceptual hash, thumbnail generation, metadata-preserving copy on processing.
- Replace the current average-hash implementation with a DCT-based pHash that can group near-duplicate photos, including neighboring burst photos that are visually close but not byte-identical.
- Keep duplicate review manual: a broader group is acceptable because the user confirms individual deletion choices before processing.
- Update user-facing wording ("JPEG") in the README/UI where it inaccurately implies JPEG-only support.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `photo-analysis`: the snapshot requirement changes from "JPEG photo files" to "JPEG and PNG photo files"; blur score and perceptual-hash requirements apply to both formats, with pHash explicitly robust to near-duplicate burst photos.
- `duplicate-review`: duplicate clustering must use the pHash distance to include visually close, non-identical photos while retaining manual per-photo review.

## Impact

- `AnalysisOrchestrator` (snapshot file filter — the sole gatekeeper of which files are recognized as photos).
- `ImageAnalysisService`: pHash implementation changes from average-hash to DCT-based pHash; PNG support remains format-agnostic through `ImageIO`.
- `DuplicateClusterService`: continues Hamming-distance clustering, with threshold calibration and tests updated for burst-like near duplicates.
- `ThumbnailService`: already format-agnostic (relies on `ImageIO`), no code change expected.
- `README.md`: wording referencing "photo JPEG".
- No database schema change (file extension isn't persisted as a distinct column).
