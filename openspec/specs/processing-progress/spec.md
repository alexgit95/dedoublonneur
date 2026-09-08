# processing-progress Specification

## Purpose
Expose determinate asynchronous export progress.

## Requirements

### Requirement: Determinate asynchronous processing progress
The system SHALL expose processing progress for an active export, including status, total item count, processed item count, progress percentage, and bytes copied. The percentage SHALL be based on processed photos and videos, and SHALL reach 100 only after successful processing completes.

#### Scenario: User monitors an active export
- **WHEN** an export is processing asynchronously
- **THEN** the progress endpoint reports `PROCESSING`, processed items, total items, percentage, and bytes copied

#### Scenario: Export completes
- **WHEN** all selected photos and all source videos have been handled successfully
- **THEN** the progress reaches 100 percent and the workflow reports completion

#### Scenario: Export fails
- **WHEN** a copy error interrupts processing
- **THEN** the job does not report successful completion and the progress endpoint reports a cancelled/failed terminal state without a successful recap