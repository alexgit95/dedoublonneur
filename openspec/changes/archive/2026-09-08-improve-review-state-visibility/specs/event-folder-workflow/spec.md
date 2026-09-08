## ADDED Requirements

### Requirement: Informational processed-folder indicator
The system SHALL display an informational `Deja exporte` indicator for event folders whose export has completed with workflow status `DONE`, and SHALL not display that indicator for folders only analyzed, under review, or cancelled.

#### Scenario: User opens folder selection with exported folders
- **WHEN** the folder selection screen loads and an available folder has a completed export
- **THEN** that folder displays a clear `Deja exporte` indicator alongside its name

#### Scenario: User opens folder selection with an unexported folder
- **WHEN** an available folder has no completed export
- **THEN** the folder is displayed without the completed-export indicator

#### Scenario: User chooses an exported folder again
- **WHEN** the user selects an available folder marked `Deja exporte`
- **THEN** the selection remains valid and the user can start a new analysis
