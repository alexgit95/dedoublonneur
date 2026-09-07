package fr.dedoublonneur.web;

import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.analysis.DuplicateClusterService;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Photos", description = "Revue des photos analysees (onglets Flou / Doublons) et bascule keep/delete")
public class PhotoAssetController {

    private final PhotoAssetRepository photoAssetRepository;
    private final DuplicateClusterService duplicateClusterService;

    public PhotoAssetController(PhotoAssetRepository photoAssetRepository,
            DuplicateClusterService duplicateClusterService) {
        this.photoAssetRepository = photoAssetRepository;
        this.duplicateClusterService = duplicateClusterService;
    }

    @GetMapping(value = "/api/jobs/{jobId}/blurred", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les photos floues d'un job (onglet \"Flou\")",
            description = "Retourne, de maniere paginee, les photos dont le score de nettete est sous le seuil "
                    + "de flou, pre-cochees pour suppression par defaut.")
    @ApiResponse(responseCode = "200", description = "Page de photos floues")
    public Page<PhotoAssetResponse> getBlurredPhotos(@PathVariable Long jobId,
            @PageableDefault(size = 50) Pageable pageable) {
        return photoAssetRepository.findByJobIdAndBlurredTrue(jobId, pageable).map(PhotoAssetResponse::from);
    }

    @GetMapping(value = "/api/jobs/{jobId}/duplicates", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Lister les groupes de doublons d'un job (onglet \"Doublons\")",
            description = "Regroupe les photos par similarite de hash perceptuel au seuil donne (0-100%), "
                    + "recalcule a la demande sans re-analyser les photos. Dans chaque groupe, la photo la plus "
                    + "nette est conservee par defaut (les autres cochees pour suppression), sauf bascule manuelle "
                    + "prealable de l'utilisateur qui est toujours respectee.")
    @ApiResponse(responseCode = "200", description = "Liste des groupes de doublons au seuil demande")
    public List<DuplicateGroupResponse> getDuplicateGroups(@PathVariable Long jobId,
            @RequestParam @Parameter(description = "Seuil de similarite, de 0 a 100%") int threshold) {
        List<List<Long>> groups = duplicateClusterService.clusterPhotoIds(jobId, threshold);
        return groups.stream().map(this::toGroupResponse).toList();
    }

    private DuplicateGroupResponse toGroupResponse(List<Long> photoIds) {
        List<PhotoAsset> photos = photoAssetRepository.findAllById(photoIds);

        // Photo par defaut conservee : la plus nette ; egalite departagee par le nom de
        // fichier qui vient en premier (comparateur inverse pour que max() la retienne).
        PhotoAsset sharpest = photos.stream()
                .max(Comparator.comparingDouble(PhotoAsset::getBlurScore)
                        .thenComparing(Comparator.comparing(PhotoAsset::getRelativePath).reversed()))
                .orElseThrow();

        List<PhotoAssetResponse> responses = photos.stream()
                .map(photo -> toResponseWithDefault(photo, photo.getId().equals(sharpest.getId())))
                .toList();
        return new DuplicateGroupResponse(responses);
    }

    private PhotoAssetResponse toResponseWithDefault(PhotoAsset photo, boolean isSharpestOfGroup) {
        if (!photo.isDeletionOverridden()) {
            boolean defaultToDelete = !isSharpestOfGroup;
            if (photo.isMarkedForDeletion() != defaultToDelete) {
                photo.setMarkedForDeletion(defaultToDelete);
                photoAssetRepository.save(photo);
            }
        }
        return PhotoAssetResponse.from(photo);
    }

    @PatchMapping(value = "/api/photos/{photoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Basculer l'etat de suppression d'une photo",
            description = "Permet de cocher/decocher une photo pour suppression, dans l'onglet Flou ou Doublons. "
                    + "Ce choix manuel est ensuite toujours respecte, meme si le seuil de similarite change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouvel etat de la photo"),
            @ApiResponse(responseCode = "404", description = "Photo introuvable")
    })
    public PhotoAssetResponse toggleDeletion(@PathVariable Long photoId, @RequestBody ToggleDeletionRequest request) {
        PhotoAsset photo = photoAssetRepository.findById(photoId).orElseThrow(() -> new PhotoNotFoundException(photoId));
        photo.setMarkedForDeletion(request.markedForDeletion());
        photo.setDeletionOverridden(true);
        return PhotoAssetResponse.from(photoAssetRepository.save(photo));
    }
}
