package fr.dedoublonneur.web;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.analysis.JobNotFoundException;
import fr.dedoublonneur.analysis.JobNotResumableException;
import fr.dedoublonneur.analysis.PhotoAnalysisRunner;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/jobs")
@Tag(name = "Jobs d'analyse", description = "Progression et reprise manuelle des jobs d'analyse en tache de fond")
public class AnalysisJobController {

    private final AnalysisJobRepository jobRepository;
    private final PhotoAssetRepository photoAssetRepository;
    private final PhotoAnalysisRunner photoAnalysisRunner;

    public AnalysisJobController(AnalysisJobRepository jobRepository, PhotoAssetRepository photoAssetRepository,
            PhotoAnalysisRunner photoAnalysisRunner) {
        this.jobRepository = jobRepository;
        this.photoAssetRepository = photoAssetRepository;
        this.photoAnalysisRunner = photoAnalysisRunner;
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulter la progression d'un job d'analyse",
            description = "Retourne le statut du job, son seuil de similarite par defaut ainsi que le nombre de "
                + "photos analysees sur le total du snapshot.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progression du job"),
            @ApiResponse(responseCode = "404", description = "Job introuvable")
    })
    public JobProgressResponse getProgress(@PathVariable Long id) {
        AnalysisJob job = findJobOrThrow(id);
        String folderName = jobRepository.findEventFolderNameByJobId(id).orElseThrow(() -> new JobNotFoundException(id));
        long analyzed = photoAssetRepository.countByJobId(id);
        return new JobProgressResponse(job.getId(), folderName, job.getStatus().name(),
                (int) analyzed, job.getSnapshotSize(), photoAnalysisRunner.isRunning(id),
                job.getSimilarityThresholdDefault());
    }

    @PostMapping(value = "/{id}/resume", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Reprendre manuellement un job d'analyse interrompu",
            description = "Relance le traitement du snapshot a partir des photos deja analysees (aucune photo "
                    + "deja traitee n'est recalculee). Refuse si le job n'est pas en statut ANALYZING ou tourne deja.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Reprise de l'analyse acceptee"),
            @ApiResponse(responseCode = "404", description = "Job introuvable"),
            @ApiResponse(responseCode = "409", description = "Job non resumable (deja termine ou deja en cours)")
    })
    public ResponseEntity<JobProgressResponse> resume(@PathVariable Long id) {
        AnalysisJob job = findJobOrThrow(id);
        if (job.getStatus() != JobStatus.ANALYZING) {
            throw new JobNotResumableException("Le job " + id + " n'est pas en statut ANALYZING (reprise impossible).");
        }
        if (photoAnalysisRunner.isRunning(id)) {
            throw new JobNotResumableException("Le job " + id + " est deja en cours de traitement.");
        }
        photoAnalysisRunner.runAnalysisAsync(id);
        return ResponseEntity.accepted().body(getProgress(id));
    }

    private AnalysisJob findJobOrThrow(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }
}
