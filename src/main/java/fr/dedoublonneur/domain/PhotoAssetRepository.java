package fr.dedoublonneur.domain;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PhotoAssetRepository extends JpaRepository<PhotoAsset, Long> {

    @Transactional(readOnly = true)
    Page<PhotoAsset> findByJobIdAndBlurredTrue(Long jobId, Pageable pageable);

    @Transactional(readOnly = true)
    List<PhotoAsset> findByJobId(Long jobId);

    @Transactional(readOnly = true)
    long countByJobId(Long jobId);
}
