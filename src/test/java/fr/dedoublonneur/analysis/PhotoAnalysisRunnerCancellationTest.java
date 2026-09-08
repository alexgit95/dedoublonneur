package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.config.AppProperties;
import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.JobStatus;
import fr.dedoublonneur.domain.PhotoAssetRepository;

@SpringBootTest
@ActiveProfiles("local")
class PhotoAnalysisRunnerCancellationTest {

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private PhotoAssetRepository photoRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private AppProperties appProperties;

    @AfterEach
    void cleanUp() {
        photoRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void cancellationDuringPhotoDoesNotFinalizeReview(@org.junit.jupiter.api.io.TempDir Path tempDir) throws Exception {
        AtomicReference<PhotoAnalysisRunner> runnerReference = new AtomicReference<>();
        Files.createFile(tempDir.resolve("photo.jpg"));
        Files.createFile(tempDir.resolve("photo-2.jpg"));
        Event event = eventRepository.save(new Event("evenement-cancel", tempDir.toString()));
        AnalysisJob job = new AnalysisJob(event, 0, 90);
        job.setSnapshot(List.of("photo.jpg", "photo-2.jpg"));
        job = jobRepository.save(job);
        Long jobId = job.getId();
        AtomicReference<Long> jobIdReference = new AtomicReference<>(jobId);
        ImageAnalysisService imageService = new ImageAnalysisService() {
            @Override
            public PhotoAnalysisResult analyze(Path imageFile) {
                runnerReference.get().requestCancellation(jobIdReference.get());
                return new PhotoAnalysisResult(100.0, 42L);
            }
        };

        PhotoAnalysisRunner runner = new PhotoAnalysisRunner(jobRepository, photoRepository, imageService,
                thumbnailService, appProperties);
        runnerReference.set(runner);
        runner.runAnalysis(jobId);

        assertThat(runner.isRunning(jobId)).isFalse();
        assertThat(photoRepository.countByJobId(jobId)).isEqualTo(1);
        assertThat(jobRepository.findById(jobId).orElseThrow().getStatus()).isEqualTo(JobStatus.ANALYZING);
    }
}