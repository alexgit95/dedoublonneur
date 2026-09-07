package fr.dedoublonneur.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifie que le schema Hibernate (ddl-auto) se genere correctement pour le
 * profil local (SQLite) et que les entites sont persistables de bout en bout.
 *
 * Pas de @Transactional : le generateur d'id hi/lo (GenerationType.AUTO) utilise sa
 * propre transaction imbriquee, incompatible avec une transaction de test englobante
 * sur SQLite (mono-ecrivain). Nettoyage explicite en fin de test.
 */
@SpringBootTest
@ActiveProfiles("local")
class SqliteSchemaGenerationTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AnalysisJobRepository analysisJobRepository;

    @Autowired
    private PhotoAssetRepository photoAssetRepository;

    @Autowired
    private ProcessingResultRepository processingResultRepository;

    @AfterEach
    void cleanUp() {
        processingResultRepository.deleteAll();
        photoAssetRepository.deleteAll();
        analysisJobRepository.deleteAll();
        eventRepository.deleteAll();
    }

    @Test
    void persistsFullEntityGraphAgainstSqlite() {
        Event event = eventRepository.save(new Event("vacances-ete-2026", "/data/nas/source/vacances-ete-2026"));

        AnalysisJob job = analysisJobRepository.save(new AnalysisJob(event, 2, 90));

        PhotoAsset sharpPhoto = photoAssetRepository
                .save(new PhotoAsset(job, "IMG_0001.jpg", 3_500_000L, 120.5, 0x1234ABCDL, false));
        PhotoAsset blurryPhoto = photoAssetRepository
                .save(new PhotoAsset(job, "IMG_0002.jpg", 3_400_000L, 4.2, 0x1234ABCEL, true));

        ProcessingResult result = processingResultRepository.save(
                new ProcessingResult(job, 1, 1, 0, 6_900_000L, 3_500_000L, "/data/nas/output/vacances-ete-2026"));

        assertThat(event.getId()).isNotNull();
        assertThat(job.getId()).isNotNull();
        assertThat(sharpPhoto.isMarkedForDeletion()).isFalse();
        assertThat(blurryPhoto.isMarkedForDeletion()).isTrue();
        assertThat(result.getId()).isNotNull();
        assertThat(photoAssetRepository.countByJobId(job.getId())).isEqualTo(2);
    }
}
