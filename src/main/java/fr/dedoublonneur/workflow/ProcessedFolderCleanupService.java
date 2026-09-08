package fr.dedoublonneur.workflow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.domain.ProcessingResult;
import fr.dedoublonneur.domain.ProcessingResultRepository;

@Service
public class ProcessedFolderCleanupService {

    private final AnalysisJobRepository jobRepository;
    private final EventRepository eventRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final ProcessingResultRepository processingResultRepository;
    private final ThumbnailService thumbnailService;
    private final WorkflowStateService workflowStateService;
    private final AppProperties appProperties;

    public ProcessedFolderCleanupService(AnalysisJobRepository jobRepository, EventRepository eventRepository,
            PhotoAssetRepository photoAssetRepository, ProcessingResultRepository processingResultRepository,
            ThumbnailService thumbnailService, WorkflowStateService workflowStateService,
            AppProperties appProperties) {
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.photoAssetRepository = photoAssetRepository;
        this.processingResultRepository = processingResultRepository;
        this.thumbnailService = thumbnailService;
        this.workflowStateService = workflowStateService;
        this.appProperties = appProperties;
    }

    public ProcessedFolderCleanupResponse cleanup(String folderName) {
        workflowStateService.findActiveJob().ifPresent(active -> {
            throw new CleanupNotAllowedException("Un workflow est actif sur le dossier en cours.");
        });

        Event event = eventRepository.findByFolderName(folderName)
            .orElseThrow(() -> new CleanupNotAllowedException("Dossier evenement introuvable."));
        List<AnalysisJob> jobs = jobRepository.findByEventFolderName(folderName);
        List<AnalysisJob> doneJobs = jobs.stream()
            .filter(job -> job.getStatus() == JobStatus.DONE)
            .toList();
        if (doneJobs.isEmpty()) {
            throw new CleanupNotAllowedException("Le dossier n'a aucun export termine.");
        }

        List<ProcessingResult> results = processingResultRepository.findByJobEventId(event.getId());
        Set<Path> targets = new LinkedHashSet<>();
        List<ProcessedFolderCleanupResponse.CleanupFailure> failures = new ArrayList<>();
        List<String> deletedPaths = new ArrayList<>();
        Path sourceRoot = absolute(appProperties.nas().sourcePath());
        Path outputRoot = absolute(appProperties.nas().outputPath());
        addValidatedTarget(targets, absolute(event.getFolderPath()), sourceRoot, "source", failures);
        for (ProcessingResult result : results) {
            addValidatedTarget(targets, absolute(result.getOutputFolderPath()), outputRoot, "sortie", failures);
        }
        Path thumbnailRoot = absolute(System.getProperty("java.io.tmpdir"));
        for (AnalysisJob job : jobs) {
            addValidatedTarget(targets, thumbnailService.cacheDir(job.getId()).toAbsolutePath().normalize(),
                    thumbnailRoot, "thumbnail", failures);
        }

        for (Path target : targets) {
            if (failures.stream().anyMatch(failure -> failure.path().equals(target.toString()))) {
                continue;
            }
            try {
                deleteRecursively(target);
                deletedPaths.add(target.toString());
            } catch (IOException | UncheckedIOException exception) {
                failures.add(new ProcessedFolderCleanupResponse.CleanupFailure(target.toString(), exception.getMessage()));
            }
        }

        if (failures.isEmpty()) {
            for (AnalysisJob job : jobs) {
                photoAssetRepository.deleteByJobId(job.getId());
                processingResultRepository.deleteByJobId(job.getId());
            }
            jobRepository.deleteAll(jobs);
            eventRepository.delete(event);
        }
        return new ProcessedFolderCleanupResponse(failures.isEmpty(), deletedPaths, failures);
    }

    private void addValidatedTarget(Set<Path> targets, Path target, Path root, String kind,
            List<ProcessedFolderCleanupResponse.CleanupFailure> failures) {
        if (target.equals(root) || !target.startsWith(root) || target.equals(Path.of("/"))) {
            failures.add(new ProcessedFolderCleanupResponse.CleanupFailure(target.toString(),
                    "Chemin " + kind + " hors de la racine autorisee."));
            return;
        }
        if (Files.isSymbolicLink(target)) {
            failures.add(new ProcessedFolderCleanupResponse.CleanupFailure(target.toString(),
                    "Les liens symboliques ne sont pas nettoyables."));
            return;
        }
        targets.add(target);
    }

    private static Path absolute(String path) {
        return Path.of(path).toAbsolutePath().normalize();
    }

    private static void deleteRecursively(Path target) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        try (var paths = Files.walk(target)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        }
    }
}
