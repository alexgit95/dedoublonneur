package fr.dedoublonneur.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import fr.dedoublonneur.domain.AnalysisJob;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.Event;
import fr.dedoublonneur.domain.EventRepository;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;

/**
 * Couvre l'onglet "Flou" (blur-review) : etat coche par defaut, pagination,
 * persistance du bascule keep/delete. Appelle le controller directement (bean Spring)
 * plutot que via HTTP/MockMvc, pour rester independant des modules de test web.
 *
 * Pas de @Transactional (voir design.md decision 9) : nettoyage explicite entre tests.
 */
@SpringBootTest
@ActiveProfiles("local")
class PhotoAssetControllerTest {

    @Autowired
    private PhotoAssetController photoAssetController;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnalysisJobRepository jobRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @AfterEach
    void cleanUp() {
        photoAssetRepository.deleteAll();
        jobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void blurredPhotoIsPreCheckedForDeletionByDefault() {
        AnalysisJob job = createJobWithPhotos(3);

        Page<PhotoAssetResponse> page = photoAssetController.getBlurredPhotos(job.getId(), PageRequest.of(0, 50));

        assertThat(page.getContent()).allSatisfy(photo -> assertThat(photo.markedForDeletion()).isTrue());
    }

    @Test
    void blurredEndpointIsPaginated() {
        AnalysisJob job = createJobWithPhotos(5);

        Page<PhotoAssetResponse> firstPage = photoAssetController.getBlurredPhotos(job.getId(), PageRequest.of(0, 2));
        Page<PhotoAssetResponse> secondPage = photoAssetController.getBlurredPhotos(job.getId(), PageRequest.of(1, 2));

        assertThat(firstPage.getNumberOfElements()).isEqualTo(2);
        assertThat(secondPage.getNumberOfElements()).isEqualTo(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
    }

    @Test
    void togglingDeletionPersists() {
        AnalysisJob job = createJobWithPhotos(1);
        PhotoAsset photo = photoAssetRepository.findByJobId(job.getId()).get(0);
        assertThat(photo.isMarkedForDeletion()).isTrue();

        PhotoAssetResponse response = photoAssetController.toggleDeletion(photo.getId(),
                new ToggleDeletionRequest(false));

        assertThat(response.markedForDeletion()).isFalse();
        PhotoAsset reloaded = photoAssetRepository.findById(photo.getId()).orElseThrow();
        assertThat(reloaded.isMarkedForDeletion()).isFalse();
    }

    private AnalysisJob createJobWithPhotos(int blurryCount) {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, blurryCount, 90));
        for (int i = 0; i < blurryCount; i++) {
            photoAssetRepository.save(new PhotoAsset(job, "IMG_" + i + ".jpg", 1000L, 5.0, i, true));
        }
        return job;
    }
}
