package fr.dedoublonneur.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Photos", description = "Revue des photos analysees (onglets Flou / Doublons) et bascule keep/delete")
public class PhotoAssetController {

    private final PhotoAssetRepository photoAssetRepository;

    public PhotoAssetController(PhotoAssetRepository photoAssetRepository) {
        this.photoAssetRepository = photoAssetRepository;
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

    @PatchMapping(value = "/api/photos/{photoId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Basculer l'etat de suppression d'une photo",
            description = "Permet de cocher/decocher une photo pour suppression, dans l'onglet Flou ou Doublons.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouvel etat de la photo"),
            @ApiResponse(responseCode = "404", description = "Photo introuvable")
    })
    public PhotoAssetResponse toggleDeletion(@PathVariable Long photoId, @RequestBody ToggleDeletionRequest request) {
        PhotoAsset photo = photoAssetRepository.findById(photoId).orElseThrow(() -> new PhotoNotFoundException(photoId));
        photo.setMarkedForDeletion(request.markedForDeletion());
        return PhotoAssetResponse.from(photoAssetRepository.save(photo));
    }
}
