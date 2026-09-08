package fr.dedoublonneur.web;

/**
 * Etat de progression d'un job d'analyse, pour le polling HTTP cote IHM.
 */
public record JobProgressResponse(Long id, String eventFolderName, String status, int analyzedCount,
        int totalCount, boolean running, int defaultSimilarityThreshold) {
}
