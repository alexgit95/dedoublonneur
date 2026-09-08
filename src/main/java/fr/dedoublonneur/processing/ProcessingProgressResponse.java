package fr.dedoublonneur.processing;

public record ProcessingProgressResponse(String status, int totalItems, int processedItems,
        int progressPercent, long bytesCopied) {
}
