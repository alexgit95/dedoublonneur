package fr.dedoublonneur.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DuplicateClusterServiceMathTest {

    @Test
    void maxHammingDistanceAt100PercentAllowsOnlyIdenticalHashes() {
        assertThat(DuplicateClusterService.maxHammingDistance(100)).isZero();
    }

    @Test
    void maxHammingDistanceAt0PercentAllowsAnyHash() {
        assertThat(DuplicateClusterService.maxHammingDistance(0)).isEqualTo(64);
    }

    @Test
    void maxHammingDistanceAt90PercentAllowsSmallDifference() {
        // 90% similarity => (100-90)/100 * 64 = 6.4 -> rounds to 6
        assertThat(DuplicateClusterService.maxHammingDistance(90)).isEqualTo(6);
    }
}
