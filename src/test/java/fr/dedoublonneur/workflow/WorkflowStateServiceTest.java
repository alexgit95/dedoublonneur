package fr.dedoublonneur.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.domain.ProcessingResult;
import fr.dedoublonneur.domain.ProcessingResultRepository;

/**
 * Couvre le verrou "un seul workflow actif a la fois" et les transitions d'etat
 * (event-folder-workflow: rejet du verrou, transitions, annulation/reset).
 *
 * Pas de @Transactional ici : le generateur d'id hi/lo (GenerationType.AUTO) utilise
 * sa propre transaction imbriquee, qui se bloque sur SQLite (mono-ecrivain) si une
 * transaction de test englobante reste ouverte. Nettoyage explicite entre tests.
 */
@SpringBootTest
@ActiveProfiles("local")
class WorkflowStateServiceTest {

    @Autowired
    private WorkflowStateService workflowStateService;

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private ProcessingResultRepository processingResultRepository;

    @AfterEach
    void cleanUp() {
        processingResultRepository.deleteAll();
        photoAssetRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void idleWhenNoJobHasEverBeenStarted() {
        assertThat(workflowStateService.currentStatus().status()).isEqualTo(WorkflowStatus.IDLE);
    }

    @Test
    void startAnalysisTransitionsToAnalyzing() {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");

        assertThat(job.getStatus()).isEqualTo(JobStatus.ANALYZING);
        WorkflowStatus status = workflowStateService.currentStatus();
        assertThat(status.status()).isEqualTo(JobStatus.ANALYZING.name());
        assertThat(status.jobId()).isEqualTo(job.getId());
        assertThat(status.eventFolderName()).isEqualTo("vacances-ete-2026");
    }

    @Test
    void currentStatusPreservesActiveReviewAndProcessingContext() {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");

        job.setStatus(JobStatus.READY_FOR_REVIEW);
        jobRepository.saveAndFlush(job);
        WorkflowStatus reviewStatus = workflowStateService.currentStatus();
        assertThat(reviewStatus.status()).isEqualTo(JobStatus.READY_FOR_REVIEW.name());
        assertThat(reviewStatus.jobId()).isEqualTo(job.getId());
        assertThat(reviewStatus.eventFolderName()).isEqualTo("vacances-ete-2026");

        job.setStatus(JobStatus.PROCESSING);
        jobRepository.saveAndFlush(job);
        WorkflowStatus processingStatus = workflowStateService.currentStatus();
        assertThat(processingStatus.status()).isEqualTo(JobStatus.PROCESSING.name());
        assertThat(processingStatus.jobId()).isEqualTo(job.getId());
        assertThat(processingStatus.eventFolderName()).isEqualTo("vacances-ete-2026");
    }

    @Test
    void currentStatusPreservesLatestCompletedWorkflowContext() {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");
        job.setStatus(JobStatus.DONE);
        jobRepository.saveAndFlush(job);
        processingResultRepository.save(new ProcessingResult(job, 1, 0, 0, 100L, 100L, "sortie"));

        WorkflowStatus status = workflowStateService.currentStatus();

        assertThat(status.status()).isEqualTo(WorkflowStatus.IDLE);
        assertThat(status.jobId()).isNull();
        assertThat(status.eventFolderName()).isNull();
        assertThat(status.lastCompletedJobId()).isEqualTo(job.getId());
        assertThat(status.lastCompletedFolderName()).isEqualTo("vacances-ete-2026");
    }

    @Test
    void rejectsSecondAnalysisWhileOneIsActive() {
        workflowStateService.startAnalysis("vacances-ete-2026");

        assertThatThrownBy(() -> workflowStateService.startAnalysis("noel-2026"))
                .isInstanceOf(WorkflowLockedException.class);
    }

    @Test
    void serializesConcurrentAnalysisStarts() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<AnalysisJob> startFirst = () -> workflowStateService.startAnalysis("vacances-ete-2026");
            Callable<AnalysisJob> startSecond = () -> workflowStateService.startAnalysis("noel-2026");

            Future<AnalysisJob> first = executor.submit(startFirst);
            Future<AnalysisJob> second = executor.submit(startSecond);

            int successfulStarts = 0;
            int rejectedStarts = 0;
            for (Future<AnalysisJob> result : List.of(first, second)) {
                try {
                    result.get();
                    successfulStarts++;
                } catch (ExecutionException exception) {
                    if (exception.getCause() instanceof WorkflowLockedException) {
                        rejectedStarts++;
                    } else {
                        throw exception;
                    }
                }
            }

            assertThat(successfulStarts).isEqualTo(1);
            assertThat(rejectedStarts).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cancelReleasesTheLockBackToIdle() {
        workflowStateService.startAnalysis("vacances-ete-2026");

        WorkflowStatus statusAfterCancel = workflowStateService.cancelActiveWorkflow();

        assertThat(statusAfterCancel.status()).isEqualTo(WorkflowStatus.IDLE);
        assertThat(workflowStateService.currentStatus().status()).isEqualTo(WorkflowStatus.IDLE);
        // Un nouveau dossier peut etre analyse une fois le verrou libere.
        AnalysisJob newJob = workflowStateService.startAnalysis("noel-2026");
        assertThat(newJob.getStatus()).isEqualTo(JobStatus.ANALYZING);
    }

    @Test
    void cancelWithoutActiveWorkflowThrows() {
        assertThatThrownBy(() -> workflowStateService.cancelActiveWorkflow())
                .isInstanceOf(NoActiveWorkflowException.class);
    }

    @Test
    void resetDuringReviewDeletesAnalysisAndThumbnailsButKeepsCancelledJob() throws Exception {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");
        job.setStatus(JobStatus.READY_FOR_REVIEW);
        jobRepository.saveAndFlush(job);
        photoAssetRepository.save(new PhotoAsset(job, "photo.jpg", 123L, 50.0, 42L, true));
        Path thumbnail = thumbnailService.cacheDir(job.getId()).resolve("1.jpg");
        Files.createDirectories(thumbnail.getParent());
        Files.writeString(thumbnail, "thumbnail");

        WorkflowStatus resetStatus = workflowStateService.cancelActiveWorkflow();

        assertThat(resetStatus.status()).isEqualTo(WorkflowStatus.IDLE);
        assertThat(photoAssetRepository.countByJobId(job.getId())).isZero();
        assertThat(Files.exists(thumbnail)).isFalse();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(workflowStateService.startAnalysis("noel-2026").getStatus()).isEqualTo(JobStatus.ANALYZING);
    }

    @Test
    void resetIsRejectedDuringProcessing() {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");
        job.setStatus(JobStatus.PROCESSING);
        jobRepository.saveAndFlush(job);

        assertThatThrownBy(() -> workflowStateService.cancelActiveWorkflow())
                .isInstanceOf(WorkflowResetNotAllowedException.class);
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PROCESSING);
    }

    @Test
    void completedExportIsReportedButReviewIsNot() {
        AnalysisJob exported = workflowStateService.startAnalysis("exporte");
        exported.setStatus(JobStatus.DONE);
        jobRepository.saveAndFlush(exported);
        processingResultRepository.save(new ProcessingResult(exported, 1, 1, 0, 100L, 50L, "sortie"));

        AnalysisJob inReview = workflowStateService.startAnalysis("en-revue");
        inReview.setStatus(JobStatus.READY_FOR_REVIEW);
        jobRepository.saveAndFlush(inReview);

        assertThat(jobRepository.findCompletedFolderStatuses())
                .extracting(status -> status.folderName())
                .containsExactly("exporte");
    }

    @Test
    void completedJobDoesNotBlockReanalysisOfTheSameFolder() {
        AnalysisJob completed = workflowStateService.startAnalysis("vacances-ete-2026");
        completed.setStatus(JobStatus.DONE);
        jobRepository.saveAndFlush(completed);

        AnalysisJob restarted = workflowStateService.startAnalysis("vacances-ete-2026");

        assertThat(restarted.getStatus()).isEqualTo(JobStatus.ANALYZING);
        assertThat(restarted.getId()).isNotEqualTo(completed.getId());
    }
}
