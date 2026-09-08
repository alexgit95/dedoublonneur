package fr.dedoublonneur.workflow;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.LastCompletedWorkflowStatus;
import fr.dedoublonneur.domain.LastCompletedWorkflowStatusRepository;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.analysis.DuplicateClusterService;
import fr.dedoublonneur.analysis.PhotoAnalysisRunner;
import fr.dedoublonneur.analysis.ThumbnailService;
import java.util.concurrent.TimeUnit;

/**
 * Point d'entree unique pour la contrainte "un seul workflow actif a la fois"
 * (design.md decision 1/9) : verifie/detient le verrou et pilote la machine a
 * etats IDLE -> ANALYZING -> READY_FOR_REVIEW -> PROCESSING -> DONE.
 */
@Service
public class WorkflowStateService {

    private static final List<JobStatus> ACTIVE_STATUSES = List.of(
            JobStatus.ANALYZING, JobStatus.READY_FOR_REVIEW, JobStatus.PROCESSING);

    private final AnalysisJobRepository jobRepository;
    private final EventRepository eventRepository;
    private final AppProperties appProperties;
    private final PhotoAssetRepository photoAssetRepository;
    private final PhotoAnalysisRunner photoAnalysisRunner;
    private final ThumbnailService thumbnailService;
    private final DuplicateClusterService duplicateClusterService;
    private final LastCompletedWorkflowStatusRepository lastCompletedRepository;

    public WorkflowStateService(AnalysisJobRepository jobRepository, EventRepository eventRepository,
            AppProperties appProperties, PhotoAssetRepository photoAssetRepository,
            PhotoAnalysisRunner photoAnalysisRunner, ThumbnailService thumbnailService,
            DuplicateClusterService duplicateClusterService,
            LastCompletedWorkflowStatusRepository lastCompletedRepository) {
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.appProperties = appProperties;
        this.photoAssetRepository = photoAssetRepository;
        this.photoAnalysisRunner = photoAnalysisRunner;
        this.thumbnailService = thumbnailService;
        this.duplicateClusterService = duplicateClusterService;
        this.lastCompletedRepository = lastCompletedRepository;
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisJob> findActiveJob() {
        return jobRepository.findFirstByStatusIn(ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public WorkflowStatus currentStatus() {
        LastCompletedWorkflowStatus lastCompleted = lastCompletedRepository.findLatest().stream().findFirst().orElse(null);
        return findActiveJob()
            .map(job -> WorkflowStatus.of(job, lastCompleted))
            .orElseGet(() -> WorkflowStatus.idle(lastCompleted));
    }

    /**
     * Demarre l'analyse d'un nouveau dossier evenement. Rejette la demande (verrou)
     * si un autre workflow est deja actif.
     */
    public synchronized AnalysisJob startAnalysis(String folderName) {
        findActiveJob().ifPresent(active -> {
            String activeFolderName = jobRepository.findEventFolderNameByJobId(active.getId()).orElse("inconnu");
            throw new WorkflowLockedException(activeFolderName);
        });

        Event event = eventRepository.findByFolderName(folderName)
                .orElseGet(() -> eventRepository.save(new Event(folderName, resolveFolderPath(folderName))));

        AnalysisJob job = new AnalysisJob(event, 0, appProperties.analysis().defaultSimilarityThreshold());
        return jobRepository.save(job);
    }

    /**
     * Annule/reinitialise manuellement le workflow actif, liberant le verrou global.
     */
    public synchronized WorkflowStatus cancelActiveWorkflow() {
        AnalysisJob job = findActiveJob().orElseThrow(NoActiveWorkflowException::new);
        if (job.getStatus() == JobStatus.PROCESSING) {
            throw new WorkflowResetNotAllowedException();
        }
        photoAnalysisRunner.requestCancellation(job.getId());
        photoAnalysisRunner.awaitCompletion(job.getId(), 10, TimeUnit.SECONDS);
        photoAssetRepository.deleteByJobId(job.getId());
        thumbnailService.purge(job.getId());
        duplicateClusterService.evictJob(job.getId());
        job.setStatus(JobStatus.CANCELLED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);
        return currentStatus();
    }

    private String resolveFolderPath(String folderName) {
        return Path.of(appProperties.nas().sourcePath(), folderName).toString();
    }
}
