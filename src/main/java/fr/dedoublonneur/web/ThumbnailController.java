package fr.dedoublonneur.web;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import fr.dedoublonneur.analysis.JobNotFoundException;
import fr.dedoublonneur.analysis.ThumbnailService;
import fr.dedoublonneur.domain.AnalysisJobRepository;
import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Vignettes", description = "Vignettes des photos analysees, en cache disque ephemere")
public class ThumbnailController {

    private final PhotoAssetRepository photoAssetRepository;
    private final AnalysisJobRepository jobRepository;
    private final ThumbnailService thumbnailService;

    public ThumbnailController(PhotoAssetRepository photoAssetRepository, AnalysisJobRepository jobRepository,
            ThumbnailService thumbnailService) {
        this.photoAssetRepository = photoAssetRepository;
        this.jobRepository = jobRepository;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping(value = "/api/photos/{id}/thumbnail", produces = MediaType.IMAGE_JPEG_VALUE)
    @Operation(summary = "Recuperer la vignette d'une photo",
            description = "Sert la vignette depuis le cache disque ephemere ; la regenere a la demande en cas "
                    + "d'absence (ex: redemarrage du conteneur ayant vide le cache).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image JPEG de la vignette"),
            @ApiResponse(responseCode = "404", description = "Photo introuvable")
    })
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long id) throws IOException {
        PhotoAsset photo = photoAssetRepository.findById(id).orElseThrow(() -> new PhotoNotFoundException(id));
        Long jobId = photo.getJob().getId(); // identifiant du proxy lazy, pas d'initialisation necessaire

        if (!thumbnailService.exists(jobId, id)) {
            String folderPath = jobRepository.findEventFolderPathByJobId(jobId)
                    .orElseThrow(() -> new JobNotFoundException(jobId));
            Path source = Path.of(folderPath, photo.getRelativePath());
            thumbnailService.generate(jobId, id, source);
        }

        byte[] bytes = Files.readAllBytes(thumbnailService.getPath(jobId, id));
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(bytes);
    }
}
