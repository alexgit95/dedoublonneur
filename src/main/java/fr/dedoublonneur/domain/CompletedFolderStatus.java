package fr.dedoublonneur.domain;

import java.time.Instant;

public record CompletedFolderStatus(String folderName, Instant processedAt, long savedBytes) {
}
