package fr.dedoublonneur.analysis;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * Calcule, pour une photo JPEG, un score de nettete (variance du Laplacien sur une
 * version reduite en niveaux de gris) et un hash perceptuel (average-hash 8x8 -> 64 bits).
 * Ne lit jamais que les pixels : le fichier source n'est jamais reecrit (design.md decision 4/6).
 */
@Service
public class ImageAnalysisService {

    /** Grille utilisee pour le score de nettete : suffisamment petite pour rester tres peu couteuse. */
    private static final int BLUR_GRID_SIZE = 64;

    /** Grille 8x8 = 64 pixels, un par bit du hash perceptuel (long). */
    private static final int HASH_GRID_SIZE = 8;

    private static final int[][] LAPLACIAN_KERNEL = {
            { 0, 1, 0 },
            { 1, -4, 1 },
            { 0, 1, 0 }
    };

    public PhotoAnalysisResult analyze(Path imageFile) throws IOException {
        BufferedImage original = ImageIO.read(imageFile.toFile());
        if (original == null) {
            throw new IOException("Fichier image illisible ou format non supporte: " + imageFile);
        }

        double blurScore = computeBlurScore(toGrayscale(original, BLUR_GRID_SIZE, BLUR_GRID_SIZE));
        long pHash = computeAverageHash(toGrayscale(original, HASH_GRID_SIZE, HASH_GRID_SIZE));
        return new PhotoAnalysisResult(blurScore, pHash);
    }

    private BufferedImage toGrayscale(BufferedImage source, int width, int height) {
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayscale.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return grayscale;
    }

    /** Variance de la convolution par un noyau Laplacien : plus la variance est faible, plus l'image est floue. */
    private double computeBlurScore(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        double[] laplacian = new double[width * height];
        double sum = 0;

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double value = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        value += LAPLACIAN_KERNEL[ky + 1][kx + 1] * grayLevel(gray, x + kx, y + ky);
                    }
                }
                laplacian[y * width + x] = value;
                sum += value;
            }
        }

        double mean = sum / laplacian.length;
        double variance = 0;
        for (double value : laplacian) {
            double diff = value - mean;
            variance += diff * diff;
        }
        return variance / laplacian.length;
    }

    /** Average-hash : 1 bit par pixel selon qu'il est au-dessus ou en-dessous de la moyenne de l'image reduite. */
    private long computeAverageHash(BufferedImage gray) {
        int width = gray.getWidth();
        int height = gray.getHeight();
        int[] levels = new int[width * height];
        long total = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int level = grayLevel(gray, x, y);
                levels[y * width + x] = level;
                total += level;
            }
        }
        double mean = (double) total / levels.length;

        long hash = 0L;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] >= mean) {
                hash |= (1L << i);
            }
        }
        return hash;
    }

    private int grayLevel(BufferedImage gray, int x, int y) {
        return gray.getRaster().getSample(x, y, 0);
    }

    /** Distance de Hamming entre deux hash perceptuels, utilisee pour le clustering de doublons a la demande. */
    public static int hammingDistance(long hashA, long hashB) {
        return Long.bitCount(hashA ^ hashB);
    }
}
