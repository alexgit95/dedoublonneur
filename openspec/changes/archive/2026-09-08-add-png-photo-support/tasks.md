## 1. Snapshot filter

- [x] 1.1 Extend the file-extension filter in `AnalysisOrchestrator` (or rename the constant) to also match `.png` (case-insensitive), alongside `.jpg`/`.jpeg`.
- [x] 1.2 Update related comments/Javadoc that say "JPEG" where the snapshot now covers JPEG and PNG.
- [x] 1.3 Replace the average-hash implementation in `ImageAnalysisService` with a 64-bit DCT-based pHash while preserving the stored `long` contract and Hamming-distance comparison.

## 2. Tests

- [x] 2.1 Add/update a unit test for the snapshot builder covering a folder with `.png` files (alone and mixed with `.jpg`).
- [x] 2.2 Add/update image-analysis fixtures for exact copies, near-duplicate burst photos, and representative different scenes; assert burst pairs have smaller pHash distances than negative pairs.
- [x] 2.3 Calibrate and document the default similarity threshold against those fixtures, including the expected Hamming-distance range.
- [x] 2.4 Add/update a unit test asserting `.png` files still get a blur score and pHash computed via `ImageAnalysisService`.
- [x] 2.5 Run the full test suite via the project's Maven wrapper/path to confirm no regressions.

## 3. Documentation

- [x] 3.1 Update `README.md` wording that currently says "chaque photo JPEG" to reflect JPEG + PNG support.
- [x] 3.2 Update `CHANGELOG.md` with an `[Added]`/`[Changed]` entry describing PNG support for photo analysis and duplicate detection.
