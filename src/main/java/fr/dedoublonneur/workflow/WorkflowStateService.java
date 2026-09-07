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

    public WorkflowStateService(AnalysisJobRepository jobRepository, EventRepository eventRepository,
            AppProperties appProperties) {
        this.jobRepository = jobRepository;
        this.eventRepository = eventRepository;
        this.appProperties = appProperties;
    }

    @Transactional(readOnly = true)
    public Optional<AnalysisJob> findActiveJob() {
        return jobRepository.findFirstByStatusIn(ACTIVE_STATUSES);
    }

    @Transactional(readOnly = true)
    public WorkflowStatus currentStatus() {
        return findActiveJob()
                .map(WorkflowStatus::of)
                .or(() -> jobRepository.findFirstByStatusOrderByIdDesc(JobStatus.DONE).map(WorkflowStatus::of))
                .orElseGet(WorkflowStatus::idle);
    }

    /**
     * Demarre l'analyse d'un nouveau dossier evenement. Rejette la demande (verrou)
     * si un autre workflow est deja actif.
     */
    @Transactional
    public AnalysisJob startAnalysis(String folderName) {
        findActiveJob().ifPresent(active -> {
            throw new WorkflowLockedException(active.getEvent().getFolderName());
        });

        Event event = eventRepository.findByFolderName(folderName)
                .orElseGet(() -> eventRepository.save(new Event(folderName, resolveFolderPath(folderName))));

        AnalysisJob job = new AnalysisJob(event, 0, appProperties.analysis().defaultSimilarityThreshold());
        return jobRepository.save(job);
    }

    /**
     * Annule/reinitialise manuellement le workflow actif, liberant le verrou global.
     */
    @Transactional
    public WorkflowStatus cancelActiveWorkflow() {
        AnalysisJob job = findActiveJob().orElseThrow(NoActiveWorkflowException::new);
        job.setStatus(JobStatus.CANCELLED);
        job.setFinishedAt(Instant.now());
        return WorkflowStatus.idle();
    }

    private String resolveFolderPath(String folderName) {
        return Path.of(appProperties.nas().sourcePath(), folderName).toString();
    }
}
