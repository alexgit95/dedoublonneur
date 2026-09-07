package fr.dedoublonneur.web;

/** Corps de requete pour basculer l'etat de suppression d'une photo (onglets Flou/Doublons). */
public record ToggleDeletionRequest(boolean markedForDeletion) {
}
