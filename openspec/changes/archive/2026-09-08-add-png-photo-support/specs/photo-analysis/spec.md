## MODIFIED Requirements

### Requirement: Snapshot of files at analysis start
The system SHALL snapshot the list of JPEG and PNG photo files present in the selected event folder at the moment analysis starts, and SHALL only analyze files present in that snapshot, ignoring any files added to the source folder afterwards.

#### Scenario: Photos added after analysis has started
- **WHEN** new photo files are added to the source folder while an analysis is in progress
- **THEN** the system does not include those new files in the current analysis job

#### Scenario: Event folder contains PNG photos
- **WHEN** the selected event folder contains `.png` files (case-insensitive extension)
- **THEN** the system includes those files in the snapshot alongside any `.jpg`/`.jpeg` files

### Requirement: Per-photo blur score computation
The system SHALL compute a sharpness/blur score for each JPEG or PNG photo in the snapshot, using a variance-of-Laplacian style algorithm on a downscaled grayscale version of the image, without modifying the original file.

#### Scenario: Blur score computed for a photo
- **WHEN** the analysis job processes a photo
- **THEN** the system persists a numeric blur score for that photo, and the original file on the source folder remains byte-for-byte unchanged

### Requirement: Per-photo perceptual hash computation
The system SHALL compute a DCT-based perceptual hash (pHash) for each JPEG or PNG photo in the snapshot and persist it alongside the photo record, without persisting any duplicate grouping. The pHash SHALL be suitable for comparing visually close, non-identical photos such as images from the same burst.

#### Scenario: pHash computed for a photo
- **WHEN** the analysis job processes a photo
- **THEN** the system persists the photo's perceptual hash value for later on-demand similarity clustering

#### Scenario: Near-duplicate burst photos receive comparable hashes
- **WHEN** the analysis job processes two photos from the same burst with small changes in subject position, lighting, or compression
- **THEN** their persisted pHashes have a smaller Hamming distance than the distance observed for representative photos from different scenes
