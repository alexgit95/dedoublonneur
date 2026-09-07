package fr.dedoublonneur.analysis;

/**
 * Resultat brut d'analyse d'une photo : score de nettete (variance du Laplacien,
 * plus eleve = plus net) et hash perceptuel 64 bits (average-hash) pour le
 * regroupement de doublons calcule a la demande.
 */
public record PhotoAnalysisResult(double blurScore, long pHash) {
}
