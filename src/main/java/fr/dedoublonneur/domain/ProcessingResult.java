package fr.dedoublonneur.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Recapitulatif produit a l'issue du traitement (copie vers le dossier de sortie) d'un {@link AnalysisJob}.
 */
@Entity
@Table(name = "processing_result")
public class ProcessingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false, unique = true)
    private AnalysisJob job;

    @Column(nullable = false)
    private int keptCount;

    @Column(nullable = false)
    private int deletedCount;

    @Column(nullable = false)
    private int videoCount;

    @Column(nullable = false)
    private long spaceBeforeBytes;

    @Column(nullable = false)
    private long spaceAfterBytes;

    @Column(nullable = false, length = 1024)
    private String outputFolderPath;

    @Column(nullable = false)
    private Instant processedAt;

    protected ProcessingResult() {
        // JPA
    }

    public ProcessingResult(AnalysisJob job, int keptCount, int deletedCount, int videoCount,
            long spaceBeforeBytes, long spaceAfterBytes, String outputFolderPath) {
        this.job = job;
        this.keptCount = keptCount;
        this.deletedCount = deletedCount;
        this.videoCount = videoCount;
        this.spaceBeforeBytes = spaceBeforeBytes;
        this.spaceAfterBytes = spaceAfterBytes;
        this.outputFolderPath = outputFolderPath;
        this.processedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AnalysisJob getJob() {
        return job;
    }

    public int getKeptCount() {
        return keptCount;
    }

    public int getDeletedCount() {
        return deletedCount;
    }

    public int getVideoCount() {
        return videoCount;
    }

    public long getSpaceBeforeBytes() {
        return spaceBeforeBytes;
    }

    public long getSpaceAfterBytes() {
        return spaceAfterBytes;
    }

    public String getOutputFolderPath() {
        return outputFolderPath;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
