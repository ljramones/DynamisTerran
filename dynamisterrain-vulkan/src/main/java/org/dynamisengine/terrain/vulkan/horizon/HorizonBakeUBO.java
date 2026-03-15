package org.dynamisengine.terrain.vulkan.horizon;

public record HorizonBakeUBO(
    float worldScale,
    float heightScale,
    float rcpSearchRadius,
    int searchRadius,
    int textureWidth,
    int textureHeight,
    float pad0,
    float pad1
) {
}
