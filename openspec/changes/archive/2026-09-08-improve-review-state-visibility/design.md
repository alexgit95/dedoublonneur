## Context

The shared review card in `shared.js` currently adds `photo-card--keeping` when a photo is not marked for deletion, but the stylesheet lowers its opacity and scales it down. This makes the safe output choice visually weaker than the deletion choice.

The folder selection endpoint currently returns only `List<String>` folder names. The domain already persists `Event`, `AnalysisJob`, and `ProcessingResult`; a completed export sets the job to `DONE` and stores a processing result, so the backend can derive processed-folder state without schema changes.

## Goals / Non-Goals

**Goals:**
- Make kept photos the visually primary state in both review tabs.
- Make photos marked for deletion visibly secondary without hiding them or changing their checkbox state.
- Return a stable enriched folder-list DTO with an explicit `processed` flag and optional completion timestamp.
- Set `processed` only when an export has completed (`AnalysisJob.status = DONE` with a processing result).
- Display a clear processed badge on the folder selection screen while preserving selection and reprocessing.

**Non-Goals:**
- Changing deletion defaults, pHash grouping, blur thresholds, or processing behavior.
- Treating `READY_FOR_REVIEW`, `ANALYZING`, or `CANCELLED` as processed.
- Blocking a user from selecting or reprocessing a folder that was already exported.
- Adding database columns or changing existing entities.

## Decisions

- **Rename the visual deletion class to `photo-card--marked-for-deletion`** and make it the dimmed state. Keep the existing `photo-card--keeping` class only if compatibility is useful, but ensure the kept state has full opacity, normal scale, and a clear accent border/glow.
- **Use both visual and textual state cues**: kept cards receive a visible `Conservee`/keep treatment while deletion cards remain visibly labeled `A supprimer`. This avoids relying on color or brightness alone.
- **Introduce `EventFolderResponse`** with `name`, `processed`, and nullable `processedAt` fields. This is an additive API response change for `GET /api/events`; the frontend is the only current consumer, so it can switch from strings to objects without changing the start-analysis URL.
- **Derive processed status from completed jobs and processing results** using a repository query grouped by event, preferably returning the latest `processedAt` per folder in one query. Do not perform one database request per folder. A `DONE` job without a processing result is not considered processed.
- **Keep folder listing as the source of truth for available directories**: merge filesystem folders from `EventFolderService` with the completed-export projection from the database. Historical events whose source folder no longer exists are not displayed.
- **Use a neutral processed badge** such as `Deja exporte` with a check icon/text, while leaving the folder selectable. The badge must not be the only indication because color-only status is inaccessible.

## Risks / Trade-offs

- [Risk] A user may interpret “Déjà exporté” as “do not select” → Mitigation: keep the radio control active and label the badge as informational; do not disable the folder.
- [Risk] A folder can have multiple jobs after reprocessing → Mitigation: report processed when a completed result exists and expose the most recent completion timestamp.
- [Risk] A deletion card may become too dim to inspect → Mitigation: preserve readable thumbnail contrast and keep the press-and-hold thumbnail preview available.
- [Risk] API consumers may expect strings from `GET /api/events` → Mitigation: update the in-repository frontend and controller tests together; document the additive response contract in OpenAPI.

## Migration Plan

No database migration is required. Deploy backend and frontend together because the folder-list JSON shape changes from strings to objects. Existing events and completed processing results are immediately eligible for the badge. Rollback requires reverting the controller DTO and frontend mapping together.

## Open Questions

(none)
