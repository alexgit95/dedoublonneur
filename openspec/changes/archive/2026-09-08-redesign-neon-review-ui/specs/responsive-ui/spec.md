## ADDED Requirements

### Requirement: Neon review visual system
The system SHALL provide a responsive black/deep-charcoal and neon-red visual theme inspired by Tron/Ares across the workflow shell, stepper, review tabs, photo cards, controls, progress states, and status messages. The theme SHALL preserve readable contrast and stable layouts on desktop and mobile.

#### Scenario: User opens the workflow on desktop
- **WHEN** the user opens the workflow at a desktop viewport
- **THEN** the shell and review surfaces use the neon visual system without horizontal overflow or overlapping controls

#### Scenario: User opens the workflow on mobile
- **WHEN** the user opens a review tab on a narrow touch viewport
- **THEN** the controls, photo cards, and tabs remain usable, fit within the viewport, and preserve visible focus/active states

#### Scenario: User prefers reduced motion
- **WHEN** the browser reports `prefers-reduced-motion: reduce`
- **THEN** decorative and state transitions are reduced or disabled without removing functional feedback
