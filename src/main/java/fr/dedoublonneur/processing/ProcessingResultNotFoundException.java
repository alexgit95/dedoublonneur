package fr.dedoublonneur.processing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProcessingResultNotFoundException extends RuntimeException {

    public ProcessingResultNotFoundException(Long jobId) {
        super("Aucun recapitulatif de traitement pour le job " + jobId + " (traitement pas encore termine).");
    }
}
