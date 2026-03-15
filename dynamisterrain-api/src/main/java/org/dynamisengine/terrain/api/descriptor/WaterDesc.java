package org.dynamisengine.terrain.api.descriptor;

public record WaterDesc(
    WaterMode mode,
    float elevation,
    float foamDepthThreshold
) {
}
