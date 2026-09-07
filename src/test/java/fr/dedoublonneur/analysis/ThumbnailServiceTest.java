package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

// Le cache de vignettes vit sur le vrai disque (java.io.tmpdir), pas dans la DB en memoire :
// des identifiants uniques + nettoyage explicite evitent toute pollution entre executions.
class ThumbnailServiceTest {

    private final ThumbnailService service = new ThumbnailService();
    private long jobId;

    @AfterEach
    void cleanUp() {
        service.purge(jobId);
    }

    @Test
    void generatesAndCachesThumbnailOnDisk(@TempDir Path tempDir) throws IOException {
        jobId = System.nanoTime();
        Path source = tempDir.resolve("photo.jpg");
        writeJpeg(source, 800, 600);

        service.generate(jobId, 42L, source);

        assertThat(service.exists(jobId, 42L)).isTrue();
        assertThat(Files.size(service.getPath(jobId, 42L))).isGreaterThan(0);
    }

    @Test
    void purgeRemovesTheWholeJobCacheDirectory(@TempDir Path tempDir) throws IOException {
        jobId = System.nanoTime();
        Path source = tempDir.resolve("photo.jpg");
        writeJpeg(source, 400, 300);
        service.generate(jobId, 1L, source);
        service.generate(jobId, 2L, source);
        assertThat(service.exists(jobId, 1L)).isTrue();

        service.purge(jobId);

        assertThat(service.exists(jobId, 1L)).isFalse();
        assertThat(service.exists(jobId, 2L)).isFalse();
        assertThat(Files.exists(service.cacheDir(jobId))).isFalse();
    }

    private static void writeJpeg(Path file, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, width, height);
        g.dispose();
        ImageIO.write(image, "jpg", file.toFile());
    }
}
