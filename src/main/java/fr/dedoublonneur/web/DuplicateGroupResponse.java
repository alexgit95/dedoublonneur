package fr.dedoublonneur.web;

import java.util.List;

/** Un groupe de photos considerees comme doublons/quasi-doublons au seuil de similarite demande. */
public record DuplicateGroupResponse(List<PhotoAssetResponse> photos) {
}
