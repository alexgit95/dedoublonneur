package fr.dedoublonneur.analysis;

/**
 * Resultat brut d'analyse d'une photo : score de nettete (variance du Laplacien,
 * plus eleve = plus net) et hash perceptuel DCT 64 bits pour le
 * regroupement de doublons calcule a la demande.
 */
public record PhotoAnalysisResult(double blurScore, long pHash) {
}
