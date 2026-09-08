package fr.dedoublonneur.processing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import fr.dedoublonneur.domain.ProcessingResult;
import fr.dedoublonneur.domain.ProcessingResultRepository;

/**
 * Couvre le traitement du dossier : rejet dossier de sortie existant, preservation des
 * metadonnees, passage des videos, absence des photos supprimees, exactitude du recap
 * (folder-processing spec).
 *
 * Pas de @Transactional (voir design.md decision 9) : nettoyage explicite entre tests.
 */
@SpringBootTest
@ActiveProfiles("local")
class FolderProcessingServiceTest {

    private static final Path TEST_OUTPUT_ROOT = createTestOutputRoot();

    @DynamicPropertySource
    static void overrideNasOutputPath(DynamicPropertyRegistry registry) {
        registry.add("app.nas.output-path", TEST_OUTPUT_ROOT::toString);
    }

    @Autowired
    private FolderProcessingService folderProcessingService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @Autowired
    private ProcessingResultRepository processingResultRepository;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private fr.dedoublonneur.config.AppProperties appProperties;

    private static Path createTestOutputRoot() {
        try {
            return Files.createTempDirectory("dedoublonneur-output-");
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @AfterEach
    void cleanUp() {
        processingResultRepository.deleteAll();
        photoAssetRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void rejectsAlreadyExistingOutputFolder(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        AnalysisJob job = createReadyJob(sourceDir);

        // Le dossier de sortie est resolu sous app.nas.output-path : on cree l'existant la-bas.
        String outputFolderName = "existant-" + System.nanoTime();
        Path existingOutputDir = Files.createDirectories(
                Path.of(appProperties.nas().outputPath(), outputFolderName));
        try {
            assertThatThrownBy(() -> folderProcessingService.startProcessing(job.getId(), outputFolderName))
                    .isInstanceOf(OutputFolderAlreadyExistsException.class);
        } finally {
            Files.deleteIfExists(existingOutputDir);
        }
    }

    @Test
    void rejectsProcessingWhenJobIsNotReadyForReview(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Event event = eventRepository.save(new Event("evt-" + System.nanoTime(), sourceDir.toString()));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 0, 90)); // reste ANALYZING

        assertThatThrownBy(() -> folderProcessingService.startProcessing(job.getId(), "sortie"))
                .isInstanceOf(ProcessingNotAllowedException.class);
    }

    @Test
    void failedAsyncProcessingCancelsTheJob(@TempDir Path root) {
        AnalysisJob job = createReadyJob(root.resolve("source"));
        photoAssetRepository.save(new PhotoAsset(job, "missing.jpg", 12L, 100.0, 1L, false));

        folderProcessingService.startProcessing(job.getId(), "failed-" + System.nanoTime());

        long deadline = System.nanoTime() + 5_000_000_000L;
        while (jobRepository.findById(job.getId()).orElseThrow().getStatus() != JobStatus.CANCELLED
            && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.CANCELLED);
        assertThat(processingResultRepository.findByJobId(job.getId())).isEmpty();
    }

    @Test
    void previewCalculatesSavingsWithoutCreatingOutput(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Path kept = sourceDir.resolve("kept.jpg");
        Path deleted = sourceDir.resolve("deleted.jpg");
        Files.writeString(kept, "1234567890");
        Files.writeString(deleted, "12345");
        Files.writeString(sourceDir.resolve("video.mp4"), "video");

        AnalysisJob job = createReadyJob(sourceDir);
        photoAssetRepository.save(new PhotoAsset(job, "kept.jpg", 10L, 100.0, 1L, false));
        photoAssetRepository.save(new PhotoAsset(job, "deleted.jpg", 5L, 1.0, 2L, true));

        ProcessingPreviewResponse preview = folderProcessingService.preview(job.getId());

        assertThat(preview.keptPhotoCount()).isEqualTo(1);
        assertThat(preview.deletedPhotoCount()).isEqualTo(1);
        assertThat(preview.videoCount()).isEqualTo(1);
        assertThat(preview.sourceBytes()).isEqualTo(20L);
        assertThat(preview.keptBytes()).isEqualTo(10L);
        assertThat(preview.estimatedSavedBytes()).isEqualTo(5L);
        assertThat(root.resolve("output")).doesNotExist();
        assertThat(jobRepository.findById(job.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.READY_FOR_REVIEW);
    }

    @Test
    void copiesKeptPhotoPreservingMetadataAndLeavesSourceUntouched(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Path photo = sourceDir.resolve("IMG_1.jpg");
        Files.writeString(photo, "contenu-photo-simulee");
        FileTime originalModifiedTime = FileTime.from(Instant.now().minus(10, ChronoUnit.DAYS));
        Files.setLastModifiedTime(photo, originalModifiedTime);

        AnalysisJob job = createReadyJob(sourceDir);
        photoAssetRepository.save(new PhotoAsset(job, "IMG_1.jpg", Files.size(photo), 100.0, 1L, false));

        Path outputDir = root.resolve("output");
        folderProcessingService.processFolder(job.getId(), sourceDir.toString(), outputDir.toString());

        Path copied = outputDir.resolve("IMG_1.jpg");
        assertThat(copied).exists();
        assertThat(folderProcessingService.progress(job.getId()).status()).isEqualTo("DONE");
        assertThat(folderProcessingService.progress(job.getId()).progressPercent()).isEqualTo(100);
        assertThat(Files.readString(copied)).isEqualTo("contenu-photo-simulee");
        BasicFileAttributes copiedAttrs = Files.readAttributes(copied, BasicFileAttributes.class);
        assertThat(copiedAttrs.lastModifiedTime()).isEqualTo(originalModifiedTime);
        // Le fichier source n'est jamais modifie ni supprime.
        assertThat(photo).exists();
        assertThat(Files.readString(photo)).isEqualTo("contenu-photo-simulee");
    }

    @Test
    void deletedPhotoIsAbsentFromOutputAndSourceRemainsUntouched(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Path photo = sourceDir.resolve("IMG_blurry.jpg");
        Files.writeString(photo, "photo-floue");

        AnalysisJob job = createReadyJob(sourceDir);
        photoAssetRepository.save(new PhotoAsset(job, "IMG_blurry.jpg", Files.size(photo), 1.0, 1L, true));

        Path outputDir = root.resolve("output");
        folderProcessingService.processFolder(job.getId(), sourceDir.toString(), outputDir.toString());

        assertThat(outputDir.resolve("IMG_blurry.jpg")).doesNotExist();
        assertThat(photo).exists();
    }

    @Test
    void copiesVideosUnconditionallyWithoutCountingThemAsPhotos(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Files.writeString(sourceDir.resolve("clip.mp4"), "contenu-video");

        AnalysisJob job = createReadyJob(sourceDir);

        Path outputDir = root.resolve("output");
        folderProcessingService.processFolder(job.getId(), sourceDir.toString(), outputDir.toString());

        assertThat(outputDir.resolve("clip.mp4")).exists();
        ProcessingResult result = processingResultRepository.findByJobId(job.getId()).orElseThrow();
        assertThat(result.getVideoCount()).isEqualTo(1);
        assertThat(result.getKeptCount()).isZero();
        assertThat(result.getDeletedCount()).isZero();
    }

    @Test
    void recapReflectsKeptDeletedAndSpaceAccurately(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Path keptPhoto = sourceDir.resolve("IMG_kept.jpg");
        Path deletedPhoto = sourceDir.resolve("IMG_deleted.jpg");
        Files.writeString(keptPhoto, "1234567890"); // 10 octets
        Files.writeString(deletedPhoto, "12345"); // 5 octets

        AnalysisJob job = createReadyJob(sourceDir);
        photoAssetRepository.save(new PhotoAsset(job, "IMG_kept.jpg", Files.size(keptPhoto), 200.0, 1L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_deleted.jpg", Files.size(deletedPhoto), 1.0, 2L, true));

        Path outputDir = root.resolve("output");
        folderProcessingService.processFolder(job.getId(), sourceDir.toString(), outputDir.toString());

        ProcessingResult result = processingResultRepository.findByJobId(job.getId()).orElseThrow();
        assertThat(result.getKeptCount()).isEqualTo(1);
        assertThat(result.getDeletedCount()).isEqualTo(1);
        assertThat(result.getSpaceBeforeBytes()).isEqualTo(15L);
        assertThat(result.getSpaceAfterBytes()).isEqualTo(10L);

        AnalysisJob reloaded = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.DONE);
    }

    @Test
    void purgesThumbnailCacheOnceProcessingCompletes(@TempDir Path root) throws IOException {
        Path sourceDir = Files.createDirectories(root.resolve("source"));
        Path photoFile = sourceDir.resolve("IMG_1.jpg");
        Files.writeString(photoFile, "photo");

        AnalysisJob job = createReadyJob(sourceDir);
        PhotoAsset photo = photoAssetRepository.save(new PhotoAsset(job, "IMG_1.jpg", 5L, 100.0, 1L, false));
        Files.createDirectories(thumbnailService.cacheDir(job.getId()));
        Files.writeString(thumbnailService.getPath(job.getId(), photo.getId()), "vignette-simulee");
        assertThat(thumbnailService.exists(job.getId(), photo.getId())).isTrue();

        Path outputDir = root.resolve("output");
        folderProcessingService.processFolder(job.getId(), sourceDir.toString(), outputDir.toString());

        assertThat(thumbnailService.exists(job.getId(), photo.getId())).isFalse();
        assertThat(Files.exists(thumbnailService.cacheDir(job.getId()))).isFalse();
    }

    private AnalysisJob createReadyJob(Path sourceDir) {
        Event event = eventRepository.save(new Event("evt-" + System.nanoTime(), sourceDir.toString()));
        AnalysisJob job = new AnalysisJob(event, 0, 90);
        job.setStatus(JobStatus.READY_FOR_REVIEW);
        return jobRepository.save(job);
    }
}
