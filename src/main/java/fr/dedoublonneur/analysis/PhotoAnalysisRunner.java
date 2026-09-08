package fr.dedoublonneur.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.config.AsyncConfig;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.workflow.WorkflowResetException;

/**
 * Traite le snapshot fige d'un {@link AnalysisJob} : calcule blur score + pHash par photo,
 * persiste un checkpoint tous les ~N photos, et permet une reprise manuelle qui ignore les
 * photos deja analysees (design.md decision 5).
 */
@Service
public class PhotoAnalysisRunner {

    private static final Logger log = LoggerFactory.getLogger(PhotoAnalysisRunner.class);

    /** Jobs en cours de traitement dans ce process (perdu au redemarrage -> reprise manuelle requise). */
    private final Set<Long> runningJobIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> cancellationRequested = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> completionSignals = new ConcurrentHashMap<>();

    private final AnalysisJobRepository jobRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final ImageAnalysisService imageAnalysisService;
    private final ThumbnailService thumbnailService;
    private final AppProperties appProperties;

    public PhotoAnalysisRunner(AnalysisJobRepository jobRepository, PhotoAssetRepository photoAssetRepository,
            ImageAnalysisService imageAnalysisService, ThumbnailService thumbnailService, AppProperties appProperties) {
        this.jobRepository = jobRepository;
        this.photoAssetRepository = photoAssetRepository;
        this.imageAnalysisService = imageAnalysisService;
        this.thumbnailService = thumbnailService;
        this.appProperties = appProperties;
    }

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    public void runAnalysisAsync(Long jobId) {
        runAnalysis(jobId);
    }

    public boolean isRunning(Long jobId) {
        return runningJobIds.contains(jobId);
    }

    public void requestCancellation(Long jobId) {
        if (isRunning(jobId)) {
            cancellationRequested.add(jobId);
        }
    }

    public void awaitCompletion(Long jobId, long timeout, TimeUnit unit) {
        CompletableFuture<Void> completion = completionSignals.get(jobId);
        if (completion == null) {
            return;
        }
        try {
            completion.get(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkflowResetException("Arret de l'analyse interrompu.", e);
        } catch (TimeoutException e) {
            throw new WorkflowResetException("L'analyse ne s'est pas arretee dans le delai imparti.", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new WorkflowResetException("Impossible d'arreter l'analyse.", e.getCause());
        }
    }

    /**
     * Traitement synchrone du snapshot (extrait pour etre appelable directement dans les tests
     * sans passer par le proxy @Async). Ignore les photos deja persistees (reprise).
     */
    public void runAnalysis(Long jobId) {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        completionSignals.put(jobId, completion);
        if (!runningJobIds.add(jobId)) {
            completionSignals.remove(jobId, completion);
            return;
        }
        try {
            AnalysisJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
            if (job.getStatus() != JobStatus.ANALYZING) {
                return;
            }
            // Requete dediee (pas job.getEvent()) : le job est un objet detache entre chaque
            // courte transaction de cette methode, la lazy association event ne peut pas etre
            // chargee a la demande (pas de session ouverte en dehors des appels repository).
            String folderPath = jobRepository.findEventFolderPathByJobId(jobId)
                    .orElseThrow(() -> new JobNotFoundException(jobId));
            Path eventDir = Path.of(folderPath);
            List<String> snapshot = job.getSnapshotFiles();

            Set<String> alreadyAnalyzed = photoAssetRepository.findByJobId(jobId).stream()
                    .map(PhotoAsset::getRelativePath)
                    .collect(Collectors.toSet());

            int processedSinceCheckpoint = 0;
            int checkpointBatchSize = appProperties.analysis().checkpointBatchSize();

            for (String relativePath : snapshot) {
                if (cancellationRequested.contains(jobId)) {
                    return;
                }
                if (alreadyAnalyzed.contains(relativePath)) {
                    continue;
                }
                analyzeOnePhoto(job, eventDir, relativePath);
                processedSinceCheckpoint++;
                if (processedSinceCheckpoint >= checkpointBatchSize) {
                    checkpoint(job);
                    processedSinceCheckpoint = 0;
                }
            }

            checkpoint(job);
            if (cancellationRequested.contains(jobId)) {
                return;
            }
            job.setStatus(JobStatus.READY_FOR_REVIEW);
            jobRepository.save(job);
        } finally {
            runningJobIds.remove(jobId);
            cancellationRequested.remove(jobId);
            completionSignals.remove(jobId);
            completion.complete(null);
        }
    }

    private void analyzeOnePhoto(AnalysisJob job, Path eventDir, String relativePath) {
        Path photoPath = eventDir.resolve(relativePath);
        try {
            PhotoAnalysisResult result = imageAnalysisService.analyze(photoPath);
            long fileSize = Files.size(photoPath);
            boolean blurred = result.blurScore() < appProperties.analysis().blurThreshold();
            PhotoAsset photo = photoAssetRepository.save(
                    new PhotoAsset(job, relativePath, fileSize, result.blurScore(), result.pHash(), blurred));
            generateThumbnail(job.getId(), photo.getId(), photoPath);
        } catch (IOException e) {
            // Une photo illisible ne doit pas bloquer l'analyse des autres : on la journalise et on continue.
            log.warn("Photo ignoree (illisible): {}", photoPath, e);
        }
    }

    private void checkpoint(AnalysisJob job) {
        long analyzed = photoAssetRepository.countByJobId(job.getId());
        job.setLastProcessedCount((int) analyzed);
        jobRepository.save(job);
    }

    /** Echec non bloquant : la vignette peut toujours etre regeneree a la demande (cf. ThumbnailController). */
    private void generateThumbnail(Long jobId, Long photoId, Path photoPath) {
        try {
            thumbnailService.generate(jobId, photoId, photoPath);
        } catch (IOException e) {
            log.warn("Vignette non generee pour la photo {}", photoPath, e);
        }
    }
}
