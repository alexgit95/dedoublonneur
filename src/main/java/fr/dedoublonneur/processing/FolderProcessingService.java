package fr.dedoublonneur.processing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import fr.dedoublonneur.analysis.DuplicateClusterService;
import fr.dedoublonneur.analysis.JobNotFoundException;
import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.config.AsyncConfig;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.domain.ProcessingResult;
import fr.dedoublonneur.domain.ProcessingResultRepository;

/**
 * Copie les photos conservees (et toutes les videos) vers le dossier de sortie, sans
 * jamais modifier le dossier source, puis produit le recapitulatif (folder-processing spec).
 */
@Service
public class FolderProcessingService {

    private static final Logger log = LoggerFactory.getLogger(FolderProcessingService.class);

    private static final List<String> VIDEO_EXTENSIONS = List.of(
            ".mp4", ".mov", ".avi", ".mkv", ".m4v", ".3gp", ".wmv");

    private final AnalysisJobRepository jobRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final ProcessingResultRepository processingResultRepository;
    private final DuplicateClusterService duplicateClusterService;
    private final ThumbnailService thumbnailService;
    private final AppProperties appProperties;
    private final ProcessingProgressRegistry progressRegistry;

    public FolderProcessingService(AnalysisJobRepository jobRepository, PhotoAssetRepository photoAssetRepository,
            ProcessingResultRepository processingResultRepository, DuplicateClusterService duplicateClusterService,
            ThumbnailService thumbnailService, AppProperties appProperties,
            ProcessingProgressRegistry progressRegistry) {
        this.jobRepository = jobRepository;
        this.photoAssetRepository = photoAssetRepository;
        this.processingResultRepository = processingResultRepository;
        this.duplicateClusterService = duplicateClusterService;
        this.thumbnailService = thumbnailService;
        this.appProperties = appProperties;
        this.progressRegistry = progressRegistry;
    }

    /** Valide la demande, verrouille le job en PROCESSING et lance la copie en tache de fond. */
    public void startProcessing(Long jobId, String outputFolderName) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        if (job.getStatus() != JobStatus.READY_FOR_REVIEW) {
            throw new ProcessingNotAllowedException(
                    "Le job " + jobId + " n'est pas en revue (statut actuel: " + job.getStatus() + ").");
        }

        String sourceFolderPath = jobRepository.findEventFolderPathByJobId(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Path outputDir = Path.of(appProperties.nas().outputPath(), outputFolderName);
        if (Files.exists(outputDir)) {
            throw new OutputFolderAlreadyExistsException(outputFolderName);
        }

        job.setStatus(JobStatus.PROCESSING);
        jobRepository.save(job);
        progressRegistry.start(jobId, countProcessingItems(jobId, sourceFolderPath));

        processFolderAsync(jobId, sourceFolderPath, outputDir.toString());
    }

    public ProcessingPreviewResponse preview(Long jobId) {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        if (job.getStatus() != JobStatus.READY_FOR_REVIEW) {
            throw new ProcessingNotAllowedException(
                    "Le job " + jobId + " n'est pas en revue (statut actuel: " + job.getStatus() + ").");
        }
        String sourceFolderPath = jobRepository.findEventFolderPathByJobId(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));
        Path sourceDir = Path.of(sourceFolderPath);
        long sourceBytes = 0;
        long keptBytes = 0;
        long savedBytes = 0;
        int kept = 0;
        int deleted = 0;
        try {
            for (PhotoAsset photo : photoAssetRepository.findByJobId(jobId)) {
                long size = Files.exists(sourceDir.resolve(photo.getRelativePath()))
                        ? Files.size(sourceDir.resolve(photo.getRelativePath())) : photo.getFileSize();
                sourceBytes += size;
                if (photo.isMarkedForDeletion()) {
                    deleted++;
                    savedBytes += size;
                } else {
                    kept++;
                    keptBytes += size;
                }
            }
            int videoCount = listVideoFiles(sourceDir).size();
            for (String video : listVideoFiles(sourceDir)) {
                sourceBytes += Files.size(sourceDir.resolve(video));
            }
            return new ProcessingPreviewResponse(kept, deleted, videoCount, sourceBytes, keptBytes, savedBytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ProcessingProgressResponse progress(Long jobId) {
        return progressRegistry.get(jobId);
    }

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    public void processFolderAsync(Long jobId, String sourceFolderPath, String outputFolderPath) {
        try {
            processFolder(jobId, sourceFolderPath, outputFolderPath);
        } catch (Exception e) {
            log.error("Echec du traitement du job {}", jobId, e);
            jobRepository.findById(jobId).ifPresent(job -> {
                job.setStatus(JobStatus.CANCELLED);
                job.setFinishedAt(Instant.now());
                jobRepository.save(job);
            });
            progressRegistry.cancel(jobId);
        }
    }

    /** Traitement synchrone (extrait pour etre appelable directement dans les tests). */
    public void processFolder(Long jobId, String sourceFolderPath, String outputFolderPath) throws IOException {
        AnalysisJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        Path sourceDir = Path.of(sourceFolderPath);
        Path outputDir = Path.of(outputFolderPath);
        if (progressRegistry.get(jobId).totalItems() == 0) {
            progressRegistry.start(jobId, countProcessingItems(jobId, sourceFolderPath));
        }
        Files.createDirectories(outputDir);

        long spaceBefore = 0;
        long spaceAfter = 0;
        int kept = 0;
        int deleted = 0;

        for (PhotoAsset photo : photoAssetRepository.findByJobId(jobId)) {
            Path source = sourceDir.resolve(photo.getRelativePath());
            long size = Files.exists(source) ? Files.size(source) : photo.getFileSize();
            spaceBefore += size;
            if (photo.isMarkedForDeletion()) {
                deleted++;
                progressRegistry.itemProcessed(jobId, 0);
                continue;
            }
            copyPreservingMetadata(source, outputDir.resolve(photo.getRelativePath()));
            spaceAfter += size;
            kept++;
            progressRegistry.itemProcessed(jobId, size);
        }

        int videoCount = 0;
        for (String videoFile : listVideoFiles(sourceDir)) {
            Path source = sourceDir.resolve(videoFile);
            long size = Files.size(source);
            spaceBefore += size;
            copyPreservingMetadata(source, outputDir.resolve(videoFile));
            spaceAfter += size;
            videoCount++;
            progressRegistry.itemProcessed(jobId, size);
        }

        processingResultRepository.save(
                new ProcessingResult(job, kept, deleted, videoCount, spaceBefore, spaceAfter, outputFolderPath));

        job.setStatus(JobStatus.DONE);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
        progressRegistry.complete(jobId);

        thumbnailService.purge(jobId);
        duplicateClusterService.evictJob(jobId);
    }

    /** Copie brute d'octets avec conservation des metadonnees (EXIF/GPS/dates) - jamais de reecriture image. */
    private void copyPreservingMetadata(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    private List<String> listVideoFiles(Path sourceDir) throws IOException {
        if (!Files.isDirectory(sourceDir)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.list(sourceDir)) {
            return entries.filter(Files::isRegularFile)
                    .filter(FolderProcessingService::isVideo)
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    private int countProcessingItems(Long jobId, String sourceFolderPath) {
        try {
            return photoAssetRepository.findByJobId(jobId).size()
                    + listVideoFiles(Path.of(sourceFolderPath)).size();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isVideo(Path path) {
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return VIDEO_EXTENSIONS.stream().anyMatch(lower::endsWith);
    }
}
