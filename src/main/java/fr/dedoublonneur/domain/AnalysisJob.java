package fr.dedoublonneur.domain;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Job d'analyse/traitement d'un {@link Event}. Un seul job non termine
 * (statut different de DONE/CANCELLED) doit exister a la fois dans l'application
 * (verrou applique au niveau service, cf. design.md decision 1).
 */
@Entity
@Table(name = "analysis_job")
public class AnalysisJob {

    private static final String SNAPSHOT_SEPARATOR = "\n";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus status;

    @Column(nullable = false)
    private int snapshotSize;

    /**
    * Liste figee (chemins relatifs, separes par des sauts de ligne) des fichiers photo JPEG ou PNG
     * a analyser, constituee au demarrage du job : les fichiers ajoutes ensuite dans le
     * dossier source sont ignores (cf. photo-analysis spec, "Snapshot of files").
     */
    @Lob
    @Column(name = "snapshot_file_list")
    private String snapshotFileList;

    @Column(nullable = false)
    private int lastProcessedCount;

    @Column(nullable = false)
    private int similarityThresholdDefault;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    protected AnalysisJob() {
        // JPA
    }

    public AnalysisJob(Event event, int snapshotSize, int similarityThresholdDefault) {
        this.event = event;
        this.status = JobStatus.ANALYZING;
        this.snapshotSize = snapshotSize;
        this.lastProcessedCount = 0;
        this.similarityThresholdDefault = similarityThresholdDefault;
        this.startedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getSnapshotSize() {
        return snapshotSize;
    }

    /** Fige la liste des fichiers a analyser (appele une seule fois, au demarrage). */
    public void setSnapshot(List<String> relativePaths) {
        this.snapshotSize = relativePaths.size();
        this.snapshotFileList = String.join(SNAPSHOT_SEPARATOR, relativePaths);
    }

    public List<String> getSnapshotFiles() {
        if (snapshotFileList == null || snapshotFileList.isBlank()) {
            return List.of();
        }
        return List.of(snapshotFileList.split(SNAPSHOT_SEPARATOR));
    }

    public int getLastProcessedCount() {
        return lastProcessedCount;
    }

    public void setLastProcessedCount(int lastProcessedCount) {
        this.lastProcessedCount = lastProcessedCount;
    }

    public int getSimilarityThresholdDefault() {
        return similarityThresholdDefault;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
