package fr.dedoublonneur.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AnalysisJobRepository extends JpaRepository<AnalysisJob, Long> {

    /**
     * Recherche le job actif (non termine) : il ne doit en exister qu'un au plus,
     * le verrou global etant applique au niveau service (design.md decision 1/9).
     * Annote explicitement @Transactional : sans cela, cette methode de requete
     * derivee s'execute hors de toute transaction et peut utiliser une connexion
     * distincte de celle de l'appelant (invisibilite des ecritures non commitees).
     */
    @Transactional(readOnly = true)
    Optional<AnalysisJob> findFirstByStatusIn(List<JobStatus> statuses);

    @Transactional(readOnly = true)
    Optional<AnalysisJob> findFirstByStatusOrderByIdDesc(JobStatus status);

    /**
     * Recupere le chemin du dossier evenement sans charger l'association paresseuse
     * {@code event}, evitant tout risque de LazyInitializationException hors session
     * lorsque le job traite est un objet detache (chaque etape d'analyse s'execute
     * dans sa propre courte transaction, cf. PhotoAnalysisRunner).
     */
    @Transactional(readOnly = true)
    @Query("select j.event.folderPath from AnalysisJob j where j.id = :jobId")
    Optional<String> findEventFolderPathByJobId(@Param("jobId") Long jobId);

    /** Idem pour le nom de dossier (affichage IHM), sans charger l'association event. */
    @Query("select j.event.folderName from AnalysisJob j where j.id = :jobId")
    Optional<String> findEventFolderNameByJobId(@Param("jobId") Long jobId);

    @Transactional(readOnly = true)
    @Query("select new fr.dedoublonneur.domain.CompletedFolderStatus(e.folderName, max(pr.processedAt), "
            + "max(pr.spaceBeforeBytes - pr.spaceAfterBytes)) "
            + "from ProcessingResult pr join pr.job j join j.event e "
            + "where j.status = fr.dedoublonneur.domain.JobStatus.DONE group by e.folderName")
    List<CompletedFolderStatus> findCompletedFolderStatuses();

    @Transactional(readOnly = true)
    @Query("select j from AnalysisJob j join fetch j.event e where e.folderName = :folderName")
    List<AnalysisJob> findByEventFolderName(@Param("folderName") String folderName);
}
