package fr.dedoublonneur.workflow;

import java.time.Instant;

import fr.dedoublonneur.domain.AnalysisJob;

/**
 * Etat courant du workflow global, tel qu'expose par l'API (IDLE quand aucun job actif).
 */
public record WorkflowStatus(String status, Long jobId, String eventFolderName, Instant startedAt) {

    public static final String IDLE = "IDLE";

    public static WorkflowStatus idle() {
        return new WorkflowStatus(IDLE, null, null, null);
    }

    public static WorkflowStatus of(AnalysisJob job) {
        return new WorkflowStatus(job.getStatus().name(), job.getId(), job.getEvent().getFolderName(),
                job.getStartedAt());
    }
}
