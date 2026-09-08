package fr.dedoublonneur.domain;

import java.time.Instant;

public record LastCompletedWorkflowStatus(Long jobId, String folderName, Instant processedAt) {
}