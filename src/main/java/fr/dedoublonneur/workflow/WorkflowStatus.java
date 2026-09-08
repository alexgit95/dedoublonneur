package fr.dedoublonneur.workflow;

import java.time.Instant;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.LastCompletedWorkflowStatus;

/**
 * Etat courant du workflow global, tel qu'expose par l'API (IDLE quand aucun job actif).
 */
public record WorkflowStatus(String status, Long jobId, String eventFolderName, Instant startedAt,
    Integer defaultSimilarityThreshold, Long lastCompletedJobId, String lastCompletedFolderName,
    Instant lastCompletedAt) {

    public static final String IDLE = "IDLE";

    public static WorkflowStatus idle(LastCompletedWorkflowStatus lastCompleted) {
        return new WorkflowStatus(IDLE, null, null, null, null,
                lastCompleted == null ? null : lastCompleted.jobId(),
                lastCompleted == null ? null : lastCompleted.folderName(),
                lastCompleted == null ? null : lastCompleted.processedAt());
    }

    public static WorkflowStatus of(AnalysisJob job, LastCompletedWorkflowStatus lastCompleted) {
        return new WorkflowStatus(job.getStatus().name(), job.getId(), job.getEvent().getFolderName(),
                job.getStartedAt(), job.getSimilarityThresholdDefault(),
                lastCompleted == null ? null : lastCompleted.jobId(),
                lastCompleted == null ? null : lastCompleted.folderName(),
                lastCompleted == null ? null : lastCompleted.processedAt());
    }
}
