package fr.dedoublonneur.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import fr.dedoublonneur.domain.PhotoAsset;
import fr.dedoublonneur.domain.PhotoAssetRepository;

/**
 * Regroupe les photos d'un job par similarite de pHash, calcule a la demande (jamais
 * persiste) et mis en cache par (job, seuil) pour eviter de recalculer le clustering a
 * chaque requete identique (design.md decision 3, risque O(n^2)).
 */
@Service
public class DuplicateClusterService {

    private static final int HASH_BIT_LENGTH = 64;

    private final PhotoAssetRepository photoAssetRepository;
    private final Map<CacheKey, List<List<Long>>> cache = new ConcurrentHashMap<>();

    public DuplicateClusterService(PhotoAssetRepository photoAssetRepository) {
        this.photoAssetRepository = photoAssetRepository;
    }

    /**
     * Convertit un seuil de similarite (0-100%) en distance de Hamming maximale
     * autorisee entre deux hash pour etre consideres comme doublons.
     */
    static int maxHammingDistance(int similarityThresholdPercent) {
        double raw = (100 - similarityThresholdPercent) / 100.0 * HASH_BIT_LENGTH;
        return (int) Math.round(raw);
    }

    /** Groupes (taille >= 2) d'identifiants de {@link PhotoAsset} consideres doublons. */
    public List<List<Long>> clusterPhotoIds(Long jobId, int similarityThresholdPercent) {
        return cache.computeIfAbsent(new CacheKey(jobId, similarityThresholdPercent),
                key -> computeClusters(jobId, similarityThresholdPercent));
    }

    /** A appeler une fois le dossier traite : le clustering en cache n'a plus lieu d'etre. */
    public void evictJob(Long jobId) {
        cache.keySet().removeIf(key -> key.jobId().equals(jobId));
    }

    private List<List<Long>> computeClusters(Long jobId, int similarityThresholdPercent) {
        List<PhotoAsset> photos = photoAssetRepository.findByJobId(jobId);
        int maxDistance = maxHammingDistance(similarityThresholdPercent);

        UnionFind unionFind = new UnionFind(photos.size());
        for (int i = 0; i < photos.size(); i++) {
            for (int j = i + 1; j < photos.size(); j++) {
                int distance = ImageAnalysisService.hammingDistance(photos.get(i).getPHash(), photos.get(j).getPHash());
                if (distance <= maxDistance) {
                    unionFind.union(i, j);
                }
            }
        }

        Map<Integer, List<Long>> groupsByRoot = new HashMap<>();
        for (int i = 0; i < photos.size(); i++) {
            groupsByRoot.computeIfAbsent(unionFind.find(i), k -> new ArrayList<>()).add(photos.get(i).getId());
        }

        List<List<Long>> groups = new ArrayList<>();
        for (List<Long> group : groupsByRoot.values()) {
            if (group.size() >= 2) {
                groups.add(group);
            }
        }
        return groups;
    }

    private record CacheKey(Long jobId, int similarityThresholdPercent) {
    }

    /** Union-find (disjoint set) elementaire, indices 0..n-1. */
    private static final class UnionFind {
        private final int[] parent;

        UnionFind(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        int find(int i) {
            while (parent[i] != i) {
                parent[i] = parent[parent[i]];
                i = parent[i];
            }
            return i;
        }

        void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA != rootB) {
                parent[rootA] = rootB;
            }
        }
    }
}
