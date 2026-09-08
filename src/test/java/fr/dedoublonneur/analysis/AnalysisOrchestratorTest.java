package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalysisOrchestratorTest {

    @Test
    void photoFilterAcceptsJpegAndPngCaseInsensitively() {
        assertThat(AnalysisOrchestrator.isPhoto(Path.of("INITIALE.jpg"))).isTrue();
        assertThat(AnalysisOrchestrator.isPhoto(Path.of("INITIALE.JPEG"))).isTrue();
        assertThat(AnalysisOrchestrator.isPhoto(Path.of("INITIALE.PNG"))).isTrue();
        assertThat(AnalysisOrchestrator.isPhoto(Path.of("notes.txt"))).isFalse();
    }

    @Test
    void snapshotIncludesJpegAndPngFilesAndIgnoresOtherFiles(@TempDir Path tempDir) throws IOException {
        Files.createFile(tempDir.resolve("photo.JPG"));
        Files.createFile(tempDir.resolve("photo.png"));
        Files.createFile(tempDir.resolve("notes.txt"));

        List<String> snapshot = AnalysisOrchestrator.listPhotoFiles(tempDir);

        assertThat(snapshot).containsExactly("photo.JPG", "photo.png");
    }
}