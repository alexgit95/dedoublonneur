package fr.dedoublonneur.web;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;

/**
 * Couvre la regeneration de vignette en cas d'absence de cache (redemarrage du conteneur).
 *
 * Pas de @Transactional (voir design.md decision 9) : nettoyage explicite entre tests.
 */
@SpringBootTest
@ActiveProfiles("local")
class ThumbnailControllerTest {

    @Autowired
    private ThumbnailController thumbnailController;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @AfterEach
    void cleanUp() {
        if (createdJobId != null) {
            thumbnailService.purge(createdJobId);
        }
        photoAssetRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    private Long createdJobId;

    @Test
    void regeneratesThumbnailOnCacheMiss(@TempDir Path sourceDir) throws IOException {
        Path photoFile = sourceDir.resolve("IMG_1.jpg");
        writeJpeg(photoFile);

        Event event = eventRepository.save(new Event("evt-" + System.nanoTime(), sourceDir.toString()));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 1, 90));
        createdJobId = job.getId();
        PhotoAsset photo = photoAssetRepository
                .save(new PhotoAsset(job, "IMG_1.jpg", Files.size(photoFile), 100.0, 1L, false));

        assertThat(thumbnailService.exists(job.getId(), photo.getId())).isFalse();

        ResponseEntity<byte[]> response = thumbnailController.getThumbnail(photo.getId());

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotEmpty();
        assertThat(thumbnailService.exists(job.getId(), photo.getId())).isTrue();
    }

    private static void writeJpeg(Path file) throws IOException {
        BufferedImage image = new BufferedImage(200, 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 200, 150);
        g.dispose();
        ImageIO.write(image, "jpg", file.toFile());
    }
}
