package org.dynamisengine.terrain.core.scatter;

public record ScatterConfig(
    float minSpacing,
    int maxCandidates,
    float scaleMin,
    float scaleMax
) {
    public static ScatterConfig defaults() {
        return new ScatterConfig(1.0f, 30, 0.9f, 1.1f);
    }
}
