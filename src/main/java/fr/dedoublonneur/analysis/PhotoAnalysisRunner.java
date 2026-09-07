package fr.dedoublonneur.analysis;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final AnalysisJobRepository jobRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final ImageAnalysisService imageAnalysisService;
    private final AppProperties appProperties;

    public PhotoAnalysisRunner(AnalysisJobRepository jobRepository, PhotoAssetRepository photoAssetRepository,
            ImageAnalysisService imageAnalysisService, AppProperties appProperties) {
        this.jobRepository = jobRepository;
        this.photoAssetRepository = photoAssetRepository;
        this.imageAnalysisService = imageAnalysisService;
        this.appProperties = appProperties;
    }

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    public void runAnalysisAsync(Long jobId) {
        runAnalysis(jobId);
    }

    public boolean isRunning(Long jobId) {
        return runningJobIds.contains(jobId);
    }

    /**
     * Traitement synchrone du snapshot (extrait pour etre appelable directement dans les tests
     * sans passer par le proxy @Async). Ignore les photos deja persistees (reprise).
     */
    public void runAnalysis(Long jobId) {
        if (!runningJobIds.add(jobId)) {
            return;
        }
        try {
            AnalysisJob job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
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
            job.setStatus(JobStatus.READY_FOR_REVIEW);
            jobRepository.save(job);
        } finally {
            runningJobIds.remove(jobId);
        }
    }

    private void analyzeOnePhoto(AnalysisJob job, Path eventDir, String relativePath) {
        Path photoPath = eventDir.resolve(relativePath);
        try {
            PhotoAnalysisResult result = imageAnalysisService.analyze(photoPath);
            long fileSize = Files.size(photoPath);
            boolean blurred = result.blurScore() < appProperties.analysis().blurThreshold();
            photoAssetRepository.save(
                    new PhotoAsset(job, relativePath, fileSize, result.blurScore(), result.pHash(), blurred));
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
}
