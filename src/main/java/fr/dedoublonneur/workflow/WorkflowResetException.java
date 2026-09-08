package fr.dedoublonneur.workflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class WorkflowResetException extends RuntimeException {

    public WorkflowResetException(String message) {
        super(message);
    }

    public WorkflowResetException(String message, Throwable cause) {
        super(message, cause);
    }
}