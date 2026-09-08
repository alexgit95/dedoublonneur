package fr.dedoublonneur.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Chemins NAS et parametres d'analyse, lus depuis application*.yml (prefixe "app").
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Nas nas, Analysis analysis) {

    public record Nas(String sourcePath, String outputPath) {
    }

    public record Analysis(int checkpointBatchSize, int defaultSimilarityThreshold, double blurThreshold) {
        public Analysis {
            if (defaultSimilarityThreshold < 0 || defaultSimilarityThreshold > 100) {
                throw new IllegalArgumentException("app.analysis.default-similarity-threshold doit etre compris entre 0 et 100");
            }
        }
    }
}
