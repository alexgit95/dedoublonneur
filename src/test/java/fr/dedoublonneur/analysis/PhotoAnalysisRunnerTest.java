package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;

/**
 * Couvre le traitement du snapshot fige : checkpoint, reprise sans recalcul des photos
 * deja analysees, et ignorance des fichiers ajoutes apres le snapshot (photo-analysis spec).
 *
 * Pas de @Transactional ici : le generateur d'id hi/lo (GenerationType.AUTO) utilise sa
 * propre transaction imbriquee, incompatible avec une transaction de test englobante sur
 * SQLite (mono-ecrivain). Nettoyage explicite entre tests.
 */
@SpringBootTest
@ActiveProfiles("local")
class PhotoAnalysisRunnerTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @Autowired
    private PhotoAnalysisRunner photoAnalysisRunner;

    @AfterEach
    void cleanUp() {
        photoAssetRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void processesEverySnapshottedPhotoAndChecksPointsProgress(@TempDir Path eventDir) throws IOException {
        writeJpeg(eventDir.resolve("photo1.jpg"));
        writeJpeg(eventDir.resolve("photo2.jpg"));
        AnalysisJob job = createJob(eventDir, List.of("photo1.jpg", "photo2.jpg"));

        photoAnalysisRunner.runAnalysis(job.getId());

        AnalysisJob reloaded = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(photoAssetRepository.countByJobId(job.getId())).isEqualTo(2);
        assertThat(reloaded.getLastProcessedCount()).isEqualTo(2);
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.READY_FOR_REVIEW);
    }

    @Test
    void ignoresFilesAddedAfterTheSnapshotWasTaken(@TempDir Path eventDir) throws IOException {
        writeJpeg(eventDir.resolve("photo1.jpg"));
        writeJpeg(eventDir.resolve("photo2.jpg"));
        AnalysisJob job = createJob(eventDir, List.of("photo1.jpg", "photo2.jpg"));

        // Ajoute apres coup : ne doit pas etre analyse, car absent du snapshot fige.
        writeJpeg(eventDir.resolve("photo3-added-later.jpg"));

        photoAnalysisRunner.runAnalysis(job.getId());

        assertThat(photoAssetRepository.countByJobId(job.getId())).isEqualTo(2);
    }

    @Test
    void resumeSkipsAlreadyAnalyzedPhotosWithoutRecomputingThem(@TempDir Path eventDir) throws IOException {
        writeJpeg(eventDir.resolve("photo1.jpg"));
        writeJpeg(eventDir.resolve("photo2.jpg"));
        AnalysisJob job = createJob(eventDir, List.of("photo1.jpg", "photo2.jpg"));

        // Simule une interruption apres la premiere photo : deja persistee avant reprise.
        PhotoAsset alreadyAnalyzed = photoAssetRepository
                .save(new PhotoAsset(job, "photo1.jpg", 123L, 999.0, 42L, false));

        photoAnalysisRunner.runAnalysis(job.getId());

        assertThat(photoAssetRepository.countByJobId(job.getId())).isEqualTo(2);
        PhotoAsset stillUntouched = photoAssetRepository.findById(alreadyAnalyzed.getId()).orElseThrow();
        assertThat(stillUntouched.getBlurScore()).isEqualTo(999.0);
        assertThat(stillUntouched.getPHash()).isEqualTo(42L);
    }

    private AnalysisJob createJob(Path eventDir, List<String> snapshot) {
        Event event = eventRepository.save(new Event("evenement-" + eventDir.getFileName(), eventDir.toString()));
        AnalysisJob job = new AnalysisJob(event, 0, 90);
        job.setSnapshot(snapshot);
        return jobRepository.save(job);
    }

    private static void writeJpeg(Path file) throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        ImageIO.write(image, "jpg", file.toFile());
    }
}
