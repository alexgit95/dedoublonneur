package fr.dedoublonneur.web;

/**
 * Representation API paginee d'une photo analysee (onglets Flou / Doublons).
 */
public record PhotoAssetResponse(Long id, String relativePath, double blurScore, boolean blurred,
        boolean markedForDeletion, String thumbnailUrl) {

    public static PhotoAssetResponse from(fr.dedoublonneur.domain.PhotoAsset photo) {
        return new PhotoAssetResponse(photo.getId(), photo.getRelativePath(), photo.getBlurScore(),
                photo.isBlurred(), photo.isMarkedForDeletion(), "/api/photos/" + photo.getId() + "/thumbnail");
    }
}
