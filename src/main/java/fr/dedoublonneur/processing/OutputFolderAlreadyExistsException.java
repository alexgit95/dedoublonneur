package fr.dedoublonneur.processing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class OutputFolderAlreadyExistsException extends RuntimeException {

    public OutputFolderAlreadyExistsException(String folderName) {
        super("Le dossier de sortie '" + folderName + "' existe deja.");
    }
}
