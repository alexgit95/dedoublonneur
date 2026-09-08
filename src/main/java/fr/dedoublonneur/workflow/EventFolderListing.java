package fr.dedoublonneur.workflow;

import java.time.Instant;

public record EventFolderListing(String name, boolean processed, Instant processedAt) {
}
