package fr.dedoublonneur.web;

/** Corps de requete pour declencher le traitement d'un dossier (bouton "Traiter le dossier"). */
public record ProcessFolderRequest(String outputFolderName) {
}
