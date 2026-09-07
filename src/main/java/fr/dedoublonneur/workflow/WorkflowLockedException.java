package fr.dedoublonneur.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Leve lorsqu'un nouveau workflow est demande alors qu'un autre est deja actif
 * (analyse, revue ou traitement en cours) - verrou global d'un seul workflow a la fois.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowLockedException extends RuntimeException {

    public WorkflowLockedException(String activeFolderName) {
        super("Un workflow est deja actif sur le dossier '" + activeFolderName
                + "'. Terminez-le ou annulez-le avant d'en demarrer un nouveau.");
    }
}
