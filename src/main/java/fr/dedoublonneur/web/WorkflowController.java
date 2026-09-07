package fr.dedoublonneur.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.workflow.WorkflowStateService;
import fr.dedoublonneur.workflow.WorkflowStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/workflow")
@Tag(name = "Workflow", description = "Etat global du workflow et verrou un seul dossier a la fois")
public class WorkflowController {

    private final WorkflowStateService workflowStateService;

    public WorkflowController(WorkflowStateService workflowStateService) {
        this.workflowStateService = workflowStateService;
    }

    @GetMapping(value = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Consulter le statut du workflow",
            description = "Retourne IDLE si aucun workflow n'est actif, sinon le statut du job en cours.")
    @ApiResponse(responseCode = "200", description = "Statut courant du workflow")
    public WorkflowStatus status() {
        return workflowStateService.currentStatus();
    }

    @PostMapping(value = "/cancel", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Annuler/reinitialiser le workflow actif",
            description = "Libere manuellement le verrou global en annulant le workflow actif, permettant de "
                    + "selectionner un nouveau dossier evenement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workflow annule, retour a l'etat IDLE"),
            @ApiResponse(responseCode = "409", description = "Aucun workflow actif a annuler")
    })
    public WorkflowStatus cancel() {
        return workflowStateService.cancelActiveWorkflow();
    }
}
