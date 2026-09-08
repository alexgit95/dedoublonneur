package fr.dedoublonneur.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowResetNotAllowedException extends RuntimeException {

    public WorkflowResetNotAllowedException() {
        super("La reinitialisation est indisponible pendant le traitement du dossier.");
    }
}