package fr.dedoublonneur.analysis;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

/**
 * Calcule, pour une photo JPEG ou PNG, un score de nettete (variance du Laplacien sur une
 * version reduite en niveaux de gris) et un hash perceptuel DCT 64 bits.
 * Ne lit jamais que les pixels : le fichier source n'est jamais reecrit (design.md decision 4/6).
 */
@Service
public class ImageAnalysisService {

    /** Grille utilisee pour le score de nettete : suffisamment petite pour rester tres peu couteuse. */
    private static final int BLUR_GRID_SIZE = 64;

    private static final int HASH_DCT_SIZE = 32;
    private static final int HASH_LOW_FREQUENCY_SIZE = 9;
    private static final int HASH_BITS = 64;

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
        long pHash = computeDctHash(toGrayscale(original, HASH_DCT_SIZE, HASH_DCT_SIZE));
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

    private long computeDctHash(BufferedImage gray) {
        double[][] cosine = new double[HASH_DCT_SIZE][HASH_LOW_FREQUENCY_SIZE];
        for (int pixel = 0; pixel < HASH_DCT_SIZE; pixel++) {
            for (int frequency = 0; frequency < HASH_LOW_FREQUENCY_SIZE; frequency++) {
                cosine[pixel][frequency] = Math.cos((2 * pixel + 1) * frequency * Math.PI
                        / (2 * HASH_DCT_SIZE));
            }
        }

        double[] selected = new double[HASH_BITS];
        int selectedCount = 0;
        for (int frequency = 1; frequency <= 2 * (HASH_LOW_FREQUENCY_SIZE - 1)
                && selectedCount < HASH_BITS; frequency++) {
            for (int yFrequency = 0; yFrequency < HASH_LOW_FREQUENCY_SIZE; yFrequency++) {
                int xFrequency = frequency - yFrequency;
                if (xFrequency < 0 || xFrequency >= HASH_LOW_FREQUENCY_SIZE
                        || (xFrequency == 0 && yFrequency == 0)) {
                    continue;
                }
                selected[selectedCount++] = dctCoefficient(gray, xFrequency, yFrequency, cosine);
                if (selectedCount == HASH_BITS) {
                    break;
                }
            }
        }

        double[] sorted = selected.clone();
        Arrays.sort(sorted);
        double median = (sorted[HASH_BITS / 2 - 1] + sorted[HASH_BITS / 2]) / 2.0;
        long hash = 0L;
        for (int i = 0; i < HASH_BITS; i++) {
            if (selected[i] > median) {
                hash |= 1L << i;
            }
        }
        return hash;
    }

    private double dctCoefficient(BufferedImage gray, int xFrequency, int yFrequency, double[][] cosine) {
        int size = gray.getWidth();
        double coefficient = 0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                coefficient += grayLevel(gray, x, y)
                        * cosine[x][xFrequency]
                        * cosine[y][yFrequency];
            }
        }
        return coefficient;
    }

    private int grayLevel(BufferedImage gray, int x, int y) {
        return gray.getRaster().getSample(x, y, 0);
    }

    /** Distance de Hamming entre deux hash perceptuels, utilisee pour le clustering de doublons a la demande. */
    public static int hammingDistance(long hashA, long hashB) {
        return Long.bitCount(hashA ^ hashB);
    }
}
