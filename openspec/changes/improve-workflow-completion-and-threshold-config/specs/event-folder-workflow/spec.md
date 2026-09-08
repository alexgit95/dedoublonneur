## MODIFIED Requirements

### Requirement: Single active workflow lock
The system SHALL allow at most one active workflow (analysis, review, or processing) at any time across the whole application. A workflow is considered active from the moment analysis starts until the folder has been fully processed (or the workflow is explicitly cancelled/reset). Jobs in `DONE` or `CANCELLED` SHALL never block a new analysis.

#### Scenario: Attempt to start a second analysis while one is active
- **WHEN** a user requests to start an analysis on a new event folder while another workflow is in status `ANALYZING`, `READY_FOR_REVIEW`, or `PROCESSING`
- **THEN** the system rejects the request with HTTP 409 and indicates which folder is currently being worked on

#### Scenario: Starting a new analysis after export
- **WHEN** the previous workflow has completed with status `DONE`
- **THEN** the system treats the active workflow as `IDLE` and allows a new analysis, including on the same event folder, without HTTP 409

#### Scenario: Two clients start concurrently after completion
- **WHEN** two clients request analysis after the previous job is `DONE`
- **THEN** exactly one new analysis starts and the other receives HTTP 409 because the new workflow is genuinely active
