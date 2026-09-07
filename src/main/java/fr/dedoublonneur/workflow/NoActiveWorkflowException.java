package fr.dedoublonneur.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Leve lorsqu'une action necessitant un workflow actif (ex: annulation) est demandee
 * alors qu'aucun n'est en cours.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class NoActiveWorkflowException extends RuntimeException {

    public NoActiveWorkflowException() {
        super("Aucun workflow actif a annuler.");
    }
}
