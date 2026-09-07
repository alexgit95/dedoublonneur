package fr.dedoublonneur.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

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

    @Test
    void identicalHashesAreGroupedAtHighThreshold() {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 3, 90));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_1.jpg", 1000L, 50.0, 0x1234L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_2.jpg", 1000L, 30.0, 0x1234L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_3.jpg", 1000L, 10.0, 0x1234L, false));

        List<DuplicateGroupResponse> groups = photoAssetController.getDuplicateGroups(job.getId(), 100);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(0).photos()).hasSize(3);
    }

    @Test
    void completelyDifferentHashesAreNotGroupedAtHighThreshold() {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 2, 90));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_1.jpg", 1000L, 50.0, 0x0000000000000000L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_2.jpg", 1000L, 30.0, 0xFFFFFFFFFFFFFFFFL, false));

        List<DuplicateGroupResponse> groups = photoAssetController.getDuplicateGroups(job.getId(), 100);

        assertThat(groups).isEmpty();
    }

    @Test
    void sharpestPhotoIsKeptByDefaultWithinAGroup() {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 2, 90));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_blurry.jpg", 1000L, 10.0, 0x1234L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_sharp.jpg", 1000L, 200.0, 0x1234L, false));

        List<DuplicateGroupResponse> groups = photoAssetController.getDuplicateGroups(job.getId(), 100);

        assertThat(groups).hasSize(1);
        PhotoAssetResponse sharp = findByPath(groups.get(0), "IMG_sharp.jpg");
        PhotoAssetResponse blurry = findByPath(groups.get(0), "IMG_blurry.jpg");
        assertThat(sharp.markedForDeletion()).isFalse();
        assertThat(blurry.markedForDeletion()).isTrue();
    }

    @Test
    void tieOnBlurScoreKeepsThePhotoThatSortsFirstByFilename() {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 2, 90));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_B.jpg", 1000L, 100.0, 0x1234L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_A.jpg", 1000L, 100.0, 0x1234L, false));

        List<DuplicateGroupResponse> groups = photoAssetController.getDuplicateGroups(job.getId(), 100);

        assertThat(findByPath(groups.get(0), "IMG_A.jpg").markedForDeletion()).isFalse();
        assertThat(findByPath(groups.get(0), "IMG_B.jpg").markedForDeletion()).isTrue();
    }

    @Test
    void manualOverrideSurvivesThresholdRecomputation() {
        Event event = eventRepository.save(new Event("evenement-" + System.nanoTime(), "/tmp/evenement"));
        AnalysisJob job = jobRepository.save(new AnalysisJob(event, 2, 90));
        PhotoAsset blurry = photoAssetRepository.save(new PhotoAsset(job, "IMG_blurry.jpg", 1000L, 10.0, 0x1234L, false));
        photoAssetRepository.save(new PhotoAsset(job, "IMG_sharp.jpg", 1000L, 200.0, 0x1234L, false));

        // L'utilisateur decide explicitement de garder la photo floue malgre le defaut.
        photoAssetController.toggleDeletion(blurry.getId(), new ToggleDeletionRequest(false));

        List<DuplicateGroupResponse> groups = photoAssetController.getDuplicateGroups(job.getId(), 100);

        assertThat(findByPath(groups.get(0), "IMG_blurry.jpg").markedForDeletion()).isFalse();
    }

    private static PhotoAssetResponse findByPath(DuplicateGroupResponse group, String relativePath) {
        return group.photos().stream()
                .filter(photo -> photo.relativePath().equals(relativePath))
                .findFirst()
                .orElseThrow();
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
