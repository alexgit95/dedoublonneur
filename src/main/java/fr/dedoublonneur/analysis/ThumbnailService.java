package fr.dedoublonneur.analysis;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Vignettes generees pendant l'analyse et mises en cache sur disque local ephemere
 * (design.md decision 7) : jamais sur le NAS, purgees une fois le dossier traite.
 */
@Service
public class ThumbnailService {

    private static final Logger log = LoggerFactory.getLogger(ThumbnailService.class);
    private static final int MAX_DIMENSION = 320;

    /** Repertoire racine du cache de vignettes pour un job donne. */
    public Path cacheDir(Long jobId) {
        return Path.of(System.getProperty("java.io.tmpdir"), "thumbnails", String.valueOf(jobId));
    }

    private Path cachePath(Long jobId, Long photoId) {
        return cacheDir(jobId).resolve(photoId + ".jpg");
    }

    public boolean exists(Long jobId, Long photoId) {
        return Files.isRegularFile(cachePath(jobId, photoId));
    }

    public Path getPath(Long jobId, Long photoId) {
        return cachePath(jobId, photoId);
    }

    /** Genere (ou regenere) la vignette reduite d'une photo a partir du fichier source. */
    public void generate(Long jobId, Long photoId, Path sourceImage) throws IOException {
        BufferedImage original = ImageIO.read(sourceImage.toFile());
        if (original == null) {
            throw new IOException("Fichier image illisible ou format non supporte: " + sourceImage);
        }

        int width = original.getWidth();
        int height = original.getHeight();
        double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(width, height));
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = thumbnail.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g.dispose();

        Path target = cachePath(jobId, photoId);
        Files.createDirectories(target.getParent());
        ImageIO.write(thumbnail, "jpg", target.toFile());
    }

    /** Supprime tout le cache de vignettes d'un job (appele une fois le dossier traite). */
    public void purge(Long jobId) {
        Path dir = cacheDir(jobId);
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted((a, b) -> b.compareTo(a)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            log.warn("Impossible de purger le cache de vignettes pour le job {}", jobId, e);
        }
    }
}
