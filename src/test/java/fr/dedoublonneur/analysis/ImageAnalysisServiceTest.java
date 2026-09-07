package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageAnalysisServiceTest {

    private final ImageAnalysisService service = new ImageAnalysisService();

    @Test
    void sharpImageScoresHigherThanUniformBlurryImage(@TempDir Path tempDir) throws IOException {
        Path sharpFile = tempDir.resolve("sharp.jpg");
        Path blurryFile = tempDir.resolve("blurry.jpg");
        writeJpeg(sharpFile, sharpNoiseImage());
        writeJpeg(blurryFile, uniformColorImage());

        PhotoAnalysisResult sharp = service.analyze(sharpFile);
        PhotoAnalysisResult blurry = service.analyze(blurryFile);

        assertThat(sharp.blurScore()).isGreaterThan(blurry.blurScore());
    }

    @Test
    void sameImageProducesTheSamePerceptualHash(@TempDir Path tempDir) throws IOException {
        Path fileA = tempDir.resolve("a.jpg");
        Path fileB = tempDir.resolve("b.jpg");
        BufferedImage image = sharpNoiseImage();
        writeJpeg(fileA, image);
        writeJpeg(fileB, image);

        long hashA = service.analyze(fileA).pHash();
        long hashB = service.analyze(fileB).pHash();

        assertThat(hashA).isEqualTo(hashB);
    }

    @Test
    void veryDifferentImagesProduceDistantHashes(@TempDir Path tempDir) throws IOException {
        Path checkerboardFile = tempDir.resolve("checkerboard.jpg");
        Path invertedFile = tempDir.resolve("inverted-checkerboard.jpg");
        writeJpeg(checkerboardFile, checkerboardImage(false));
        writeJpeg(invertedFile, checkerboardImage(true));

        long checkerboardHash = service.analyze(checkerboardFile).pHash();
        long invertedHash = service.analyze(invertedFile).pHash();

        assertThat(ImageAnalysisService.hammingDistance(checkerboardHash, invertedHash)).isGreaterThan(32);
    }

    private static BufferedImage checkerboardImage(boolean inverted) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                boolean black = (((x / 32) + (y / 32)) % 2 == 0) ^ inverted;
                image.setRGB(x, y, black ? 0x000000 : 0xFFFFFF);
            }
        }
        return image;
    }

    private static void writeJpeg(Path file, BufferedImage image) throws IOException {
        ImageIO.write(image, "jpg", file.toFile());
    }

    private static BufferedImage sharpNoiseImage() {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(42);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                boolean black = ((x / 4) + (y / 4)) % 2 == 0;
                image.setRGB(x, y, black ? 0x000000 : 0xFFFFFF);
            }
        }
        // quelques pixels de bruit pour eviter un motif trop parfaitement periodique
        for (int i = 0; i < 200; i++) {
            image.setRGB(random.nextInt(256), random.nextInt(256), random.nextInt(0xFFFFFF));
        }
        return image;
    }

    private static BufferedImage uniformColorImage() {
        return solidColorImage(new Color(128, 128, 128));
    }

    private static BufferedImage solidColorImage(Color color) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 256, 256);
        g.dispose();
        return image;
    }
}
