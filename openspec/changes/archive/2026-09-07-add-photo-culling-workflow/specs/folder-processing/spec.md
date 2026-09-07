## ADDED Requirements

### Requirement: Output folder name validation
The system SHALL require the user to provide an output folder name when triggering processing, and SHALL reject the request if a folder with that name already exists under the NAS output location.

#### Scenario: User submits an output folder name that already exists
- **WHEN** the user triggers "Traiter le dossier" with a name that already exists under the output location
- **THEN** the system rejects the request and does not start processing

#### Scenario: User submits a new, unique output folder name
- **WHEN** the user triggers "Traiter le dossier" with a name that does not yet exist under the output location
- **THEN** the system creates the output folder and starts processing

### Requirement: Metadata-preserving copy of kept photos
The system SHALL copy every photo not marked for deletion (across both blur and duplicate review) to the output folder using a raw byte copy that preserves all original file metadata, including EXIF and GPS data and file timestamps.

#### Scenario: A kept photo is copied to the output folder
- **WHEN** the folder is processed
- **THEN** the kept photo appears in the output folder with identical EXIF/GPS metadata and timestamps as the source file, and the source file remains unmodified

### Requirement: Video files are copied unconditionally
The system SHALL copy every video file present in the source event folder to the output folder as-is, without analysis, and without including them in the kept/deleted photo counts of the recap.

#### Scenario: Source folder contains a video file
- **WHEN** the folder is processed
- **THEN** the video file is copied to the output folder and is not counted among the kept/deleted photo counts in the recap

### Requirement: Deleted photos are never copied and source is untouched
The system SHALL NOT copy photos marked for deletion to the output folder, and SHALL NOT modify or delete anything in the source event folder during processing.

#### Scenario: A photo marked for deletion is processed
- **WHEN** the folder is processed
- **THEN** the deleted photo is absent from the output folder and remains untouched in the source folder

### Requirement: Processing recap
The system SHALL present a recap after processing completes, including the number of photos kept, the number of photos deleted, disk space used before processing, and disk space used after processing (output folder size).

#### Scenario: Processing completes successfully
- **WHEN** processing of the folder finishes
- **THEN** the system displays the count of kept photos, the count of deleted photos, and the disk space before vs. after
