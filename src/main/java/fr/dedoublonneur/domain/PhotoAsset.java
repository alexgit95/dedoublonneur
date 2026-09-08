package fr.dedoublonneur.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Photo JPEG ou PNG d'un {@link AnalysisJob} avec son score de nettete et son hash perceptuel.
 * Les groupes de doublons ne sont jamais persistes : seul le pHash brut l'est,
 * le regroupement etant recalcule a la demande (cf. design.md decision 3).
 */
@Entity
@Table(name = "photo_asset")
public class PhotoAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private AnalysisJob job;

    @Column(nullable = false, length = 1024)
    private String relativePath;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private double blurScore;

    @Column(nullable = false)
    private long pHash;

    @Column(nullable = false)
    private boolean blurred;

    /**
     * Etat courant de suppression : initialise a la valeur par defaut (blurred)
     * a l'analyse, puis modifiable via les endpoints de toggle des onglets de revue.
     */
    @Column(nullable = false)
    private boolean markedForDeletion;

    /**
     * Vrai des que l'utilisateur a explicitement bascule cette photo (bouton coche/decoche).
     * Permet de ne jamais ecraser un choix manuel lors du recalcul des groupes de doublons
     * a un nouveau seuil de similarite (cf. duplicate-review spec).
     */
    @Column(nullable = false)
    private boolean deletionOverridden;

    @Column(nullable = false)
    private Instant analyzedAt;

    protected PhotoAsset() {
        // JPA
    }

    public PhotoAsset(AnalysisJob job, String relativePath, long fileSize, double blurScore, long pHash,
            boolean blurred) {
        this.job = job;
        this.relativePath = relativePath;
        this.fileSize = fileSize;
        this.blurScore = blurScore;
        this.pHash = pHash;
        this.blurred = blurred;
        this.markedForDeletion = blurred;
        this.analyzedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public AnalysisJob getJob() {
        return job;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public double getBlurScore() {
        return blurScore;
    }

    public long getPHash() {
        return pHash;
    }

    public boolean isBlurred() {
        return blurred;
    }

    public boolean isMarkedForDeletion() {
        return markedForDeletion;
    }

    public void setMarkedForDeletion(boolean markedForDeletion) {
        this.markedForDeletion = markedForDeletion;
    }

    public boolean isDeletionOverridden() {
        return deletionOverridden;
    }

    public void setDeletionOverridden(boolean deletionOverridden) {
        this.deletionOverridden = deletionOverridden;
    }

    public Instant getAnalyzedAt() {
        return analyzedAt;
    }
}
