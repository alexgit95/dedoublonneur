package fr.dedoublonneur.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;

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

    @AfterEach
    void cleanUp() {
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
        assertThat(workflowStateService.currentStatus())
                .extracting(WorkflowStatus::status, WorkflowStatus::jobId, WorkflowStatus::eventFolderName)
                .containsExactly(JobStatus.READY_FOR_REVIEW.name(), job.getId(), "vacances-ete-2026");

        job.setStatus(JobStatus.PROCESSING);
        jobRepository.saveAndFlush(job);
        assertThat(workflowStateService.currentStatus())
                .extracting(WorkflowStatus::status, WorkflowStatus::jobId, WorkflowStatus::eventFolderName)
                .containsExactly(JobStatus.PROCESSING.name(), job.getId(), "vacances-ete-2026");
    }

    @Test
    void currentStatusPreservesLatestCompletedWorkflowContext() {
        AnalysisJob job = workflowStateService.startAnalysis("vacances-ete-2026");
        job.setStatus(JobStatus.DONE);
        jobRepository.saveAndFlush(job);

        WorkflowStatus status = workflowStateService.currentStatus();

        assertThat(status.status()).isEqualTo(JobStatus.DONE.name());
        assertThat(status.jobId()).isEqualTo(job.getId());
        assertThat(status.eventFolderName()).isEqualTo("vacances-ete-2026");
    }

    @Test
    void rejectsSecondAnalysisWhileOneIsActive() {
        workflowStateService.startAnalysis("vacances-ete-2026");

        assertThatThrownBy(() -> workflowStateService.startAnalysis("noel-2026"))
                .isInstanceOf(WorkflowLockedException.class);
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
}
