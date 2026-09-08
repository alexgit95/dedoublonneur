package fr.dedoublonneur.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppPropertiesTest {

    @Test
    void acceptsThresholdBoundaries() {
        new AppProperties.Analysis(20, 0, 60.0);
        new AppProperties.Analysis(20, 100, 60.0);
    }

    @Test
    void rejectsThresholdOutsidePercentageRange() {
        assertThatThrownBy(() -> new AppProperties.Analysis(20, -1, 60.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AppProperties.Analysis(20, 101, 60.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}