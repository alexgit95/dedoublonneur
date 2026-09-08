package fr.dedoublonneur.workflow;

import java.util.List;

public record ProcessedFolderCleanupResponse(boolean complete, List<String> deletedPaths,
        List<CleanupFailure> failures) {

    public record CleanupFailure(String path, String reason) {
    }
}
