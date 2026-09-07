package fr.dedoublonneur.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.domain.ProcessingResultRepository;
import fr.dedoublonneur.processing.FolderProcessingService;
import fr.dedoublonneur.processing.ProcessingResultNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Traitement", description = "Copie du dossier vers la sortie et recapitulatif (folder-processing)")
public class FolderProcessingController {

    private final FolderProcessingService folderProcessingService;
    private final ProcessingResultRepository processingResultRepository;

    public FolderProcessingController(FolderProcessingService folderProcessingService,
            ProcessingResultRepository processingResultRepository) {
        this.folderProcessingService = folderProcessingService;
        this.processingResultRepository = processingResultRepository;
    }

    @PostMapping(value = "/api/jobs/{id}/process", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Traiter le dossier (copie vers la sortie)",
            description = "Copie les photos conservees et toutes les videos vers le dossier de sortie indique, "
                    + "sans jamais modifier le dossier source. Refuse la demande si le dossier de sortie existe "
                    + "deja ou si le job n'est pas en revue (READY_FOR_REVIEW).")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Traitement demarre en tache de fond"),
            @ApiResponse(responseCode = "404", description = "Job introuvable"),
            @ApiResponse(responseCode = "409", description = "Dossier de sortie deja existant ou job non pret")
    })
    public ResponseEntity<Void> processFolder(@PathVariable Long id, @RequestBody ProcessFolderRequest request) {
        folderProcessingService.startProcessing(id, request.outputFolderName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping(value = "/api/jobs/{id}/result", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulter le recapitulatif de traitement",
            description = "Retourne le nombre de photos conservees/supprimees et l'espace disque avant/apres, "
                    + "une fois le traitement du dossier termine.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recapitulatif disponible"),
            @ApiResponse(responseCode = "404", description = "Traitement pas encore termine")
    })
    public ProcessingResultResponse getResult(@PathVariable Long id) {
        return processingResultRepository.findByJobId(id)
                .map(ProcessingResultResponse::from)
                .orElseThrow(() -> new ProcessingResultNotFoundException(id));
    }
}
