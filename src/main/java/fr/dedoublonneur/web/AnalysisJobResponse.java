package fr.dedoublonneur.web;

import java.time.Instant;

/**
 * Representation API d'un {@link fr.dedoublonneur.domain.AnalysisJob} nouvellement demarre.
 */
public record AnalysisJobResponse(Long id, String eventFolderName, String status, Instant startedAt,
    int defaultSimilarityThreshold) {

    /**
     * Le nom de dossier est fourni explicitement plutot que lu via job.getEvent() :
     * le job peut etre un objet detache (transaction deja fermee) a cet instant.
     */
    public static AnalysisJobResponse from(fr.dedoublonneur.domain.AnalysisJob job, String eventFolderName) {
        return new AnalysisJobResponse(job.getId(), eventFolderName, job.getStatus().name(),
                job.getStartedAt(), job.getSimilarityThresholdDefault());
    }
}
