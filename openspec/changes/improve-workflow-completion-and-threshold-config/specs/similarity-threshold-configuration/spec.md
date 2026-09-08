## ADDED Requirements

### Requirement: Environment-configured default similarity threshold
The system SHALL read the default duplicate-similarity threshold from `APP_DEFAULT_SIMILARITY_THRESHOLD`, SHALL use `90` when the variable is absent, and SHALL require the resolved value to be an integer from 0 to 100.

#### Scenario: Environment variable is absent
- **WHEN** the application starts without `APP_DEFAULT_SIMILARITY_THRESHOLD`
- **THEN** new analysis jobs use a default similarity threshold of 90

#### Scenario: Environment variable is valid
- **WHEN** the application starts with `APP_DEFAULT_SIMILARITY_THRESHOLD=85`
- **THEN** new analysis jobs store 85 as their default similarity threshold

#### Scenario: Environment variable is invalid
- **WHEN** the configured threshold is non-numeric or outside 0 to 100
- **THEN** application configuration validation fails with a clear error

### Requirement: Per-job default threshold exposed to review
The system SHALL initialize the duplicate-review threshold control from the threshold stored on the current analysis job, while allowing the user to change the threshold for on-demand clustering.

#### Scenario: Review opens for a job with a configured default
- **WHEN** the duplicate-review tab opens for a job whose stored default threshold is 85
- **THEN** the slider starts at 85 and the user can adjust it without changing the stored job default
