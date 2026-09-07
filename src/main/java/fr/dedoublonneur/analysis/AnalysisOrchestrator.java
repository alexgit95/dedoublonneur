package fr.dedoublonneur.analysis;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;

/**
 * Construit l'instantane (snapshot) fige des photos JPEG d'un dossier evenement au
 * demarrage du job, puis declenche le traitement en tache de fond (design.md decision 5).
 */
@Service
public class AnalysisOrchestrator {

    private static final List<String> JPEG_EXTENSIONS = List.of(".jpg", ".jpeg");

    private final AnalysisJobRepository jobRepository;
    private final PhotoAnalysisRunner photoAnalysisRunner;

    public AnalysisOrchestrator(AnalysisJobRepository jobRepository, PhotoAnalysisRunner photoAnalysisRunner) {
        this.jobRepository = jobRepository;
        this.photoAnalysisRunner = photoAnalysisRunner;
    }

    /** Fige le snapshot de fichiers puis lance l'analyse en tache de fond. */
    public void beginAnalysis(AnalysisJob job) {
        // job est detache ici (transaction de WorkflowStateService.startAnalysis deja fermee) :
        // on relit le chemin dossier via une requete dediee plutot que job.getEvent().
        String folderPath = jobRepository.findEventFolderPathByJobId(job.getId())
                .orElseThrow(() -> new JobNotFoundException(job.getId()));
        List<String> snapshot = listJpegFiles(Path.of(folderPath));
        job.setSnapshot(snapshot);
        jobRepository.save(job);
        photoAnalysisRunner.runAnalysisAsync(job.getId());
    }

    private List<String> listJpegFiles(Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(folder)) {
            return entries.filter(Files::isRegularFile)
                    .filter(AnalysisOrchestrator::isJpeg)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de lister le dossier evenement: " + folder, e);
        }
    }

    private static boolean isJpeg(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return JPEG_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
