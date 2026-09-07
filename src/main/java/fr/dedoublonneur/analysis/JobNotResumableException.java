package fr.dedoublonneur.analysis;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Leve lorsqu'une reprise est demandee alors que le job est deja en cours de traitement
 * ou n'est plus dans un etat resumable (ANALYZING).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class JobNotResumableException extends RuntimeException {

    public JobNotResumableException(String message) {
        super(message);
    }
}
