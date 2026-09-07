package fr.dedoublonneur.web;

import java.time.Instant;

/** Recapitulatif final : photos conservees/supprimees, espace disque avant/apres. */
public record ProcessingResultResponse(int keptCount, int deletedCount, int videoCount, long spaceBeforeBytes,
        long spaceAfterBytes, String outputFolderPath, Instant processedAt) {

    public static ProcessingResultResponse from(fr.dedoublonneur.domain.ProcessingResult result) {
        return new ProcessingResultResponse(result.getKeptCount(), result.getDeletedCount(), result.getVideoCount(),
                result.getSpaceBeforeBytes(), result.getSpaceAfterBytes(), result.getOutputFolderPath(),
                result.getProcessedAt());
    }
}
