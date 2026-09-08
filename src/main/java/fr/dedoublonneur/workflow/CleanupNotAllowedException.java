package fr.dedoublonneur.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CleanupNotAllowedException extends RuntimeException {

    public CleanupNotAllowedException(String message) {
        super(message);
    }
}
