package org.dynamisterrain.api.descriptor;

public record WaterDesc(
    WaterMode mode,
    float elevation,
    float foamDepthThreshold
) {
}
