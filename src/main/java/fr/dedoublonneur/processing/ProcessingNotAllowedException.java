package fr.dedoublonneur.processing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Leve lorsque le traitement est demande alors que le job n'est pas en revue (READY_FOR_REVIEW). */
@ResponseStatus(HttpStatus.CONFLICT)
public class ProcessingNotAllowedException extends RuntimeException {

    public ProcessingNotAllowedException(String message) {
        super(message);
    }
}
