package fr.dedoublonneur.domain;

/**
 * Etats du cycle de vie d'un {@link AnalysisJob}, refletant le workflow global
 * (un seul job actif a la fois, cf. capability event-folder-workflow).
 */
public enum JobStatus {
    ANALYZING,
    READY_FOR_REVIEW,
    PROCESSING,
    DONE,
    CANCELLED
}
