package fr.dedoublonneur.processing;

public record ProcessingPreviewResponse(int keptPhotoCount, int deletedPhotoCount, int videoCount,
        long sourceBytes, long keptBytes, long estimatedSavedBytes) {
}
