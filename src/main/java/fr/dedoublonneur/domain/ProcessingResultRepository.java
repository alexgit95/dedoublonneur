package fr.dedoublonneur.domain;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ProcessingResultRepository extends JpaRepository<ProcessingResult, Long> {

    @Transactional(readOnly = true)
    Optional<ProcessingResult> findByJobId(Long jobId);

    @Transactional(readOnly = true)
    List<ProcessingResult> findByJobEventId(Long eventId);

    @Transactional
    long deleteByJobId(Long jobId);
}
