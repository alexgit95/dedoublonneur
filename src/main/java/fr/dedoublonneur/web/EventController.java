package fr.dedoublonneur.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.analysis.AnalysisOrchestrator;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.workflow.EventFolderService;
import fr.dedoublonneur.workflow.EventFolderListing;
import fr.dedoublonneur.workflow.WorkflowStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Evenements", description = "Listage des dossiers evenement et demarrage d'analyse")
public class EventController {

    private final EventFolderService eventFolderService;
    private final WorkflowStateService workflowStateService;
    private final AnalysisOrchestrator analysisOrchestrator;

    public EventController(EventFolderService eventFolderService, WorkflowStateService workflowStateService,
            AnalysisOrchestrator analysisOrchestrator) {
        this.eventFolderService = eventFolderService;
        this.workflowStateService = workflowStateService;
        this.analysisOrchestrator = analysisOrchestrator;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les dossiers evenement",
                description = "Retourne les sous-dossiers presents directement sous le point de montage NAS source, "
                    + "avec un indicateur informatif lorsque leur export est termine.")
            @ApiResponse(responseCode = "200", description = "Liste des dossiers evenement et de leur etat d'export")
            public List<EventFolderListing> listEventFolders() {
        return eventFolderService.listAvailableFolders();
    }

    @PostMapping(value = "/{folderName}/analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Demarrer l'analyse d'un dossier evenement",
            description = "Cree un nouveau job d'analyse pour le dossier donne. Refuse la demande si un autre "
                    + "workflow (analyse, revue ou traitement) est deja actif.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Job d'analyse cree et demarre"),
            @ApiResponse(responseCode = "409", description = "Un autre workflow est deja actif")
    })
    public ResponseEntity<AnalysisJobResponse> startAnalysis(@PathVariable String folderName) {
        AnalysisJob job = workflowStateService.startAnalysis(folderName);
        analysisOrchestrator.beginAnalysis(job);
        return ResponseEntity.status(HttpStatus.CREATED).body(AnalysisJobResponse.from(job, folderName));
    }
}
