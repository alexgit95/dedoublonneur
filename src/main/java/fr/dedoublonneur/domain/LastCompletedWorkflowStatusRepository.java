package fr.dedoublonneur.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface LastCompletedWorkflowStatusRepository extends JpaRepository<ProcessingResult, Long> {

    @Transactional(readOnly = true)
    @Query("select new fr.dedoublonneur.domain.LastCompletedWorkflowStatus(j.id, e.folderName, pr.processedAt) "
            + "from ProcessingResult pr join pr.job j join j.event e "
            + "where j.status = fr.dedoublonneur.domain.JobStatus.DONE "
            + "order by pr.processedAt desc")
    List<LastCompletedWorkflowStatus> findLatest();
}